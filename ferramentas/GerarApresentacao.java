import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Eleicao;
import urna.model.Partido;
import urna.model.Voto;
import urna.persistencia.ArmazenamentoEmArquivo;
import urna.persistencia.ArquivoRelatorio;
import urna.persistencia.RepositorioEleicao;
import urna.service.Relatorios;
import urna.service.Urna;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Gera três eleições de demonstração (dados fictícios) para apresentar o projeto:
 *
 *   1. pronta para votar          -> demonstrar a votação ao vivo
 *   2. votação em andamento        -> demonstrar "Retomar votação"
 *   3. votação encerrada           -> demonstrar o Boletim de Urna e a apuração
 *
 * Uso (na raiz do projeto, depois de compilar para out/main):
 *   java -cp out/main ferramentas/GerarApresentacao.java [pasta-das-eleicoes]
 *
 * A pasta padrão é "eleicoes". Para o .exe, use dist/eleicoes.
 * Os votos são sorteados com semente fixa: rodar de novo gera exatamente o mesmo resultado.
 */
public class GerarApresentacao {

    static final String SENHA = "1234";
    static final int TOTAL_ELEITORES = 400;

    public static void main(String[] args) throws Exception {
        Path base = Paths.get(args.length > 0 ? args[0] : "eleicoes");

        Eleicao pronta = criar("Apresentação 1 - Grêmio 2026 (pronta para votar)");
        salvar(pronta, base, 0, false);

        Eleicao andamento = criar("Apresentação 2 - Grêmio 2026 (votação em andamento)");
        salvar(andamento, base, 147, false);

        Eleicao encerrada = criar("Apresentação 3 - Grêmio 2026 (votação encerrada)");
        Urna urna = salvar(encerrada, base, 352, true);
        System.out.println();
        System.out.println(Relatorios.boletim(urna));
    }

    // ------------------------------------------------------------------
    // Configuração da eleição
    // ------------------------------------------------------------------

    static Eleicao criar(String nome) {
        Eleicao e = new Eleicao(nome);
        e.setSenhaMesario(SENHA);

        Cargo conselho = e.adicionarCargo(Cargo.proporcional("Conselho de Representantes", 3, 7));
        Cargo cultura = e.adicionarCargo(Cargo.majoritario("Diretor de Cultura e Eventos", 2, null));
        Cargo esportes = e.adicionarCargo(Cargo.majoritario("Diretor de Esportes", 2, null));
        Cargo presidente = e.adicionarCargo(Cargo.majoritario("Presidente do Grêmio", 2, "Vice-presidente"));

        Partido voz = e.cadastrarPartido(10, "VOZ ATIVA", "Chapa Voz Ativa");
        Partido mov = e.cadastrarPartido(20, "MOVIMENTO", "Chapa Escola em Movimento");
        Partido jun = e.cadastrarPartido(30, "JUNTOS", "Chapa Juntos pela Escola");
        Partido nova = e.cadastrarPartido(40, "NOVA GERAÇÃO", "Chapa Nova Geração");
        Partido con = e.cadastrarPartido(50, "CONECTA", "Chapa Conecta");

        // Presidente e vice (a chapa Conecta não concorre a presidente)
        e.cadastrarCandidato(new Candidato(10, "Laura Siqueira", voz, presidente, "Mateus Prado"));
        e.cadastrarCandidato(new Candidato(20, "Rafael Antunes", mov, presidente, "Sofia Brandão"));
        e.cadastrarCandidato(new Candidato(30, "Yasmin Torres", jun, presidente, "Caio Nogueira"));
        e.cadastrarCandidato(new Candidato(40, "Gustavo Henrique Melo", nova, presidente, "Bianca Rezende"));

        // Cultura e eventos (a Conecta também não concorre)
        e.cadastrarCandidato(new Candidato(10, "Manuela Castro", voz, cultura));
        e.cadastrarCandidato(new Candidato(20, "Enzo Gabriel Ramos", mov, cultura));
        e.cadastrarCandidato(new Candidato(30, "Clara Vasconcelos", jun, cultura));
        e.cadastrarCandidato(new Candidato(40, "Lorenzo Batista", nova, cultura));

        // Esportes (todas as chapas)
        e.cadastrarCandidato(new Candidato(10, "Pedro Lucas Farias", voz, esportes));
        e.cadastrarCandidato(new Candidato(20, "Valentina Moreira", mov, esportes));
        e.cadastrarCandidato(new Candidato(30, "Heitor Santana", jun, esportes));
        e.cadastrarCandidato(new Candidato(40, "Isadora Lima", nova, esportes));
        e.cadastrarCandidato(new Candidato(50, "João Miguel Teixeira", con, esportes));

        // Conselho de Representantes: 4 candidatos por chapa
        String[][] conselheiros = {
                {"Alice Fontes", "Davi Rezende", "Beatriz Moura", "Samuel Duarte"},
                {"Helena Barros", "Miguel Peixoto", "Lara Cardoso", "Benício Araújo"},
                {"Lívia Cunha", "Arthur Macedo", "Cecília Prates", "Bernardo Lopes"},
                {"Sophia Albuquerque", "Theo Gonçalves", "Antonella Ribeiro", "Nicolas Freire"},
                {"Maria Eduarda Pinto", "Ravi Monteiro", "Agatha Campos", "Joaquim Dantas"},
        };
        Partido[] chapas = {voz, mov, jun, nova, con};
        for (int c = 0; c < chapas.length; c++) {
            for (int i = 0; i < 4; i++) {
                int numero = chapas[c].getNumero() * 10 + i + 1; // 101..104, 201..204, ...
                e.cadastrarCandidato(new Candidato(numero, conselheiros[c][i], chapas[c], conselho));
            }
        }

        for (String[] aluno : alunos()) {
            e.cadastrarEleitor(aluno[0], aluno[1]);
        }
        return e;
    }

