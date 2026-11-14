package urna.service;

import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Eleitor;
import urna.model.Partido;
import urna.model.Voto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Regras de votação e contagem dos votos.
 * Não conhece a interface gráfica: pode ser usada e testada isoladamente.
 *
 * Para garantir o sigilo do voto, a urna guarda QUEM votou (títulos) separado
 * de EM QUEM se votou (contadores), sem nenhuma ligação entre os dois.
 */
public class Urna {

    public static final int VAGAS_VEREADOR_PADRAO = 9;
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final CadastroEleitoral cadastro;
    private final String senhaMesario;
    private final int vagasVereador;

    private final Map<Cargo, Map<Integer, Integer>> votosNominais = new EnumMap<>(Cargo.class);
    private final Map<Cargo, Map<Integer, Integer>> votosLegenda = new EnumMap<>(Cargo.class);
    private final Map<Cargo, Integer> votosBrancos = new EnumMap<>(Cargo.class);
    private final Map<Cargo, Integer> votosNulos = new EnumMap<>(Cargo.class);

    private final Set<String> titulosQueVotaram = new HashSet<>();
    private Eleitor eleitorAtual;
    private boolean encerrada;
    private final LocalDateTime abertura = LocalDateTime.now();
    private LocalDateTime encerramento;

    public Urna(CadastroEleitoral cadastro, String senhaMesario, int vagasVereador) {
        if (vagasVereador <= 0) {
            throw new IllegalArgumentException("O número de vagas deve ser positivo.");
        }
        this.cadastro = cadastro;
        this.senhaMesario = senhaMesario;
        this.vagasVereador = vagasVereador;
        for (Cargo cargo : Cargo.values()) {
            votosNominais.put(cargo, new HashMap<>());
            votosLegenda.put(cargo, new HashMap<>());
            votosBrancos.put(cargo, 0);
            votosNulos.put(cargo, 0);
        }
    }

    public Urna(CadastroEleitoral cadastro, String senhaMesario) {
        this(cadastro, senhaMesario, VAGAS_VEREADOR_PADRAO);
    }

    public CadastroEleitoral getCadastro() {
        return cadastro;
    }

    public int getVagasVereador() {
        return vagasVereador;
    }

    public boolean validarSenhaMesario(String senha) {
        return senhaMesario.equals(senha);
    }

    // ------------------------------------------------------------------
    // Controle de eleitores
    // ------------------------------------------------------------------

    /**
     * O mesário libera a urna para o eleitor com o título informado.
     *
     * @throws IllegalStateException    se a urna estiver encerrada ou já houver um eleitor votando
     * @throws IllegalArgumentException se o título não existir ou o eleitor já tiver votado
     */
    public Eleitor liberarEleitor(String titulo) {
        if (encerrada) {
            throw new IllegalStateException("A votação já foi encerrada.");
        }
        if (eleitorAtual != null) {
            throw new IllegalStateException("O eleitor " + eleitorAtual.getNome() + " ainda está votando.");
        }
        Eleitor eleitor = cadastro.buscarEleitor(titulo)
                .orElseThrow(() -> new IllegalArgumentException("Título não encontrado nesta seção: " + titulo));
        if (jaVotou(eleitor)) {
            throw new IllegalArgumentException("O eleitor " + eleitor.getNome() + " já votou.");
        }
        eleitorAtual = eleitor;
        return eleitor;
    }

    public boolean jaVotou(Eleitor eleitor) {
        return titulosQueVotaram.contains(eleitor.getTitulo());
    }

    /** Eleitor liberado que está votando agora, ou {@code null}. */
    public Eleitor getEleitorAtual() {
        return eleitorAtual;
    }

    /** Chamado quando o eleitor termina de votar em todos os cargos. */
    public void finalizarEleitor() {
        if (eleitorAtual == null) {
            throw new IllegalStateException("Nenhum eleitor foi liberado.");
        }
        titulosQueVotaram.add(eleitorAtual.getTitulo());
        eleitorAtual = null;
    }

    public int getEleitoresQueVotaram() {
        return titulosQueVotaram.size();
    }

    public int getEleitoresAptos() {
        return cadastro.listarEleitores().size();
    }

    public int getAbstencoes() {
        return getEleitoresAptos() - getEleitoresQueVotaram();
    }

    // ------------------------------------------------------------------
    // Votação
    // ------------------------------------------------------------------

