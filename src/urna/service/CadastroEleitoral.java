package urna.service;

import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Eleitor;
import urna.model.Partido;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda os partidos, candidatos e eleitores registrados para a eleição.
 */
public class CadastroEleitoral {

    private final Map<Integer, Partido> partidos = new LinkedHashMap<>();
    private final Map<Cargo, Map<Integer, Candidato>> candidatos = new EnumMap<>(Cargo.class);
    private final Map<String, Eleitor> eleitores = new LinkedHashMap<>();

    public CadastroEleitoral() {
        for (Cargo cargo : Cargo.values()) {
            candidatos.put(cargo, new LinkedHashMap<>());
        }
    }

    public Partido cadastrarPartido(int numero, String sigla, String nome) {
        if (partidos.containsKey(numero)) {
            throw new IllegalArgumentException("Já existe um partido com o número " + numero);
        }
        Partido partido = new Partido(numero, sigla, nome);
        partidos.put(numero, partido);
        return partido;
    }

    public Candidato cadastrarCandidato(Candidato candidato) {
        if (!partidos.containsKey(candidato.getPartido().getNumero())) {
            throw new IllegalArgumentException("Partido não cadastrado: " + candidato.getPartido());
        }
        Map<Integer, Candidato> doCargo = candidatos.get(candidato.getCargo());
        if (doCargo.containsKey(candidato.getNumero())) {
            throw new IllegalArgumentException("Já existe candidato a " + candidato.getCargo()
                    + " com o número " + candidato.getNumero());
        }
        doCargo.put(candidato.getNumero(), candidato);
        return candidato;
    }

    public Eleitor cadastrarEleitor(String titulo, String nome) {
        Eleitor eleitor = new Eleitor(titulo, nome);
        if (eleitores.containsKey(eleitor.getTitulo())) {
            throw new IllegalArgumentException("Já existe eleitor com o título " + eleitor.getTitulo());
        }
        eleitores.put(eleitor.getTitulo(), eleitor);
        return eleitor;
    }

    public Optional<Eleitor> buscarEleitor(String titulo) {
        return titulo == null ? Optional.empty() : Optional.ofNullable(eleitores.get(titulo.trim()));
    }

    public List<Eleitor> listarEleitores() {
        return new ArrayList<>(eleitores.values());
    }

    public Optional<Partido> buscarPartido(int numero) {
        return Optional.ofNullable(partidos.get(numero));
    }

    public Optional<Candidato> buscarCandidato(Cargo cargo, int numero) {
        return Optional.ofNullable(candidatos.get(cargo).get(numero));
    }

    public List<Partido> listarPartidos() {
        return new ArrayList<>(partidos.values());
    }

    public List<Candidato> listarCandidatos(Cargo cargo) {
        return new ArrayList<>(candidatos.get(cargo).values());
    }

    /** Cria um cadastro com partidos, candidatos e eleitores fictícios para demonstração. */
    public static CadastroEleitoral comDadosDeExemplo() {
        CadastroEleitoral c = new CadastroEleitoral();

        Partido pti = c.cadastrarPartido(12, "PTI", "Partido da Tecnologia e Inovação");
        Partido pev = c.cadastrarPartido(25, "PEV", "Partido da Educação e Valorização");
        Partido pms = c.cadastrarPartido(40, "PMS", "Partido do Meio Ambiente Sustentável");
        Partido pss = c.cadastrarPartido(77, "PSS", "Partido da Saúde e Segurança");

        // Prefeito (2 dígitos = número do partido) com vice
        c.cadastrarCandidato(new Candidato(12, "Ana Souza", pti, Cargo.PREFEITO, "Carlos Lima"));
        c.cadastrarCandidato(new Candidato(25, "Bruno Ferreira", pev, Cargo.PREFEITO, "Juliana Rocha"));
        c.cadastrarCandidato(new Candidato(40, "Mariana Alves", pms, Cargo.PREFEITO, "Pedro Santos"));
        c.cadastrarCandidato(new Candidato(77, "Ricardo Gomes", pss, Cargo.PREFEITO, "Fernanda Dias"));

        // Vereador (5 dígitos, começando pelo número do partido)
        c.cadastrarCandidato(new Candidato(12123, "João Pereira", pti, Cargo.VEREADOR));
        c.cadastrarCandidato(new Candidato(12456, "Larissa Martins", pti, Cargo.VEREADOR));
        c.cadastrarCandidato(new Candidato(25111, "Paulo Oliveira", pev, Cargo.VEREADOR));
        c.cadastrarCandidato(new Candidato(25222, "Camila Ribeiro", pev, Cargo.VEREADOR));
        c.cadastrarCandidato(new Candidato(40001, "Lucas Barbosa", pms, Cargo.VEREADOR));
        c.cadastrarCandidato(new Candidato(40002, "Beatriz Costa", pms, Cargo.VEREADOR));
        c.cadastrarCandidato(new Candidato(77777, "Marcos Teixeira", pss, Cargo.VEREADOR));
        c.cadastrarCandidato(new Candidato(77100, "Patrícia Nunes", pss, Cargo.VEREADOR));

        // Eleitores aptos a votar nesta seção
        String[] nomes = {"Adriana Moreira", "Bernardo Castro", "Cecília Ramos", "Daniel Freitas",
                "Eduarda Pinto", "Felipe Cardoso", "Gabriela Mendes", "Henrique Lopes",
                "Isabela Duarte", "Jorge Almeida", "Kátia Vieira", "Leonardo Araújo",
                "Natália Correia", "Otávio Monteiro", "Renata Campos"};
        for (int i = 0; i < nomes.length; i++) {
            c.cadastrarEleitor(String.valueOf(1001 + i), nomes[i]);
        }

        return c;
    }
}
