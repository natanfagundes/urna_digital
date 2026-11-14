package urna.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static urna.service.UrnaTest.votar;

class RelatoriosTest {

    @Test
    @DisplayName("Zerésima mostra todos os candidatos com zero votos")
    void zeresima() {
        Urna urna = Urna.nova(Exemplos.gremioEstudantil());
        String texto = Relatorios.zeresima(urna);
        assertTrue(texto.contains("ZERÉSIMA"));
        assertTrue(texto.contains("Laura Siqueira"));
        assertTrue(texto.contains("ZERO votos: SIM"));
    }

    @Test
    @DisplayName("Boletim só existe depois do encerramento e traz os eleitos")
    void boletim() {
        Urna urna = Urna.nova(Exemplos.gremioEstudantil());
        votar(urna, "2026001", "101", "10");
        votar(urna, "2026002", "201", "10");
        assertThrows(IllegalStateException.class, () -> Relatorios.boletim(urna));

        urna.encerrar();
        String texto = Relatorios.boletim(urna);
        assertTrue(texto.contains("BOLETIM DE URNA"));
        assertTrue(texto.contains("Comparecimento:    2"));
        assertTrue(texto.contains("10 - Laura Siqueira (VOZ ATIVA) - 2 votos | Vice-presidente: Mateus Prado"));
        assertTrue(texto.contains("Quociente eleitoral"));
        assertFalse(texto.contains("empate"));
    }
}