    /**
     * Interpreta os dígitos digitados para um cargo, sem registrar o voto.
     * Usado para mostrar ao eleitor o que será confirmado.
     */
    public Voto interpretar(Cargo cargo, String digitos) {
        if (digitos == null || !digitos.matches("\\d{" + cargo.getQuantidadeDigitos() + "}")) {
            throw new IllegalArgumentException("Digite " + cargo.getQuantidadeDigitos()
                    + " dígitos para " + cargo);
        }
        int numero = Integer.parseInt(digitos);

        Optional<Candidato> candidato = cadastro.buscarCandidato(cargo, numero);
        if (candidato.isPresent()) {
            return Voto.nominal(candidato.get());
        }
        if (cargo.aceitaVotoLegenda()) {
            int numeroPartido = Integer.parseInt(digitos.substring(0, 2));
            Optional<Partido> partido = cadastro.buscarPartido(numeroPartido);
            if (partido.isPresent()) {
                return Voto.legenda(cargo, partido.get());
            }
        }
        return Voto.nulo(cargo);
    }

    public void registrar(Voto voto) {
        if (encerrada) {
            throw new IllegalStateException("A votação já foi encerrada.");
        }
        if (eleitorAtual == null) {
            throw new IllegalStateException("A urna não foi liberada para nenhum eleitor.");
        }
        Cargo cargo = voto.getCargo();
        switch (voto.getTipo()) {
            case NOMINAL:
                votosNominais.get(cargo).merge(voto.getCandidato().getNumero(), 1, Integer::sum);
                break;
            case LEGENDA:
                votosLegenda.get(cargo).merge(voto.getPartido().getNumero(), 1, Integer::sum);
                break;
            case BRANCO:
                votosBrancos.merge(cargo, 1, Integer::sum);
                break;
            case NULO:
                votosNulos.merge(cargo, 1, Integer::sum);
                break;
        }
    }

    /**
     * Encerra a votação. Se houver um eleitor no meio do voto, a votação
     * não pode ser encerrada.
     */
    public void encerrar() {
        if (eleitorAtual != null) {
            throw new IllegalStateException("Não é possível encerrar: o eleitor "
                    + eleitorAtual.getNome() + " ainda está votando.");
        }
        encerrada = true;
        encerramento = LocalDateTime.now();
    }

    public boolean isEncerrada() {
        return encerrada;
    }

    // ------------------------------------------------------------------
    // Contagem
    // ------------------------------------------------------------------

    public int getVotosCandidato(Candidato candidato) {
        return votosNominais.get(candidato.getCargo()).getOrDefault(candidato.getNumero(), 0);
    }

    public int getVotosLegenda(Cargo cargo, Partido partido) {
        return votosLegenda.get(cargo).getOrDefault(partido.getNumero(), 0);
    }

    /** Votos nominais dos candidatos do partido somados aos votos de legenda. */
    public int getVotosPartido(Cargo cargo, Partido partido) {
        int total = getVotosLegenda(cargo, partido);
        for (Candidato c : cadastro.listarCandidatos(cargo)) {
            if (c.getPartido().equals(partido)) {
                total += getVotosCandidato(c);
            }
        }
        return total;
    }

    public int getVotosBrancos(Cargo cargo) {
        return votosBrancos.get(cargo);
    }

    public int getVotosNulos(Cargo cargo) {
        return votosNulos.get(cargo);
    }

    /** Votos válidos = nominais + legenda (brancos e nulos não entram). */
    public int getVotosValidos(Cargo cargo) {
        int total = 0;
        for (int v : votosNominais.get(cargo).values()) total += v;
        for (int v : votosLegenda.get(cargo).values()) total += v;
        return total;
    }

    public int getTotalVotos(Cargo cargo) {
        return getVotosValidos(cargo) + getVotosBrancos(cargo) + getVotosNulos(cargo);
    }

    /** Candidatos do cargo ordenados do mais votado para o menos votado. */
    public List<Candidato> ranking(Cargo cargo) {
        List<Candidato> lista = new ArrayList<>(cadastro.listarCandidatos(cargo));
        lista.sort(Comparator.comparingInt(this::getVotosCandidato).reversed()
                .thenComparing(Candidato::getNome));
        return lista;
    }

    // ------------------------------------------------------------------
    // Relatórios
    // ------------------------------------------------------------------

    /**
     * Zerésima: relatório emitido antes do primeiro voto, provando que
     * todos os candidatos começam com zero votos.
     */
    public String gerarZeresima() {
        StringBuilder sb = new StringBuilder();
        sb.append("=============== ZERÉSIMA ===============\n");
        sb.append("Emitida em: ").append(LocalDateTime.now().format(FORMATO_DATA)).append('\n');
        sb.append("Eleitores aptos: ").append(getEleitoresAptos()).append("\n\n");
        for (Cargo cargo : Cargo.values()) {
            sb.append("------ ").append(cargo.getDescricao().toUpperCase()).append(" ------\n");
            for (Candidato c : cadastro.listarCandidatos(cargo)) {
                sb.append(String.format("%-6d %-20s %-5s %11s%n",
                        c.getNumero(), c.getNome(), c.getPartido().getSigla(), votos(getVotosCandidato(c))));
            }
            sb.append(String.format("Brancos: %d   Nulos: %d   Total: %d%n%n",
                    getVotosBrancos(cargo), getVotosNulos(cargo), getTotalVotos(cargo)));
        }
        sb.append("Todos os candidatos estão com ZERO votos: ")
                .append(getEleitoresQueVotaram() == 0 ? "SIM" : "NÃO").append('\n');
        sb.append("=========================================\n");
        return sb.toString();
    }

