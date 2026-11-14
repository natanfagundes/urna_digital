package urna.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextosTest {

    @Test
    @DisplayName("Gera nomes de pasta sem acentos nem espaços")
    void slug() {
        assertEquals("gremio-estudantil-2026", Textos.slug("  Grêmio Estudantil 2026! "));
        assertEquals("eleicao", Textos.slug("???"));
    }

    @Test
    @DisplayName("Singular e plural de votos")
    void votos() {
        assertEquals("1 voto", Textos.votos(1));
        assertEquals("0 votos", Textos.votos(0));
    }

    @Test
    @DisplayName("SHA-256 conhecido")
    void sha256() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Hash.sha256("abc"));
    }
}
