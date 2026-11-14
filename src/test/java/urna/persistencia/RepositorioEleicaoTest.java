package urna.persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Eleicao;
import urna.service.Exemplos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioEleicaoTest {

    @TempDir
    Path pasta;

    private final RepositorioEleicao repositorio = new RepositorioEleicao();

    @Test
    @DisplayName("Salva e carrega a eleição com todos os dados")
    void idaEVolta() throws IOException {
        Eleicao original = Exemplos.gremioEstudantil();
        repositorio.salvar(original, pasta);
        assertTrue(RepositorioEleicao.existeEleicao(pasta));

        Eleicao carregada = repositorio.carregar(pasta);
        assertEquals(original.getNome(), carregada.getNome());
        assertEquals(original.getCargos(), carregada.getCargos());
        Cargo presidente = carregada.getCargos().get(1);
        assertEquals("Vice-presidente", presidente.getRotuloVice());
        assertEquals(Cargo.Tipo.MAJORITARIO, presidente.getTipo());
        assertEquals(5, carregada.getCargos().get(0).getVagas());
        assertEquals(original.listarPartidos().size(), carregada.listarPartidos().size());
        assertEquals(original.listarTodosCandidatos().size(), carregada.listarTodosCandidatos().size());
        assertEquals("Mateus Prado", carregada.buscarCandidato(presidente, 10).get().getNomeVice());
        assertEquals(20, carregada.listarEleitores().size());
        assertTrue(carregada.validarSenhaMesario(Exemplos.SENHA_PADRAO));
        assertFalse(carregada.isBloqueada());
    }

    @Test
    @DisplayName("Copia a foto do candidato para a pasta da eleição")
    void foto() throws IOException {
        Path imagem = pasta.resolve("minha foto.png");
        Files.write(imagem, new byte[]{1, 2, 3});
        Eleicao e = Exemplos.gremioEstudantil();
        Cargo presidente = e.buscarCargo("Presidente do Grêmio").get();
        Candidato laura = e.buscarCandidato(presidente, 10).get();
        e.atualizarCandidato(laura.comFoto(imagem));

        Path destino = pasta.resolve("eleicao");
        repositorio.salvar(e, destino);
        Path copiada = destino.resolve("fotos").resolve("presidente-do-gremio_10.png");
        assertTrue(Files.exists(copiada));

        Candidato carregada = repositorio.carregar(destino).buscarCandidato(presidente, 10).get();
        assertEquals(copiada, carregada.getFoto());
    }

    @Test
    @DisplayName("Informa o arquivo e a linha quando o CSV tem erro")
    void erroNoCsv() throws IOException {
        repositorio.salvar(Exemplos.municipal(), pasta);
        Files.write(pasta.resolve("partidos.csv"), "numero;sigla;nome\n5;X;Inválido\n".getBytes(StandardCharsets.UTF_8));
        IOException erro = assertThrows(IOException.class, () -> repositorio.carregar(pasta));
        assertTrue(erro.getMessage().contains("partidos.csv"));
    }
}