    /** Texto do Boletim de Urna com a apuração e os eleitos de todos os cargos. */
    public String gerarBoletim() {
        if (!encerrada) {
            throw new IllegalStateException("O Boletim de Urna só pode ser emitido após o encerramento.");
        }
        Apuracao apuracao = new Apuracao(this);
        StringBuilder sb = new StringBuilder();
        sb.append("========== BOLETIM DE URNA ==========\n");
        sb.append("Abertura:      ").append(abertura.format(FORMATO_DATA)).append('\n');
        sb.append("Encerramento:  ").append(encerramento.format(FORMATO_DATA)).append('\n');
        sb.append("Eleitores aptos:   ").append(getEleitoresAptos()).append('\n');
        sb.append("Comparecimento:    ").append(getEleitoresQueVotaram()).append('\n');
        sb.append("Abstenções:        ").append(getAbstencoes()).append("\n\n");

        for (Cargo cargo : Cargo.values()) {
            int validos = getVotosValidos(cargo);
            sb.append("------ ").append(cargo.getDescricao().toUpperCase()).append(" ------\n");

            for (Candidato c : ranking(cargo)) {
                int votos = getVotosCandidato(c);
                sb.append(String.format("%-6d %-20s %-5s %11s  %6s%n",
                        c.getNumero(), c.getNome(), c.getPartido().getSigla(), votos(votos), percentual(votos, validos)));
            }

            if (cargo.aceitaVotoLegenda()) {
                Map<Partido, Integer> cadeiras = apuracao.distribuirCadeiras(cargo, vagasVereador);
                sb.append("\nPor partido (nominais + legenda):\n");
                for (Partido p : cadastro.listarPartidos()) {
                    sb.append(String.format("%-6d %-6s %11s  (legenda: %d)  cadeiras: %d%n",
                            p.getNumero(), p.getSigla(), votos(getVotosPartido(cargo, p)),
                            getVotosLegenda(cargo, p), cadeiras.get(p)));
                }
            }

            sb.append('\n');
            sb.append("Votos válidos: ").append(validos).append('\n');
            sb.append("Votos brancos: ").append(getVotosBrancos(cargo)).append('\n');
            sb.append("Votos nulos:   ").append(getVotosNulos(cargo)).append('\n');
            sb.append("Total:         ").append(getTotalVotos(cargo)).append("\n\n");

            if (cargo.aceitaVotoLegenda()) {
                sb.append("Vagas: ").append(vagasVereador)
                        .append("   Quociente eleitoral: ").append(apuracao.quocienteEleitoral(cargo, vagasVereador))
                        .append("   Votação mínima individual: ").append(votos(apuracao.votacaoMinima(cargo, vagasVereador)))
                        .append('\n');
                List<Candidato> eleitos = apuracao.eleitosProporcional(cargo, vagasVereador);
                sb.append("ELEITOS:").append(eleitos.isEmpty() ? " nenhum" : "").append('\n');
                for (Candidato c : eleitos) {
                    sb.append("  ").append(c).append(" - ").append(votos(getVotosCandidato(c))).append('\n');
                }
                if (eleitos.size() < vagasVereador) {
                    sb.append("Vagas não preenchidas: ").append(vagasVereador - eleitos.size()).append('\n');
                }
            } else {
                List<Candidato> vencedores = apuracao.vencedores(cargo);
                if (vencedores.isEmpty()) {
                    sb.append("ELEITO: nenhum (sem votos válidos)\n");
                } else if (vencedores.size() == 1) {
                    Candidato c = vencedores.get(0);
                    sb.append("ELEITO: ").append(c);
                    if (c.getNomeVice() != null) {
                        sb.append(" | Vice: ").append(c.getNomeVice());
                    }
                    sb.append('\n');
                } else {
                    sb.append("EMPATE entre: ");
                    for (int i = 0; i < vencedores.size(); i++) {
                        sb.append(i > 0 ? ", " : "").append(vencedores.get(i).getNome());
                    }
                    sb.append(" (desempate pelo candidato mais idoso)\n");
                }
            }
            sb.append('\n');
        }
        sb.append("=====================================\n");
        return sb.toString();
    }

    private static String votos(int quantidade) {
        return quantidade + (quantidade == 1 ? " voto" : " votos");
    }

    private static String percentual(int parte, int total) {
        if (total == 0) return "0,0%";
        return String.format("%.1f%%", parte * 100.0 / total);
    }
}