    /** 400 alunos fictícios, matrículas 2026001 a 2026400, ordenados por nome. */
    static List<String[]> alunos() {
        String[] nomes = {"Ana", "Bruno", "Carolina", "Diego", "Eduarda", "Fábio", "Giovana", "Igor", "Júlia", "Kauã",
                "Letícia", "Murilo", "Nicole", "Otávio", "Paula", "Rafaela", "Samuel", "Tainá", "Vinícius", "Yasmin",
                "Amanda", "Caio", "Débora", "Emanuel", "Fernanda", "Gabriel", "Heloísa", "Ícaro", "Jéssica", "Kevin",
                "Luana", "Matheus", "Natália", "Pietro", "Raíssa", "Thiago", "Vitória", "Wesley", "Yuri", "Zoe"};
        String[] sobrenomes = {"Almeida", "Barbosa", "Cavalcanti", "Duarte", "Esteves", "Ferraz", "Guimarães", "Holanda",
                "Lacerda", "Machado", "Nascimento", "Oliveira", "Pacheco", "Quintana", "Rocha", "Siqueira", "Tavares",
                "Uchôa", "Vieira", "Xavier"};
        List<String> nomesCompletos = new ArrayList<>();
        for (int i = 0; i < TOTAL_ELEITORES; i++) {
            // combinação determinística e sem repetição (40 nomes x 20 sobrenomes = 800 opções)
            nomesCompletos.add(nomes[i % nomes.length] + " " + sobrenomes[(i / nomes.length + i * 7) % sobrenomes.length]);
        }
        Collections.sort(nomesCompletos);
        List<String[]> alunos = new ArrayList<>();
        for (int i = 0; i < nomesCompletos.size(); i++) {
            alunos.add(new String[]{String.valueOf(2026001 + i), nomesCompletos.get(i)});
        }
        return alunos;
    }

    // ------------------------------------------------------------------
    // Votação simulada
    // ------------------------------------------------------------------

