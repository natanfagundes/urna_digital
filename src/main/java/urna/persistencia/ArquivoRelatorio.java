package urna.persistencia;

import urna.util.Hash;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Salva a Zerésima e o Boletim de Urna em arquivos de texto.
 * Cada arquivo recebe um código SHA-256 do conteúdo, que permite
 * verificar se o relatório foi alterado depois de emitido.
 */
public class ArquivoRelatorio {

    private static final DateTimeFormatter FORMATO_ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path pasta;

    public ArquivoRelatorio(Path pasta) {
        this.pasta = pasta;
    }

    /** Grava o relatório e devolve o caminho do arquivo criado. */
    public Path salvar(String prefixo, String conteudo) throws IOException {
        Files.createDirectories(pasta);
        String nome = prefixo + "_" + LocalDateTime.now().format(FORMATO_ARQUIVO) + ".txt";
        Path arquivo = pasta.resolve(nome);
        String texto = conteudo + "Código de verificação (SHA-256):\n" + Hash.sha256(conteudo) + "\n";
        Files.write(arquivo, texto.getBytes(StandardCharsets.UTF_8));
        return arquivo;
    }
}
