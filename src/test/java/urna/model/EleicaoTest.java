package urna.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EleicaoTest {

    private Eleicao eleicao;
    private Cargo presidente;
    private Partido chapa;

    @BeforeEach
    void preparar() {
        eleicao = new Eleicao("Grêmio 2026");
        presidente = eleicao.adicionarCargo(Cargo.majoritario("Presidente", 2, null));
        chapa = eleicao.cadastrarPartido(10, "VOZ", "Chapa Voz");
    }

    @Test
    @DisplayName("Não aceita cargos, partidos, candidatos ou eleitores repetidos")
    void semRepeticoes() {
        assertThrows(IllegalArgumentException.class, () -> eleicao.adicionarCargo(Cargo.majoritario("presidente", 2, null)));
        assertThrows(IllegalArgumentException.class, () -> eleicao.cadastrarPartido(10, "OUTRA", ""));
        eleicao.cadastrarCandidato(new Candidato(10, "Laura", chapa, presidente));
        assertThrows(IllegalArgumentException.class, () -> eleicao.cadastrarCandidato(new Candidato(10, "Outra", chapa, presidente)));
        eleicao.cadastrarEleitor("1", "Ana");
        assertThrows(IllegalArgumentException.class, () -> eleicao.cadastrarEleitor("1", "Bia"));
    }

    @Test
    @DisplayName("Não remove cargo ou partido que ainda tem candidatos")
    void remocaoProtegida() {
        Candidato c = eleicao.cadastrarCandidato(new Candidato(10, "Laura", chapa, presidente));
        assertThrows(IllegalArgumentException.class, () -> eleicao.removerCargo(presidente));
        assertThrows(IllegalArgumentException.class, () -> eleicao.removerPartido(chapa));
        eleicao.removerCandidato(c);
        eleicao.removerCargo(presidente);
        eleicao.removerPartido(chapa);
        assertTrue(eleicao.getCargos().isEmpty());
        assertTrue(eleicao.listarPartidos().isEmpty());
    }

    @Test
    @DisplayName("Candidato só pode usar partido cadastrado na eleição")
    void partidoDeOutraEleicao() {
        Partido estranho = new Partido(20, "X", "Não cadastrado");
        assertThrows(IllegalArgumentException.class, () -> eleicao.cadastrarCandidato(new Candidato(20, "Y", estranho, presidente)));
    }

    @Test
    @DisplayName("A ordem dos cargos pode ser alterada")
    void ordemDosCargos() {
        Cargo conselho = eleicao.adicionarCargo(Cargo.proporcional("Conselho", 3, 5));
        eleicao.moverCargo(conselho, -1);
        assertEquals(Arrays.asList(conselho, presidente), eleicao.getCargos());
        eleicao.moverCargo(conselho, -1); // já é o primeiro: nada muda
        assertEquals(conselho, eleicao.getCargos().get(0));
    }

    @Test
    @DisplayName("Lista o que falta para iniciar a votação")
    void pendencias() {
        assertEquals(3, eleicao.verificarPendencias().size()); // sem candidatos, sem eleitores, sem senha
        eleicao.cadastrarCandidato(new Candidato(10, "Laura", chapa, presidente));
        eleicao.cadastrarEleitor("1", "Ana");
        eleicao.setSenhaMesario("segredo");
        assertTrue(eleicao.verificarPendencias().isEmpty());
    }

    @Test
    @DisplayName("Senha do mesário: guarda só o resumo e exige tamanho mínimo")
    void senha() {
        assertThrows(IllegalArgumentException.class, () -> eleicao.setSenhaMesario("123"));
        eleicao.setSenhaMesario("1234");
        assertTrue(eleicao.validarSenhaMesario("1234"));
        assertFalse(eleicao.validarSenhaMesario("4321"));
        assertNotEquals("1234", eleicao.getHashSenhaMesario());
        assertEquals(64, eleicao.getHashSenhaMesario().length());
    }

    @Test
    @DisplayName("Depois de bloqueada, a eleição não pode mais ser alterada")
    void bloqueio() {
        eleicao.bloquear();
        assertThrows(IllegalStateException.class, () -> eleicao.cadastrarEleitor("9", "Zé"));
        assertThrows(IllegalStateException.class, () -> eleicao.setNome("Outro nome"));
        assertThrows(IllegalStateException.class, () -> eleicao.adicionarCargo(Cargo.majoritario("Vice", 2, null)));
    }
}