    /** Preferência do eleitorado em cada cargo: número digitado e peso. "B" = branco. */
    static final Object[][][] PREFERENCIAS = {
            // Conselho de Representantes (inclui votos de legenda "1xx/2xx..." e nulos "999")
            {{"101", 31}, {"102", 12}, {"103", 9}, {"104", 4}, {"109", 6},
             {"201", 24}, {"202", 15}, {"203", 6}, {"204", 3}, {"209", 4},
             {"301", 18}, {"302", 11}, {"303", 7}, {"304", 2}, {"309", 3},
             {"401", 13}, {"402", 6}, {"403", 3}, {"404", 2},
             {"501", 9}, {"502", 4}, {"503", 2}, {"504", 1},
             {"B", 8}, {"999", 5}},
            // Diretor de Cultura e Eventos
            {{"10", 25}, {"20", 33}, {"30", 21}, {"40", 12}, {"B", 6}, {"77", 3}},
            // Diretor de Esportes
            {{"10", 22}, {"20", 18}, {"30", 15}, {"40", 9}, {"50", 27}, {"B", 6}, {"88", 3}},
            // Presidente do Grêmio
            {{"10", 35}, {"20", 29}, {"30", 20}, {"40", 10}, {"B", 4}, {"99", 2}},
    };

    static Urna salvar(Eleicao eleicao, Path base, int votantes, boolean encerrar) throws Exception {
        Path pasta = base.resolve(urna.util.Textos.slug(eleicao.getNome()));
        apagarVotacaoAnterior(pasta);
        new RepositorioEleicao().salvar(eleicao, pasta);
        if (votantes == 0) {
            System.out.println("Criada: " + pasta + " (pronta para votar)");
            return null;
        }

        ArmazenamentoEmArquivo armazenamento = new ArmazenamentoEmArquivo(RepositorioEleicao.pastaVotacao(pasta));
        Urna urna = Urna.nova(eleicao, armazenamento);
        ArquivoRelatorio relatorios = new ArquivoRelatorio(RepositorioEleicao.pastaRelatorios(pasta));
        relatorios.salvar("zeresima", Relatorios.zeresima(urna));

        // Quem compareceu: sorteado entre os alunos, com semente fixa
        Random sorteio = new Random(2026);
        List<String> titulos = new ArrayList<>();
        eleicao.listarEleitores().forEach(el -> titulos.add(el.getTitulo()));
        Collections.shuffle(titulos, sorteio);

        for (String titulo : titulos.subList(0, votantes)) {
            urna.liberarEleitor(titulo);
            for (int c = 0; c < eleicao.getCargos().size(); c++) {
                Cargo cargo = eleicao.getCargos().get(c);
                String escolha = sortear(PREFERENCIAS[c], sorteio);
                urna.registrar(escolha.equals("B") ? Voto.branco(cargo) : urna.interpretar(cargo, escolha));
            }
            urna.finalizarEleitor();
        }
        if (encerrar) {
            urna.encerrar();
            relatorios.salvar("boletim_urna", Relatorios.boletim(urna));
        }
        System.out.println("Criada: " + pasta + " (" + votantes + " de " + TOTAL_ELEITORES + " votaram"
                + (encerrar ? ", encerrada)" : ", em andamento)"));
        return urna;
    }

    static String sortear(Object[][] opcoes, Random sorteio) {
        int total = 0;
        for (Object[] o : opcoes) total += (Integer) o[1];
        int r = sorteio.nextInt(total);
        for (Object[] o : opcoes) {
            r -= (Integer) o[1];
            if (r < 0) return (String) o[0];
        }
        throw new IllegalStateException();
    }

    /** Para poder rodar de novo: remove votação e relatórios antigos dessa eleição de demonstração. */
    static void apagarVotacaoAnterior(Path pasta) throws Exception {
        for (Path p : new Path[]{RepositorioEleicao.pastaVotacao(pasta), RepositorioEleicao.pastaRelatorios(pasta)}) {
            if (Files.exists(p)) {
                try (Stream<Path> arquivos = Files.walk(p)) {
                    arquivos.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
                }
            }
        }
    }
}
