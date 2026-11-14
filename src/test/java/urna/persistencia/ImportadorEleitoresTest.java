package urna.persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import urna.model.Eleicao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportadorEleitoresTest {

    @TempDir
    Path pasta;

    @Test
    @DisplayName("Importa a planilha, pula o cabeçalho e relata as linhas com erro")
    void importar() throws Exception {
        Path csv = pasta.resolve("alunos.csv");
        String conteudo = "﻿Matrícula;Nome do aluno\r\n"
                + "2026001;Ana Clara\r\n"
                + "2026002;Bruno\r\n"
                + "2026001;Repetido\r\n"
                + "ABC;Sem número\r\n"
                + "2026003\r\n";
        Files.write(csv, conteudo.getBytes(StandardCharsets.UTF_8));

        Eleicao eleicao = new Eleicao("Escola");
        ImportadorEleitores.Resultado r = new ImportadorEleitores().importar(csv, eleicao);

        assertEquals(2, r.getImportados());
        assertEquals(3, r.getErros().size());
        assertTrue(r.getErros().get(0).startsWith("Linha 4"));
        assertEquals("Ana Clara", eleicao.buscarEleitor("2026001").get().getNome());
    }
}
