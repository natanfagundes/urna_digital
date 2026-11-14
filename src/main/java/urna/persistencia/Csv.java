package urna.persistencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Leitura e escrita de arquivos CSV simples, compatíveis com o Excel em português
 * (separador ";" e codificação UTF-8 com BOM). Na leitura, também aceita ",".
 *
 * Campos com separador ou aspas são escritos entre aspas. Quebras de linha dentro
 * de um campo não são suportadas.
 */
public final class Csv {

    public static final char SEPARADOR = ';';
    private static final String BOM = "﻿";

    private Csv() {
    }

    /** Lê todas as linhas não vazias, detectando o separador pela primeira linha. */
    public static List<List<String>> ler(Path arquivo) throws IOException {
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        List<List<String>> resultado = new ArrayList<>();
        Character separador = null;
        for (String linha : linhas) {
            if (linha.startsWith(BOM)) {
                linha = linha.substring(1);
            }
            if (linha.trim().isEmpty()) {
                continue;
            }
            if (separador == null) {
                separador = linha.indexOf(SEPARADOR) >= 0 || linha.indexOf(',') < 0 ? SEPARADOR : ',';
            }
            resultado.add(lerLinha(linha, separador));
        }
        return resultado;
    }

    public static void escrever(Path arquivo, List<List<String>> linhas) throws IOException {
        StringBuilder sb = new StringBuilder(BOM);
        for (List<String> campos : linhas) {
            sb.append(formatarLinha(campos, SEPARADOR)).append("\r\n");
        }
        Files.write(arquivo, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static String formatarLinha(List<String> campos, char separador) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.size(); i++) {
            if (i > 0) {
                sb.append(separador);
            }
            String campo = campos.get(i) == null ? "" : campos.get(i);
            if (campo.indexOf(separador) >= 0 || campo.indexOf('"') >= 0) {
                sb.append('"').append(campo.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(campo);
            }
        }
        return sb.toString();
    }

    public static List<String> lerLinha(String linha, char separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas) {
                if (c == '"' && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else if (c == '"') {
                    entreAspas = false;
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == separador) {
                campos.add(atual.toString().trim());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString().trim());
        return campos;
    }
}
