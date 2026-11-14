package urna.service;

import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Eleicao;
import urna.model.Partido;

/** Eleições de exemplo, com dados fictícios, para demonstração e aulas. */
public final class Exemplos {

    public static final String SENHA_PADRAO = "1234";

    private Exemplos() {
    }

    /** Eleição municipal: Vereador (proporcional, 9 vagas) e Prefeito (majoritário, com vice). */
    public static Eleicao municipal() {
        Eleicao e = new Eleicao("Eleição Municipal (exemplo)");
        e.setSenhaMesario(SENHA_PADRAO);

        Cargo vereador = e.adicionarCargo(Cargo.proporcional("Vereador", 5, 9));
        Cargo prefeito = e.adicionarCargo(Cargo.majoritario("Prefeito", 2, "Vice-prefeito"));

        Partido pti = e.cadastrarPartido(12, "PTI", "Partido da Tecnologia e Inovação");
        Partido pev = e.cadastrarPartido(25, "PEV", "Partido da Educação e Valorização");
        Partido pms = e.cadastrarPartido(40, "PMS", "Partido do Meio Ambiente Sustentável");
        Partido pss = e.cadastrarPartido(77, "PSS", "Partido da Saúde e Segurança");

        e.cadastrarCandidato(new Candidato(12, "Ana Souza", pti, prefeito, "Carlos Lima"));
        e.cadastrarCandidato(new Candidato(25, "Bruno Ferreira", pev, prefeito, "Juliana Rocha"));
        e.cadastrarCandidato(new Candidato(40, "Mariana Alves", pms, prefeito, "Pedro Santos"));
        e.cadastrarCandidato(new Candidato(77, "Ricardo Gomes", pss, prefeito, "Fernanda Dias"));

        e.cadastrarCandidato(new Candidato(12123, "João Pereira", pti, vereador));
        e.cadastrarCandidato(new Candidato(12456, "Larissa Martins", pti, vereador));
        e.cadastrarCandidato(new Candidato(25111, "Paulo Oliveira", pev, vereador));
        e.cadastrarCandidato(new Candidato(25222, "Camila Ribeiro", pev, vereador));
        e.cadastrarCandidato(new Candidato(40001, "Lucas Barbosa", pms, vereador));
        e.cadastrarCandidato(new Candidato(40002, "Beatriz Costa", pms, vereador));
        e.cadastrarCandidato(new Candidato(77777, "Marcos Teixeira", pss, vereador));
        e.cadastrarCandidato(new Candidato(77100, "Patrícia Nunes", pss, vereador));

        String[] nomes = {"Adriana Moreira", "Bernardo Castro", "Cecília Ramos", "Daniel Freitas",
                "Eduarda Pinto", "Felipe Cardoso", "Gabriela Mendes", "Henrique Lopes",
                "Isabela Duarte", "Jorge Almeida", "Kátia Vieira", "Leonardo Araújo",
                "Natália Correia", "Otávio Monteiro", "Renata Campos"};
        for (int i = 0; i < nomes.length; i++) {
            e.cadastrarEleitor(String.valueOf(1001 + i), nomes[i]);
        }
        return e;
    }

    /**
     * Eleição de grêmio estudantil: Conselho de Representantes (proporcional, 5 vagas)
     * e Presidente do Grêmio (majoritário, com vice). Os "partidos" são as chapas.
     */
    public static Eleicao gremioEstudantil() {
        Eleicao e = new Eleicao("Grêmio Estudantil (exemplo)");
        e.setSenhaMesario(SENHA_PADRAO);

        Cargo conselho = e.adicionarCargo(Cargo.proporcional("Conselho de Representantes", 3, 5));
        Cargo presidente = e.adicionarCargo(Cargo.majoritario("Presidente do Grêmio", 2, "Vice-presidente"));

        Partido vozAtiva = e.cadastrarPartido(10, "VOZ ATIVA", "Chapa Voz Ativa");
        Partido movimento = e.cadastrarPartido(20, "MOVIMENTO", "Chapa Escola em Movimento");
        Partido juntos = e.cadastrarPartido(30, "JUNTOS", "Chapa Juntos pela Escola");

        e.cadastrarCandidato(new Candidato(10, "Laura Siqueira", vozAtiva, presidente, "Mateus Prado"));
        e.cadastrarCandidato(new Candidato(20, "Rafael Antunes", movimento, presidente, "Sofia Brandão"));
        e.cadastrarCandidato(new Candidato(30, "Yasmin Torres", juntos, presidente, "Caio Nogueira"));

        e.cadastrarCandidato(new Candidato(101, "Alice Fontes", vozAtiva, conselho));
        e.cadastrarCandidato(new Candidato(102, "Davi Rezende", vozAtiva, conselho));
        e.cadastrarCandidato(new Candidato(201, "Helena Barros", movimento, conselho));
        e.cadastrarCandidato(new Candidato(202, "Miguel Peixoto", movimento, conselho));
        e.cadastrarCandidato(new Candidato(301, "Lívia Cunha", juntos, conselho));
        e.cadastrarCandidato(new Candidato(302, "Arthur Macedo", juntos, conselho));

        String[] alunos = {"Ana Clara Dias", "Bruno Henrique Sá", "Carolina Matos", "Diego Farias",
                "Emanuelly Rocha", "Fábio Lacerda", "Giovana Pires", "Igor Medeiros",
                "Júlia Batista", "Kauã Moura", "Letícia Guimarães", "Murilo Tavares",
                "Nicole Fernandes", "Pedro Henrique Luz", "Rebeca Amaral", "Samuel Coelho",
                "Thaís Brito", "Vinícius Lima", "Wesley Carvalho", "Yara Moreira"};
        for (int i = 0; i < alunos.length; i++) {
            e.cadastrarEleitor(String.valueOf(2026001 + i), alunos[i]);
        }
        return e;
    }
}
