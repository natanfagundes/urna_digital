package urna.persistencia;

import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Eleicao;
import urna.model.Eleitor;
import urna.model.Partido;
import urna.util.Textos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Salva e carrega a configuração de uma eleição numa pasta:
 *
 * <pre>
 * eleicoes/&lt;nome-da-eleicao&gt;/
 *   eleicao.properties   nome e resumo da senha do mesário
 *   cargos.csv           ordem de votação, dígitos, tipo, vagas, vice
 *   partidos.csv         partidos ou chapas
 *   candidatos.csv       candidatos (com o caminho da foto, se houver)
 *   eleitores.csv        títulos/matrículas e nomes
 *   fotos/               fotos dos candidatos
 *   votacao/             votação em andamento (ver {@link ArmazenamentoEmArquivo})
 *   relatorios/          Zerésima e Boletins de Urna
 * </pre>
 *
 * Os arquivos CSV podem ser abertos e editados no Excel.
 */
public class RepositorioEleicao {

    public static final Path PASTA_ELEICOES = Paths.get("eleicoes");

    private static final String ARQ_ELEICAO = "eleicao.properties";
    private static final String ARQ_CARGOS = "cargos.csv";
    private static final String ARQ_PARTIDOS = "partidos.csv";
    private static final String ARQ_CANDIDATOS = "candidatos.csv";
    private static final String ARQ_ELEITORES = "eleitores.csv";
    private static final String PASTA_FOTOS = "fotos";

    /** Pasta sugerida para uma eleição, a partir do nome. */
    public static Path pastaPadrao(String nomeEleicao) {
        return PASTA_ELEICOES.resolve(Textos.slug(nomeEleicao));
    }

    public static boolean existeEleicao(Path pasta) {
        return Files.isRegularFile(pasta.resolve(ARQ_ELEICAO));
    }

    public static Path pastaVotacao(Path pastaEleicao) {
        return pastaEleicao.resolve("votacao");
    }

    public static Path pastaRelatorios(Path pastaEleicao) {
        return pastaEleicao.resolve("relatorios");
    }

    // ------------------------------------------------------------------
    // Salvar
    // ------------------------------------------------------------------

    public void salvar(Eleicao eleicao, Path pasta) throws IOException {
        Files.createDirectories(pasta);

        Properties props = new Properties();
        props.setProperty("nome", eleicao.getNome());
        if (eleicao.getHashSenhaMesario() != null) {
            props.setProperty("senhaMesarioSha256", eleicao.getHashSenhaMesario());
        }
        try (Writer w = Files.newBufferedWriter(pasta.resolve(ARQ_ELEICAO), StandardCharsets.UTF_8)) {
            props.store(w, "Urna Digital - configuracao da eleicao");
        }

        List<List<String>> cargos = new ArrayList<>();
        cargos.add(Arrays.asList("ordem", "nome", "digitos", "tipo", "vagas", "rotulo_vice"));
        int ordem = 1;
        for (Cargo c : eleicao.getCargos()) {
            cargos.add(Arrays.asList(String.valueOf(ordem++), c.getNome(), String.valueOf(c.getQuantidadeDigitos()),
                    c.getTipo().name(), String.valueOf(c.getVagas()), c.getRotuloVice()));
        }
        Csv.escrever(pasta.resolve(ARQ_CARGOS), cargos);

        List<List<String>> partidos = new ArrayList<>();
        partidos.add(Arrays.asList("numero", "sigla", "nome"));
        for (Partido p : eleicao.listarPartidos()) {
            partidos.add(Arrays.asList(String.valueOf(p.getNumero()), p.getSigla(), p.getNome()));
        }
        Csv.escrever(pasta.resolve(ARQ_PARTIDOS), partidos);

        List<List<String>> candidatos = new ArrayList<>();
        candidatos.add(Arrays.asList("cargo", "numero", "nome", "partido", "vice", "foto"));
        for (Candidato c : eleicao.listarTodosCandidatos()) {
            String foto = salvarFoto(c, pasta);
            candidatos.add(Arrays.asList(c.getCargo().getNome(), String.valueOf(c.getNumero()), c.getNome(),
                    String.valueOf(c.getPartido().getNumero()), c.getNomeVice(), foto));
        }
        Csv.escrever(pasta.resolve(ARQ_CANDIDATOS), candidatos);

        List<List<String>> eleitores = new ArrayList<>();
        eleitores.add(Arrays.asList("titulo", "nome"));
        for (Eleitor e : eleicao.listarEleitores()) {
            eleitores.add(Arrays.asList(e.getTitulo(), e.getNome()));
        }
        Csv.escrever(pasta.resolve(ARQ_ELEITORES), eleitores);
    }

