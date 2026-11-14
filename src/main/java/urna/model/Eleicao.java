package urna.model;

import urna.util.Hash;
import urna.util.Textos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuração completa de uma eleição: cargos (na ordem em que são votados),
 * partidos/chapas, candidatos, eleitores e a senha do mesário.
 *
 * Depois que a votação começa, a eleição é bloqueada e não pode mais ser alterada.
 */
public class Eleicao {

    public static final int TAMANHO_MINIMO_SENHA = 4;

    private String nome;
    private String hashSenhaMesario;
    private boolean bloqueada;

    private final List<Cargo> cargos = new ArrayList<>();
    private final Map<Integer, Partido> partidos = new LinkedHashMap<>();
    private final Map<Cargo, Map<Integer, Candidato>> candidatos = new LinkedHashMap<>();
    private final Map<String, Eleitor> eleitores = new LinkedHashMap<>();

    public Eleicao(String nome) {
        setNome(nome);
    }

    // ------------------------------------------------------------------
    // Dados gerais
    // ------------------------------------------------------------------

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        verificarDesbloqueada();
        if (Textos.vazio(nome)) {
            throw new IllegalArgumentException("O nome da eleição é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public void setSenhaMesario(String senha) {
        verificarDesbloqueada();
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException("A senha do mesário deve ter pelo menos "
                    + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        this.hashSenhaMesario = calcularHashSenha(senha);
    }

    /** A senha nunca é guardada: só o seu resumo SHA-256. */
    public String getHashSenhaMesario() {
        return hashSenhaMesario;
    }

    /** Usado ao carregar uma eleição salva. */
    public void setHashSenhaMesario(String hash) {
        verificarDesbloqueada();
        this.hashSenhaMesario = hash;
    }

    public boolean possuiSenhaMesario() {
        return hashSenhaMesario != null;
    }

    public boolean validarSenhaMesario(String senha) {
        return hashSenhaMesario != null && senha != null && hashSenhaMesario.equals(calcularHashSenha(senha));
    }

    private static String calcularHashSenha(String senha) {
        return Hash.sha256("urna-digital:" + senha);
    }

    public boolean isBloqueada() {
        return bloqueada;
    }

    /** Impede novas alterações. Chamado quando a votação começa. */
    public void bloquear() {
        bloqueada = true;
    }

    private void verificarDesbloqueada() {
        if (bloqueada) {
            throw new IllegalStateException("A eleição não pode ser alterada depois que a votação começou.");
        }
    }

    // ------------------------------------------------------------------
    // Cargos
    // ------------------------------------------------------------------

    public Cargo adicionarCargo(Cargo cargo) {
        verificarDesbloqueada();
        if (candidatos.containsKey(cargo)) {
            throw new IllegalArgumentException("Já existe o cargo " + cargo.getNome());
        }
        cargos.add(cargo);
        candidatos.put(cargo, new LinkedHashMap<>());
        return cargo;
    }

    public void removerCargo(Cargo cargo) {
        verificarDesbloqueada();
        if (!listarCandidatos(cargo).isEmpty()) {
            throw new IllegalArgumentException("Remova primeiro os candidatos a " + cargo.getNome() + ".");
        }
        cargos.remove(cargo);
        candidatos.remove(cargo);
    }

    /** Muda a posição do cargo na ordem de votação (-1 sobe, +1 desce). */
    public void moverCargo(Cargo cargo, int deslocamento) {
        verificarDesbloqueada();
        int atual = cargos.indexOf(cargo);
        int nova = atual + deslocamento;
        if (atual < 0 || nova < 0 || nova >= cargos.size()) {
            return;
        }
        Collections.swap(cargos, atual, nova);
    }

    /** Cargos na ordem em que o eleitor vota. */
    public List<Cargo> getCargos() {
        return Collections.unmodifiableList(cargos);
    }

    public Optional<Cargo> buscarCargo(String nome) {
        for (Cargo c : cargos) {
            if (c.getNome().equalsIgnoreCase(nome == null ? "" : nome.trim())) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // Partidos / chapas
    // ------------------------------------------------------------------

    public Partido cadastrarPartido(int numero, String sigla, String nome) {
        verificarDesbloqueada();
        if (partidos.containsKey(numero)) {
            throw new IllegalArgumentException("Já existe um partido/chapa com o número " + numero);
        }
        Partido partido = new Partido(numero, sigla, nome);
        partidos.put(numero, partido);
        return partido;
    }

    public void removerPartido(Partido partido) {
        verificarDesbloqueada();
        for (Map<Integer, Candidato> doCargo : candidatos.values()) {
            for (Candidato c : doCargo.values()) {
                if (c.getPartido().equals(partido)) {
                    throw new IllegalArgumentException("Remova primeiro os candidatos de " + partido.getSigla() + ".");
                }
            }
        }
        partidos.remove(partido.getNumero());
    }

    public Optional<Partido> buscarPartido(int numero) {
        return Optional.ofNullable(partidos.get(numero));
    }

    public List<Partido> listarPartidos() {
        return new ArrayList<>(partidos.values());
    }

    // ------------------------------------------------------------------
    // Candidatos
    // ------------------------------------------------------------------

    public Candidato cadastrarCandidato(Candidato candidato) {
        verificarDesbloqueada();
        Map<Integer, Candidato> doCargo = candidatos.get(candidato.getCargo());
        if (doCargo == null) {
            throw new IllegalArgumentException("Cargo não cadastrado: " + candidato.getCargo());
        }
        if (!candidato.getPartido().equals(partidos.get(candidato.getPartido().getNumero()))) {
            throw new IllegalArgumentException("Partido/chapa não cadastrado: " + candidato.getPartido());
        }
        if (doCargo.containsKey(candidato.getNumero())) {
            throw new IllegalArgumentException("Já existe candidato a " + candidato.getCargo()
                    + " com o número " + candidato.getNumero());
        }
        doCargo.put(candidato.getNumero(), candidato);
        return candidato;
    }

    /** Substitui um candidato já cadastrado (ex.: para trocar a foto). */
    public void atualizarCandidato(Candidato candidato) {
        verificarDesbloqueada();
        Map<Integer, Candidato> doCargo = candidatos.get(candidato.getCargo());
        if (doCargo == null || !doCargo.containsKey(candidato.getNumero())) {
            throw new IllegalArgumentException("Candidato não cadastrado: " + candidato);
        }
        doCargo.put(candidato.getNumero(), candidato);
    }

    public void removerCandidato(Candidato candidato) {
        verificarDesbloqueada();
        Map<Integer, Candidato> doCargo = candidatos.get(candidato.getCargo());
        if (doCargo != null) {
            doCargo.remove(candidato.getNumero());
        }
    }

    public Optional<Candidato> buscarCandidato(Cargo cargo, int numero) {
        Map<Integer, Candidato> doCargo = candidatos.get(cargo);
        return doCargo == null ? Optional.empty() : Optional.ofNullable(doCargo.get(numero));
    }

    public List<Candidato> listarCandidatos(Cargo cargo) {
        Map<Integer, Candidato> doCargo = candidatos.get(cargo);
        return doCargo == null ? new ArrayList<>() : new ArrayList<>(doCargo.values());
    }

    /** Todos os candidatos, agrupados na ordem dos cargos. */
    public List<Candidato> listarTodosCandidatos() {
        List<Candidato> todos = new ArrayList<>();
        for (Cargo c : cargos) {
            todos.addAll(listarCandidatos(c));
        }
        return todos;
    }

    // ------------------------------------------------------------------
    // Eleitores
    // ------------------------------------------------------------------

    public Eleitor cadastrarEleitor(String titulo, String nome) {
        verificarDesbloqueada();
        Eleitor eleitor = new Eleitor(titulo, nome);
        if (eleitores.containsKey(eleitor.getTitulo())) {
            throw new IllegalArgumentException("Já existe eleitor com o título/matrícula " + eleitor.getTitulo());
        }
        eleitores.put(eleitor.getTitulo(), eleitor);
        return eleitor;
    }

    public void removerEleitor(Eleitor eleitor) {
        verificarDesbloqueada();
        eleitores.remove(eleitor.getTitulo());
    }

    public Optional<Eleitor> buscarEleitor(String titulo) {
        return titulo == null ? Optional.empty() : Optional.ofNullable(eleitores.get(titulo.trim()));
    }

    public List<Eleitor> listarEleitores() {
        return new ArrayList<>(eleitores.values());
    }

    // ------------------------------------------------------------------
    // Validação
    // ------------------------------------------------------------------

    /** Lista o que falta para a votação poder começar (vazia = tudo certo). */
    public List<String> verificarPendencias() {
        List<String> pendencias = new ArrayList<>();
        if (cargos.isEmpty()) {
            pendencias.add("Cadastre pelo menos um cargo.");
        }
        for (Cargo c : cargos) {
            if (listarCandidatos(c).isEmpty()) {
                pendencias.add("O cargo " + c.getNome() + " não tem candidatos.");
            }
        }
        if (eleitores.isEmpty()) {
            pendencias.add("Cadastre ou importe os eleitores.");
        }
        if (!possuiSenhaMesario()) {
            pendencias.add("Defina a senha do mesário.");
        }
        return pendencias;
    }
}
