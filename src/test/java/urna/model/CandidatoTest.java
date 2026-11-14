package urna.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidatoTest {

    private final Partido pti = new Partido(12, "PTI", "Partido Teste");
    private final Cargo vereador = Cargo.proporcional("Vereador", 5, 9);
    private final Cargo prefeito = Cargo.majoritario("Prefeito", 2, "Vice-prefeito");

    @Test
    @DisplayName("Candidato válido guarda os dados")
    void candidatoValido() {
        Candidato c = new Candidato(12345, "  Ana Souza ", pti, vereador);
        assertEquals("Ana Souza", c.getNome());
        assertNull(c.getNomeVice());
    }

    @Test
    @DisplayName("Número precisa ter a quantidade de dígitos do cargo")
    void quantidadeDeDigitos() {
        assertThrows(IllegalArgumentException.class, () -> new Candidato(123, "X", pti, vereador));
    }

    @Test
    @DisplayName("Número precisa começar com o número do partido")
    void prefixoDoPartido() {
        assertThrows(IllegalArgumentException.class, () -> new Candidato(25111, "X", pti, vereador));
    }

    @Test
    @DisplayName("Cargo com vice exige o nome do vice")
    void viceObrigatorio() {
        assertThrows(IllegalArgumentException.class, () -> new Candidato(12, "X", pti, prefeito));
        assertEquals("Y", new Candidato(12, "X", pti, prefeito, "Y").getNomeVice());
    }

    @Test
    @DisplayName("Cargos: legenda só em proporcional com mais de 2 dígitos")
    void votoDeLegenda() {
        assertTrue(vereador.aceitaVotoLegenda());
        assertFalse(prefeito.aceitaVotoLegenda());
        assertFalse(Cargo.proporcional("Conselho", 2, 3).aceitaVotoLegenda());
    }

    @Test
    @DisplayName("Cargo e partido com dados inválidos são recusados")
    void validacoesDeCargoEPartido() {
        assertThrows(IllegalArgumentException.class, () -> Cargo.proporcional("X", 6, 3));
        assertThrows(IllegalArgumentException.class, () -> Cargo.proporcional("X", 3, 0));
        assertThrows(IllegalArgumentException.class, () -> Cargo.majoritario(" ", 2, null));
        assertThrows(IllegalArgumentException.class, () -> new Partido(5, "A", "B"));
        assertThrows(IllegalArgumentException.class, () -> new Eleitor("12a", "Nome"));
    }
}