    /**
     * Copia a foto do candidato para a pasta fotos/ da eleição (se ainda não estiver lá)
     * e devolve o caminho relativo que vai no CSV.
     */
    private String salvarFoto(Candidato c, Path pasta) throws IOException {
        if (c.getFoto() == null || !Files.isRegularFile(c.getFoto())) {
            return "";
        }
        Path fotos = pasta.resolve(PASTA_FOTOS);
        Files.createDirectories(fotos);
        String original = c.getFoto().getFileName().toString();
        String extensao = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
        String nome = Textos.slug(c.getCargo().getNome()) + "_" + c.getNumero() + extensao;
        Path destino = fotos.resolve(nome);
        if (!Files.exists(destino) || !Files.isSameFile(c.getFoto(), destino)) {
            Files.copy(c.getFoto(), destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return PASTA_FOTOS + "/" + nome;
    }

    // ------------------------------------------------------------------
    // Carregar
    // ------------------------------------------------------------------

    public Eleicao carregar(Path pasta) throws IOException {
        if (!existeEleicao(pasta)) {
            throw new IOException("Não há uma eleição salva em " + pasta.toAbsolutePath());
        }
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(pasta.resolve(ARQ_ELEICAO), StandardCharsets.UTF_8)) {
            props.load(r);
        }
        Eleicao eleicao = new Eleicao(props.getProperty("nome", pasta.getFileName().toString()));
        eleicao.setHashSenhaMesario(props.getProperty("senhaMesarioSha256"));

        for (List<String> l : semCabecalho(pasta.resolve(ARQ_CARGOS))) {
            try {
                eleicao.adicionarCargo(new Cargo(campo(l, 1), inteiro(campo(l, 2)),
                        Cargo.Tipo.valueOf(campo(l, 3).toUpperCase(Locale.ROOT)), inteiro(campo(l, 4)), campo(l, 5)));
            } catch (RuntimeException ex) {
                throw erro(ARQ_CARGOS, l, ex);
            }
        }
        for (List<String> l : semCabecalho(pasta.resolve(ARQ_PARTIDOS))) {
            try {
                eleicao.cadastrarPartido(inteiro(campo(l, 0)), campo(l, 1), campo(l, 2));
            } catch (RuntimeException ex) {
                throw erro(ARQ_PARTIDOS, l, ex);
            }
        }
        for (List<String> l : semCabecalho(pasta.resolve(ARQ_CANDIDATOS))) {
            try {
                Cargo cargo = eleicao.buscarCargo(campo(l, 0))
                        .orElseThrow(() -> new IllegalArgumentException("cargo inexistente: " + campo(l, 0)));
                Partido partido = eleicao.buscarPartido(inteiro(campo(l, 3)))
                        .orElseThrow(() -> new IllegalArgumentException("partido inexistente: " + campo(l, 3)));
                String foto = campo(l, 5);
                eleicao.cadastrarCandidato(new Candidato(inteiro(campo(l, 1)), campo(l, 2), partido, cargo,
                        campo(l, 4), foto.isEmpty() ? null : pasta.resolve(foto)));
            } catch (RuntimeException ex) {
                throw erro(ARQ_CANDIDATOS, l, ex);
            }
        }
        for (List<String> l : semCabecalho(pasta.resolve(ARQ_ELEITORES))) {
            try {
                eleicao.cadastrarEleitor(campo(l, 0), campo(l, 1));
            } catch (RuntimeException ex) {
                throw erro(ARQ_ELEITORES, l, ex);
            }
        }
        return eleicao;
    }

    private static List<List<String>> semCabecalho(Path arquivo) throws IOException {
        if (!Files.exists(arquivo)) {
            return new ArrayList<>();
        }
        List<List<String>> linhas = Csv.ler(arquivo);
        return linhas.isEmpty() ? linhas : linhas.subList(1, linhas.size());
    }

    private static String campo(List<String> linha, int indice) {
        return indice < linha.size() ? linha.get(indice) : "";
    }

    private static int inteiro(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("número inválido: \"" + texto + "\"");
        }
    }

    private static IOException erro(String arquivo, List<String> linha, RuntimeException causa) {
        return new IOException("Erro em " + arquivo + ", linha " + linha + ": " + causa.getMessage(), causa);
    }
}
