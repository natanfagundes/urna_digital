package urna.persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import urna.model.Cargo;
import urna.model.Eleicao;
import urna.model.Voto;
import urna.service.Exemplos;
import urna.service.Urna;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmazenamentoEmArquivoTest {

    @TempDir
    Path pasta;

    private static void votar(Urna urna, String titulo, String vereador, String prefeito) {
        urna.liberarEleitor(titulo);
        urna.registrar(urna.interpretar(urna.getEleicao().getCargos().get(0), vereador));
        urna.registrar(urna.interpretar(urna.getEleicao().getCargos().get(1), prefeito));
        urna.finalizarEleitor();
    }

    @Test
    @DisplayName("Depois de fechar o programa, a votação continua de onde parou")
    void retomarDoDisco() {
        Path votacao = pasta.resolve("votacao");
        Urna urna = Urna.nova(Exemplos.municipal(), new ArmazenamentoEmArquivo(votacao));
        votar(urna, "1001", "12123", "12");
        votar(urna, "1002", "12999", "99");
        // eleitor 1003 começa a votar e o computador desliga antes do FIM
        urna.liberarEleitor("1003");
        urna.registrar(Voto.branco(urna.getEleicao().getCargos().get(0)));

        Eleicao reaberta = Exemplos.municipal();
        Urna retomada = Urna.retomar(reaberta, new ArmazenamentoEmArquivo(votacao));
        Cargo vereador = reaberta.buscarCargo("Vereador").get();
        Cargo prefeito = reaberta.buscarCargo("Prefeito").get();
        assertEquals(2, retomada.getEleitoresQueVotaram());
        assertEquals(1, retomada.getVotosCandidato(reaberta.buscarCandidato(vereador, 12123).get()));
        assertEquals(1, retomada.getVotosLegenda(vereador, reaberta.buscarPartido(12).get()));
        assertEquals(1, retomada.getVotosNulos(prefeito));
        assertEquals(0, retomada.getVotosBrancos(vereador)); // voto incompleto do 1003 não contou
        retomada.liberarEleitor("1003"); // e ele pode votar
    }

    @Test
    @DisplayName("O arquivo não liga eleitores aos votos (títulos em ordem numérica)")
    void sigilo() throws Exception {
        Path votacao = pasta.resolve("votacao");
        Urna urna = Urna.nova(Exemplos.municipal(), new ArmazenamentoEmArquivo(votacao));
        votar(urna, "1009", "12123", "12");
        votar(urna, "1001", "25111", "25");
        String conteudo = new String(Files.readAllBytes(votacao.resolve("estado.txt")), StandardCharsets.UTF_8);
        assertTrue(conteudo.indexOf("VOTOU;1001") < conteudo.indexOf("VOTOU;1009"));
    }

    @Test
    @DisplayName("Detecta alteração manual no arquivo de votação")
    void adulteracao() throws Exception {
        Path votacao = pasta.resolve("votacao");
        Urna urna = Urna.nova(Exemplos.municipal(), new ArmazenamentoEmArquivo(votacao));
        votar(urna, "1001", "12123", "12");

        Path arquivo = votacao.resolve("estado.txt");
        String original = new String(Files.readAllBytes(arquivo), StandardCharsets.UTF_8);
        Files.write(arquivo, original.replace("NOMINAL;Prefeito;12;1", "NOMINAL;Prefeito;12;500")
                .getBytes(StandardCharsets.UTF_8));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> new ArmazenamentoEmArquivo(votacao).carregar());
        assertTrue(erro.getMessage().contains("alterado"));
    }

    @Test
    @DisplayName("Encerramento fica gravado")
    void encerramentoGravado() {
        Path votacao = pasta.resolve("votacao");
        Urna urna = Urna.nova(Exemplos.municipal(), new ArmazenamentoEmArquivo(votacao));
        urna.encerrar();
        assertTrue(new ArmazenamentoEmArquivo(votacao).carregar().get().isEncerrada());
    }

    @Test
    @DisplayName("Arquivar guarda a votação em outra pasta para começar uma nova")
    void arquivar() throws Exception {
        Path votacao = pasta.resolve("votacao");
        ArmazenamentoEmArquivo armazenamento = new ArmazenamentoEmArquivo(votacao);
        Urna.nova(Exemplos.municipal(), armazenamento);
        assertTrue(armazenamento.existe());

        Path arquivada = armazenamento.arquivar();
        assertFalse(armazenamento.existe());
        assertTrue(Files.exists(arquivada.resolve("estado.txt")));
    }
}
