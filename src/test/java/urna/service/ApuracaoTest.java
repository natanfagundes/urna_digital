package urna.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Eleicao;
import urna.model.Partido;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static urna.service.UrnaTest.votar;

class ApuracaoTest {

    /** Eleição pequena: Conselho (proporcional, 3 dígitos, N vagas) e Presidente (majoritário, M vagas). */
    private static Eleicao eleicao(int vagasConselho, int vagasPresidente) {
        Eleicao e = new Eleicao("Teste");
        e.setSenhaMesario("1234");
        Cargo conselho = e.adicionarCargo(Cargo.proporcional("Conselho", 3, vagasConselho));
        Cargo presidente = e.adicionarCargo(new Cargo("Presidente", 2, Cargo.Tipo.MAJORITARIO, vagasPresidente, null));
        Partido a = e.cadastrarPartido(10, "A", "Chapa A");
        Partido b = e.cadastrarPartido(20, "B", "Chapa B");
        Partido c = e.cadastrarPartido(30, "C", "Chapa C");
        for (Partido p : Arrays.asList(a, b, c)) {
            e.cadastrarCandidato(new Candidato(p.getNumero(), "Presidente " + p.getSigla(), p, presidente));
            e.cadastrarCandidato(new Candidato(p.getNumero() * 10 + 1, "Cand " + p.getSigla() + "1", p, conselho));
            e.cadastrarCandidato(new Candidato(p.getNumero() * 10 + 2, "Cand " + p.getSigla() + "2", p, conselho));
        }
        for (int i = 1; i <= 30; i++) {
            e.cadastrarEleitor(String.valueOf(i), "Eleitor " + i);
        }
        return e;
    }

    private static List<Integer> numeros(List<Candidato> candidatos) {
        return candidatos.stream().map(Candidato::getNumero).collect(Collectors.toList());
    }

    @Test
    @DisplayName("Majoritário: vence o mais votado; sem votos, ninguém é eleito")
    void majoritario() {
        Urna urna = Urna.nova(eleicao(3, 1));
        Cargo presidente = urna.getEleicao().buscarCargo("Presidente").get();
        Apuracao apuracao = new Apuracao(urna);
        assertTrue(apuracao.eleitos(presidente).isEmpty());

        votar(urna, "1", null, "20");
        votar(urna, "2", null, "20");
        votar(urna, "3", null, "10");
        assertEquals(Arrays.asList(20), numeros(apuracao.eleitos(presidente)));
        assertTrue(apuracao.empateNaUltimaVaga(presidente).isEmpty());
    }

    @Test
    @DisplayName("Majoritário: aponta empate na última vaga")
    void empate() {
        Urna urna = Urna.nova(eleicao(3, 1));
        Cargo presidente = urna.getEleicao().buscarCargo("Presidente").get();
        votar(urna, "1", null, "10");
        votar(urna, "2", null, "20");
        assertEquals(2, new Apuracao(urna).empateNaUltimaVaga(presidente).size());
    }

    @Test
    @DisplayName("Majoritário com 2 vagas elege os 2 mais votados")
    void majoritarioComDuasVagas() {
        Urna urna = Urna.nova(eleicao(3, 2));
        Cargo presidente = urna.getEleicao().buscarCargo("Presidente").get();
        votar(urna, "1", null, "30");
        votar(urna, "2", null, "30");
        votar(urna, "3", null, "10");
        assertEquals(Arrays.asList(30, 10), numeros(new Apuracao(urna).eleitos(presidente)));
    }

    @Test
    @DisplayName("Proporcional: quociente eleitoral, cadeiras por partido e sobras pela maior média")
    void proporcional() {
        // 3 vagas, 10 votos válidos -> QE = round(10/3) = 3
        Urna urna = Urna.nova(eleicao(3, 1));
        Cargo conselho = urna.getEleicao().buscarCargo("Conselho").get();
        String[] votos = {"101", "101", "101", "102", "109",   // A: 5 (um de legenda)
                "201", "201", "202",                          // B: 3
                "301", "301"};                                // C: 2
        for (int i = 0; i < votos.length; i++) {
            votar(urna, String.valueOf(i + 1), votos[i], null);
        }
        Apuracao apuracao = new Apuracao(urna);
        assertEquals(3, apuracao.quocienteEleitoral(conselho));

        // A: 5/3 = 1, B: 3/3 = 1, sobra 1 -> médias A 5/2=2.5, B 3/2=1.5, C 2/1=2 -> A
        Map<Partido, Integer> cadeiras = apuracao.distribuirCadeiras(conselho);
        Eleicao e = urna.getEleicao();
        assertEquals(2, cadeiras.get(e.buscarPartido(10).get()));
        assertEquals(1, cadeiras.get(e.buscarPartido(20).get()));
        assertEquals(0, cadeiras.get(e.buscarPartido(30).get()));
        assertEquals(Arrays.asList(101, 201, 102), numeros(apuracao.eleitos(conselho)));
    }

    @Test
    @DisplayName("Proporcional: candidato sem a votação mínima não ocupa cadeira")
    void votacaoMinima() {
        // 5 vagas, mas só 2 candidatos com votos: as outras 3 vagas ficam sem preencher
        Urna urna = Urna.nova(eleicao(5, 1));
        Cargo conselho = urna.getEleicao().buscarCargo("Conselho").get();
        votar(urna, "1", "101", null);
        votar(urna, "2", "201", null);
        Apuracao apuracao = new Apuracao(urna);
        assertEquals(1, apuracao.votacaoMinima(conselho));
        assertEquals(Arrays.asList(101, 201), numeros(apuracao.eleitos(conselho)));
    }
}
