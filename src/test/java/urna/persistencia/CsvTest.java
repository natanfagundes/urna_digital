package urna.persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvTest {

    @TempDir
    Path pasta;

    @Test
    @DisplayName("Escreve e lê de volta, inclusive campos com ; e aspas")
    void idaEVolta() throws Exception {
        Path arquivo = pasta.resolve("dados.csv");
        List<List<String>> linhas = Arrays.asList(
                Arrays.asList("nome", "obs"),
                Arrays.asList("José; o \"Zé\"", "ação"));
        Csv.escrever(arquivo, linhas);
        assertEquals(linhas, Csv.ler(arquivo));
    }

    @Test
    @DisplayName("Lê arquivo separado por vírgula e ignora linhas vazias")
    void separadorVirgula() throws Exception {
        Path arquivo = pasta.resolve("virgula.csv");
        Files.write(arquivo, "titulo,nome\n\n123,Ana\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(Arrays.asList(Arrays.asList("titulo", "nome"), Arrays.asList("123", "Ana")), Csv.ler(arquivo));
    }
}
