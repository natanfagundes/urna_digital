package urna.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Salva a Zerésima e o Boletim de Urna em arquivos de texto.
 * Cada arquivo recebe um código SHA-256 do conteúdo, que permite
 * verificar se o relatório foi alterado depois de emitido.
 */
public class ArquivoBoletim {

    private static final DateTimeFormatter FORMATO_ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path pasta;

    public ArquivoBoletim(Path pasta) {
        this.pasta = pasta;
    }

    public ArquivoBoletim() {
        this(Paths.get("relatorios"));
    }

    /** Grava o relatório e devolve o caminho do arquivo criado. */
    public Path salvar(String prefixo, String conteudo) throws IOException {
        Files.createDirectories(pasta);
        String nome = prefixo + "_" + LocalDateTime.now().format(FORMATO_ARQUIVO) + ".txt";
        Path arquivo = pasta.resolve(nome);
        String texto = conteudo + "Código de verificação (SHA-256):\n" + sha256(conteudo) + "\n";
        Files.write(arquivo, texto.getBytes(StandardCharsets.UTF_8));
        return arquivo;
    }

    public static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
