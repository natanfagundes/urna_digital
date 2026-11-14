package urna.service;

import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Eleicao;
import urna.model.Eleitor;
import urna.model.Partido;
import urna.model.Voto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Regras de votação e contagem dos votos.
 * Não conhece a interface gráfica: pode ser usada e testada isoladamente.
 *
 * <p><b>Sigilo:</b> a urna guarda QUEM votou (títulos) separado de EM QUEM se votou
 * (contadores), sem nenhuma ligação entre os dois.</p>
 *
 * <p><b>Votação como transação:</b> os votos do eleitor ficam pendentes e só entram na
 * contagem quando ele conclui todos os cargos ({@link #finalizarEleitor()}). Nesse momento
 * o estado é gravado pelo {@link ArmazenamentoVotacao}. Se o programa fechar no meio, nada
 * do eleitor incompleto é contado e ele pode votar de novo.</p>
 */
public class Urna {

    private final Eleicao eleicao;
    private final ArmazenamentoVotacao armazenamento;

    private Contagem contagem;
    private Set<String> titulosQueVotaram;
    private final LocalDateTime abertura;
    private LocalDateTime encerramento;

    private Eleitor eleitorAtual;
    private final List<Voto> votosPendentes = new ArrayList<>();

    private Urna(Eleicao eleicao, ArmazenamentoVotacao armazenamento, EstadoVotacao estado) {
        this.eleicao = eleicao;
        this.armazenamento = armazenamento;
        this.contagem = estado.getContagem();
        this.titulosQueVotaram = new TreeSet<>(estado.getTitulosQueVotaram());
        this.abertura = estado.getAbertura();
        this.encerramento = estado.getEncerramento();
    }

    /** Abre uma votação nova (zerada). A configuração da eleição fica bloqueada. */
    public static Urna nova(Eleicao eleicao, ArmazenamentoVotacao armazenamento) {
        List<String> pendencias = eleicao.verificarPendencias();
        if (!pendencias.isEmpty()) {
            throw new IllegalStateException("A eleição não está pronta: " + String.join(" ", pendencias));
        }
        eleicao.bloquear();
        EstadoVotacao inicial = new EstadoVotacao(new Contagem(), new TreeSet<>(), LocalDateTime.now(), null);
        armazenamento.salvar(inicial);
        return new Urna(eleicao, armazenamento, inicial);
    }

    /** Votação nova que não grava nada em disco (útil em testes e demonstrações). */
    public static Urna nova(Eleicao eleicao) {
        return nova(eleicao, ArmazenamentoVotacao.NENHUM);
    }

    /**
     * Continua uma votação gravada anteriormente.
     *
     * @throws IllegalStateException se não houver votação gravada ou se ela não
     *                               corresponder à configuração da eleição
     */
    public static Urna retomar(Eleicao eleicao, ArmazenamentoVotacao armazenamento) {
        EstadoVotacao estado = armazenamento.carregar()
                .orElseThrow(() -> new IllegalStateException("Não há votação gravada para retomar."));
        verificarCompatibilidade(eleicao, estado);
        eleicao.bloquear();
        return new Urna(eleicao, armazenamento, estado);
    }

    /** Garante que a votação gravada só cita cargos, candidatos, partidos e eleitores existentes. */
    private static void verificarCompatibilidade(Eleicao eleicao, EstadoVotacao estado) {
        Contagem c = estado.getContagem();
        for (Map.Entry<String, Map<Integer, Integer>> e : c.getNominais().entrySet()) {
            Cargo cargo = cargoExistente(eleicao, e.getKey());
            for (int numero : e.getValue().keySet()) {
                if (!eleicao.buscarCandidato(cargo, numero).isPresent()) {
                    throw new IllegalStateException("A votação gravada tem votos para o candidato "
                            + numero + " (" + cargo + "), que não existe na eleição.");
                }
            }
        }
        for (Map.Entry<String, Map<Integer, Integer>> e : c.getLegenda().entrySet()) {
            cargoExistente(eleicao, e.getKey());
            for (int numero : e.getValue().keySet()) {
                if (!eleicao.buscarPartido(numero).isPresent()) {
                    throw new IllegalStateException("A votação gravada tem votos para o partido "
                            + numero + ", que não existe na eleição.");
                }
            }
        }
        c.getBrancos().keySet().forEach(cargo -> cargoExistente(eleicao, cargo));
        c.getNulos().keySet().forEach(cargo -> cargoExistente(eleicao, cargo));
        for (String titulo : estado.getTitulosQueVotaram()) {
            if (!eleicao.buscarEleitor(titulo).isPresent()) {
                throw new IllegalStateException("A votação gravada cita o título " + titulo
                        + ", que não está entre os eleitores.");
            }
        }
    }

    private static Cargo cargoExistente(Eleicao eleicao, String nome) {
        return eleicao.buscarCargo(nome).orElseThrow(() ->
                new IllegalStateException("A votação gravada cita o cargo " + nome + ", que não existe na eleição."));
    }

    public Eleicao getEleicao() {
        return eleicao;
    }

    public boolean validarSenhaMesario(String senha) {
        return eleicao.validarSenhaMesario(senha);
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
        if (isEncerrada()) {
            throw new IllegalStateException("A votação já foi encerrada.");
        }
        if (eleitorAtual != null) {
            throw new IllegalStateException("O eleitor " + eleitorAtual.getNome() + " ainda está votando.");
        }
        Eleitor eleitor = eleicao.buscarEleitor(titulo)
                .orElseThrow(() -> new IllegalArgumentException("Título/matrícula não encontrado: " + titulo));
        if (jaVotou(eleitor)) {
            throw new IllegalArgumentException("O eleitor " + eleitor.getNome() + " já votou.");
        }
        eleitorAtual = eleitor;
        votosPendentes.clear();
        return eleitor;
    }

    /** O eleitor desistiu: seus votos pendentes são descartados e ele poderá votar depois. */
    public void cancelarEleitor() {
        eleitorAtual = null;
        votosPendentes.clear();
    }

    public boolean jaVotou(Eleitor eleitor) {
        return titulosQueVotaram.contains(eleitor.getTitulo());
    }

    /** Eleitor liberado que está votando agora, ou {@code null}. */
    public Eleitor getEleitorAtual() {
        return eleitorAtual;
    }

    public int getEleitoresQueVotaram() {
        return titulosQueVotaram.size();
    }

    public int getEleitoresAptos() {
        return eleicao.listarEleitores().size();
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

        Optional<Candidato> candidato = eleicao.buscarCandidato(cargo, numero);
        if (candidato.isPresent()) {
            return Voto.nominal(candidato.get());
        }
        if (cargo.aceitaVotoLegenda()) {
            Optional<Partido> partido = buscarPartidoPeloInicio(digitos);
            if (partido.isPresent()) {
                return Voto.legenda(cargo, partido.get());
            }
        }
        return Voto.nulo(cargo);
    }

    /** Partido cujo número são os 2 primeiros dígitos digitados. */
    public Optional<Partido> buscarPartidoPeloInicio(String digitos) {
        if (digitos == null || digitos.length() < Partido.DIGITOS) {
            return Optional.empty();
        }
        return eleicao.buscarPartido(Integer.parseInt(digitos.substring(0, Partido.DIGITOS)));
    }

    /** Guarda o voto do eleitor atual para um cargo. Só entra na contagem ao finalizar o eleitor. */
    public void registrar(Voto voto) {
        if (isEncerrada()) {
            throw new IllegalStateException("A votação já foi encerrada.");
        }
        if (eleitorAtual == null) {
            throw new IllegalStateException("A urna não foi liberada para nenhum eleitor.");
        }
        if (!eleicao.getCargos().contains(voto.getCargo())) {
            throw new IllegalArgumentException("Cargo inexistente nesta eleição: " + voto.getCargo());
        }
        for (Voto v : votosPendentes) {
            if (v.getCargo().equals(voto.getCargo())) {
                throw new IllegalStateException("O eleitor já votou para " + voto.getCargo() + ".");
            }
        }
        votosPendentes.add(voto);
    }

    /** Cargos que o eleitor atual ainda não votou, na ordem de votação. */
    public List<Cargo> cargosPendentes() {
        List<Cargo> pendentes = new ArrayList<>(eleicao.getCargos());
        for (Voto v : votosPendentes) {
            pendentes.remove(v.getCargo());
        }
        return pendentes;
    }

    /**
     * Conclui o voto do eleitor atual: os votos entram na contagem, o eleitor é
     * marcado como "votou" e o estado é gravado.
     */
    public void finalizarEleitor() {
        if (eleitorAtual == null) {
            throw new IllegalStateException("Nenhum eleitor foi liberado.");
        }
        if (!cargosPendentes().isEmpty()) {
            throw new IllegalStateException("Ainda falta votar para: " + cargosPendentes());
        }
        Contagem novaContagem = contagem.copia();
        votosPendentes.forEach(novaContagem::registrar);
        Set<String> novosVotaram = new TreeSet<>(titulosQueVotaram);
        novosVotaram.add(eleitorAtual.getTitulo());

        // Grava primeiro; só altera a memória se a gravação deu certo.
        armazenamento.salvar(new EstadoVotacao(novaContagem, novosVotaram, abertura, null));
        contagem = novaContagem;
        titulosQueVotaram = novosVotaram;
        cancelarEleitor();
    }

    /** Encerra a votação. Não é possível encerrar com um eleitor votando. */
    public void encerrar() {
        if (eleitorAtual != null) {
            throw new IllegalStateException("Não é possível encerrar: o eleitor "
                    + eleitorAtual.getNome() + " ainda está votando.");
        }
        if (isEncerrada()) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        armazenamento.salvar(new EstadoVotacao(contagem, titulosQueVotaram, abertura, agora));
        encerramento = agora;
    }

    public boolean isEncerrada() {
        return encerramento != null;
    }

    public LocalDateTime getAbertura() {
        return abertura;
    }

    public LocalDateTime getEncerramento() {
        return encerramento;
    }

    // ------------------------------------------------------------------
    // Contagem
    // ------------------------------------------------------------------

    public int getVotosCandidato(Candidato candidato) {
        return contagem.nominais(candidato.getCargo(), candidato.getNumero());
    }

    public int getVotosLegenda(Cargo cargo, Partido partido) {
        return contagem.legenda(cargo, partido.getNumero());
    }

    /** Votos nominais dos candidatos do partido somados aos votos de legenda. */
    public int getVotosPartido(Cargo cargo, Partido partido) {
        int total = getVotosLegenda(cargo, partido);
        for (Candidato c : eleicao.listarCandidatos(cargo)) {
            if (c.getPartido().equals(partido)) {
                total += getVotosCandidato(c);
            }
        }
        return total;
    }

    public int getVotosBrancos(Cargo cargo) {
        return contagem.brancos(cargo);
    }

    public int getVotosNulos(Cargo cargo) {
        return contagem.nulos(cargo);
    }

    /** Votos válidos = nominais + legenda (brancos e nulos não entram). */
    public int getVotosValidos(Cargo cargo) {
        return contagem.validos(cargo);
    }

    public int getTotalVotos(Cargo cargo) {
        return getVotosValidos(cargo) + getVotosBrancos(cargo) + getVotosNulos(cargo);
    }

    /** Candidatos do cargo ordenados do mais votado para o menos votado. */
    public List<Candidato> ranking(Cargo cargo) {
        List<Candidato> lista = new ArrayList<>(eleicao.listarCandidatos(cargo));
        lista.sort(Comparator.comparingInt(this::getVotosCandidato).reversed()
                .thenComparing(Candidato::getNome));
        return lista;
    }
}
