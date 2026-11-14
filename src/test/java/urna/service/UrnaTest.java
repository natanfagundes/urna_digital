package urna.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import urna.model.Cargo;
import urna.model.Eleicao;
import urna.model.Partido;
import urna.model.Voto;

import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrnaTest {

    private Eleicao eleicao;
    private Cargo vereador;
    private Cargo prefeito;
    private ArmazenamentoEmMemoria armazenamento;
    private Urna urna;

    @BeforeEach
    void preparar() {
        eleicao = Exemplos.municipal();
        vereador = eleicao.buscarCargo("Vereador").get();
        prefeito = eleicao.buscarCargo("Prefeito").get();
        armazenamento = new ArmazenamentoEmMemoria();
        urna = Urna.nova(eleicao, armazenamento);
    }

    /** Vota como o eleitor do título. {@code null} = voto em branco. */
    static void votar(Urna urna, String titulo, String... numeros) {
        urna.liberarEleitor(titulo);
        int i = 0;
        for (Cargo cargo : urna.getEleicao().getCargos()) {
            String n = numeros[i++];
            urna.registrar(n == null ? Voto.branco(cargo) : urna.interpretar(cargo, n));
        }
        urna.finalizarEleitor();
    }

    @Test
    @DisplayName("Interpreta voto nominal, de legenda e nulo")
    void interpretacao() {
        assertEquals(Voto.Tipo.NOMINAL, urna.interpretar(vereador, "12123").getTipo());
        Voto legenda = urna.interpretar(vereador, "12999");
        assertEquals(Voto.Tipo.LEGENDA, legenda.getTipo());
        assertEquals(12, legenda.getPartido().getNumero());
        assertEquals(Voto.Tipo.NULO, urna.interpretar(vereador, "99999").getTipo());
        assertEquals(Voto.Tipo.NULO, urna.interpretar(prefeito, "99").getTipo());
        assertThrows(IllegalArgumentException.class, () -> urna.interpretar(vereador, "123"));
    }

    @Test
    @DisplayName("Cargo majoritário não tem voto de legenda")
    void majoritarioSemLegenda() {
        Eleicao e = Exemplos.municipal();
        e.cadastrarPartido(55, "PXX", "Sem prefeito");
        Urna u = Urna.nova(e);
        assertEquals(Voto.Tipo.NULO, u.interpretar(e.buscarCargo("Prefeito").get(), "55").getTipo());
    }

    @Test
    @DisplayName("Conta válidos, brancos, nulos e votos do partido")
    void contagem() {
        votar(urna, "1001", "12123", "12");
        votar(urna, "1002", "12999", null);   // legenda + branco
        votar(urna, "1003", "99999", "99");   // nulo + nulo

        assertEquals(2, urna.getVotosValidos(vereador));
        assertEquals(1, urna.getVotosNulos(vereador));
        assertEquals(1, urna.getVotosBrancos(prefeito));
        assertEquals(1, urna.getVotosNulos(prefeito));
        assertEquals(3, urna.getTotalVotos(prefeito));
        Partido pti = eleicao.buscarPartido(12).get();
        assertEquals(2, urna.getVotosPartido(vereador, pti));
        assertEquals(3, urna.getEleitoresQueVotaram());
        assertEquals(urna.getEleitoresAptos() - 3, urna.getAbstencoes());
    }

    @Test
    @DisplayName("Os votos só entram na contagem quando o eleitor conclui todos os cargos")
    void votacaoComoTransacao() {
        urna.liberarEleitor("1001");
        urna.registrar(urna.interpretar(vereador, "12123"));
        assertEquals(0, urna.getVotosValidos(vereador));
        assertThrows(IllegalStateException.class, urna::finalizarEleitor); // falta prefeito
        urna.registrar(urna.interpretar(prefeito, "25"));
        urna.finalizarEleitor();
        assertEquals(1, urna.getVotosValidos(vereador));
    }

    @Test
    @DisplayName("O eleitor não pode votar duas vezes no mesmo cargo")
    void umVotoPorCargo() {
        urna.liberarEleitor("1001");
        urna.registrar(Voto.branco(vereador));
        assertThrows(IllegalStateException.class, () -> urna.registrar(Voto.branco(vereador)));
    }

    @Test
    @DisplayName("Eleitor que desiste não tem votos contados e pode votar depois")
    void desistencia() {
        urna.liberarEleitor("1001");
        urna.registrar(urna.interpretar(vereador, "12123"));
        urna.cancelarEleitor();
        assertEquals(0, urna.getVotosValidos(vereador));
        assertEquals(0, urna.getEleitoresQueVotaram());
        votar(urna, "1001", "12123", "12"); // pode votar de novo
        assertEquals(1, urna.getEleitoresQueVotaram());
    }

    @Test
    @DisplayName("Controle de eleitores: voto duplo, título inexistente, urna não liberada")
    void controleDeEleitores() {
        votar(urna, "1001", "12123", "12");
        assertThrows(IllegalArgumentException.class, () -> urna.liberarEleitor("1001"));
        assertThrows(IllegalArgumentException.class, () -> urna.liberarEleitor("0000"));
        assertThrows(IllegalStateException.class, () -> urna.registrar(Voto.branco(prefeito)));
        urna.liberarEleitor("1002");
        assertThrows(IllegalStateException.class, () -> urna.liberarEleitor("1003"));
    }

    @Test
    @DisplayName("Encerramento: bloqueia novos votos e não ocorre com eleitor votando")
    void encerramento() {
        urna.liberarEleitor("1001");
        assertThrows(IllegalStateException.class, urna::encerrar);
        urna.cancelarEleitor();
        urna.encerrar();
        assertTrue(urna.isEncerrada());
        assertThrows(IllegalStateException.class, () -> urna.liberarEleitor("1002"));
    }

    @Test
    @DisplayName("Abrir a votação bloqueia a configuração e exige eleição completa")
    void abertura() {
        assertTrue(eleicao.isBloqueada());
        assertThrows(IllegalStateException.class, () -> Urna.nova(new Eleicao("Vazia")));
    }

    @Test
    @DisplayName("Grava a votação na abertura, a cada eleitor e no encerramento")
    void gravacoes() {
        assertEquals(1, armazenamento.gravacoes);
        votar(urna, "1001", "12123", "12");
        assertEquals(2, armazenamento.gravacoes);
        urna.encerrar();
        assertEquals(3, armazenamento.gravacoes);
        assertTrue(armazenamento.carregar().get().isEncerrada());
    }

    @Test
    @DisplayName("Se a gravação falhar, o voto não é contado")
    void falhaNaGravacao() {
        urna.liberarEleitor("1001");
        urna.registrar(Voto.branco(vereador));
        urna.registrar(Voto.branco(prefeito));
        armazenamento.falhar = true;
        assertThrows(UncheckedIOException.class, urna::finalizarEleitor);
        assertEquals(0, urna.getTotalVotos(vereador));
        assertEquals(0, urna.getEleitoresQueVotaram());
    }

    @Test
    @DisplayName("Retoma a votação gravada com os mesmos totais")
    void retomar() {
        votar(urna, "1001", "12123", "12");
        votar(urna, "1002", "12999", null);

        Eleicao mesmaEleicao = Exemplos.municipal();
        Urna retomada = Urna.retomar(mesmaEleicao, armazenamento);
        Cargo v = mesmaEleicao.buscarCargo("Vereador").get();
        assertEquals(2, retomada.getVotosValidos(v));
        assertEquals(2, retomada.getEleitoresQueVotaram());
        assertThrows(IllegalArgumentException.class, () -> retomada.liberarEleitor("1001"));
        assertEquals(urna.getAbertura(), retomada.getAbertura());
    }

    @Test
    @DisplayName("Não retoma votação incompatível com a eleição")
    void retomarIncompativel() {
        votar(urna, "1001", "12123", "12");
        Eleicao outra = Exemplos.gremioEstudantil();
        assertThrows(IllegalStateException.class, () -> Urna.retomar(outra, armazenamento));
        assertThrows(IllegalStateException.class, () -> Urna.retomar(Exemplos.municipal(), new ArmazenamentoEmMemoria()));
        assertFalse(outra.isBloqueada());
    }
}
