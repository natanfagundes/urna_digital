package urna.persistencia;

import urna.service.ArmazenamentoVotacao;
import urna.service.Contagem;
import urna.service.EstadoVotacao;
import urna.util.Hash;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Grava a votação no arquivo {@code votacao/estado.txt}.
 *
 * <ul>
 *   <li><b>Atômico:</b> grava num arquivo temporário e depois o renomeia por cima do
 *       anterior. Se faltar energia no meio, fica valendo a versão anterior inteira.</li>
 *   <li><b>Íntegro:</b> a última linha é o SHA-256 do conteúdo. Se alguém editar o
 *       arquivo à mão, a urna percebe e se recusa a carregá-lo.</li>
 *   <li><b>Sigiloso:</b> só guarda totais e a lista (ordenada) de quem votou.</li>
 * </ul>
 */
public class ArmazenamentoEmArquivo implements ArmazenamentoVotacao {

    private static final String CABECALHO = "URNA-DIGITAL-VOTACAO;1";
    private static final char SEP = ';';
    private static final int TENTATIVAS = 10;
    private static final DateTimeFormatter FORMATO_ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path pasta;

    public ArmazenamentoEmArquivo(Path pasta) {
        this.pasta = pasta;
    }

    private Path arquivo() {
        return pasta.resolve("estado.txt");
    }

    public boolean existe() {
        return Files.exists(arquivo());
    }

    /**
     * Guarda a votação atual com outro nome (nada é apagado), para começar uma nova.
     *
     * @return pasta para onde a votação anterior foi movida
     */
    public Path arquivar() throws IOException {
        Path destino = pasta.resolveSibling(pasta.getFileName() + "_arquivada_" + LocalDateTime.now().format(FORMATO_ARQUIVO));
        Files.move(pasta, destino);
        return destino;
    }

    @Override
    public void salvar(EstadoVotacao estado) {
        List<String> linhas = new ArrayList<>();
        linhas.add(CABECALHO);
        linhas.add(linha("ABERTURA", estado.getAbertura().toString()));
        if (estado.getEncerramento() != null) {
            linhas.add(linha("ENCERRAMENTO", estado.getEncerramento().toString()));
        }
        for (String titulo : estado.getTitulosQueVotaram()) {
            linhas.add(linha("VOTOU", titulo));
        }
        Contagem c = estado.getContagem();
        for (Map.Entry<String, Map<Integer, Integer>> e : c.getNominais().entrySet()) {
            new TreeMap<>(e.getValue()).forEach((numero, qtd) ->
                    linhas.add(linha("NOMINAL", e.getKey(), String.valueOf(numero), String.valueOf(qtd))));
        }
        for (Map.Entry<String, Map<Integer, Integer>> e : c.getLegenda().entrySet()) {
            new TreeMap<>(e.getValue()).forEach((numero, qtd) ->
                    linhas.add(linha("LEGENDA", e.getKey(), String.valueOf(numero), String.valueOf(qtd))));
        }
        c.getBrancos().forEach((cargo, qtd) -> linhas.add(linha("BRANCO", cargo, String.valueOf(qtd))));
        c.getNulos().forEach((cargo, qtd) -> linhas.add(linha("NULO", cargo, String.valueOf(qtd))));

        String conteudo = String.join("\n", linhas) + "\n";
        String texto = conteudo + "SHA256;" + Hash.sha256(conteudo) + "\n";

        try {
            Files.createDirectories(pasta);
            Path temporario = pasta.resolve("estado.tmp");
            Files.write(temporario, texto.getBytes(StandardCharsets.UTF_8));
            substituirComNovasTentativas(temporario);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar a votação em " + arquivo().toAbsolutePath(), e);
        }
    }

    /**
     * Troca o estado.txt pelo arquivo temporário. No Windows, o OneDrive, o antivírus ou o
     * indexador podem abrir o arquivo por um instante logo depois de gravado, e nesse momento
     * a troca é negada (AccessDeniedException). Por isso tenta de novo algumas vezes.
     */
    private void substituirComNovasTentativas(Path temporario) throws IOException {
        for (int tentativa = 1; ; tentativa++) {
            try {
                try {
                    Files.move(temporario, arquivo(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporario, arquivo(), StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (AccessDeniedException e) {
                if (tentativa >= TENTATIVAS) {
                    throw e;
                }
                try {
                    Thread.sleep(50L * tentativa);
                } catch (InterruptedException interrompida) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    @Override
    public Optional<EstadoVotacao> carregar() {
        if (!existe()) {
            return Optional.empty();
        }
        List<String> linhas;
        try {
            linhas = Files.readAllLines(arquivo(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + arquivo().toAbsolutePath(), e);
        }
        verificarIntegridade(linhas);

        Contagem contagem = new Contagem();
        TreeSet<String> votaram = new TreeSet<>();
        LocalDateTime abertura = null;
        LocalDateTime encerramento = null;
        for (String l : linhas.subList(1, linhas.size() - 1)) {
            List<String> c = Csv.lerLinha(l, SEP);
            switch (c.get(0)) {
                case "ABERTURA":
                    abertura = LocalDateTime.parse(c.get(1));
                    break;
                case "ENCERRAMENTO":
                    encerramento = LocalDateTime.parse(c.get(1));
                    break;
                case "VOTOU":
                    votaram.add(c.get(1));
                    break;
                case "NOMINAL":
                    contagem.definirNominais(c.get(1), Integer.parseInt(c.get(2)), Integer.parseInt(c.get(3)));
                    break;
                case "LEGENDA":
                    contagem.definirLegenda(c.get(1), Integer.parseInt(c.get(2)), Integer.parseInt(c.get(3)));
                    break;
                case "BRANCO":
                    contagem.definirBrancos(c.get(1), Integer.parseInt(c.get(2)));
                    break;
                case "NULO":
                    contagem.definirNulos(c.get(1), Integer.parseInt(c.get(2)));
                    break;
                default:
                    throw new IllegalStateException("Linha desconhecida no arquivo de votação: " + l);
            }
        }
        if (abertura == null) {
            throw new IllegalStateException("Arquivo de votação sem horário de abertura.");
        }
        return Optional.of(new EstadoVotacao(contagem, votaram, abertura, encerramento));
    }

    private void verificarIntegridade(List<String> linhas) {
        if (linhas.size() < 3 || !linhas.get(0).equals(CABECALHO) || !linhas.get(linhas.size() - 1).startsWith("SHA256;")) {
            throw new IllegalStateException("O arquivo de votação está incompleto ou em formato desconhecido.");
        }
        String conteudo = String.join("\n", linhas.subList(0, linhas.size() - 1)) + "\n";
        String esperado = linhas.get(linhas.size() - 1).substring("SHA256;".length());
        if (!Hash.sha256(conteudo).equals(esperado)) {
            throw new IllegalStateException("O arquivo de votação foi alterado ou está corrompido "
                    + "(código de verificação não confere).");
        }
    }

    private static String linha(String... campos) {
        return Csv.formatarLinha(Arrays.asList(campos), SEP);
    }
}
