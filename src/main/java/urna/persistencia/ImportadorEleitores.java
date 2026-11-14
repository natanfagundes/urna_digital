package urna.persistencia;

import urna.model.Eleicao;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Importa eleitores de uma planilha CSV com duas colunas: título (ou matrícula) e nome.
 * A primeira linha pode ser um cabeçalho; ela é ignorada se o primeiro campo não for número.
 */
public class ImportadorEleitores {

    public static final class Resultado {
        private final int importados;
        private final List<String> erros;

        Resultado(int importados, List<String> erros) {
            this.importados = importados;
            this.erros = Collections.unmodifiableList(erros);
        }

        public int getImportados() {
            return importados;
        }

        /** Linhas recusadas, com o motivo. */
        public List<String> getErros() {
            return erros;
        }
    }

    public Resultado importar(Path arquivo, Eleicao eleicao) throws IOException {
        List<List<String>> linhas = Csv.ler(arquivo);
        int importados = 0;
        List<String> erros = new ArrayList<>();
        for (int i = 0; i < linhas.size(); i++) {
            List<String> l = linhas.get(i);
            String titulo = l.isEmpty() ? "" : l.get(0);
            if (i == 0 && !titulo.matches("\\d+")) {
                continue; // cabeçalho
            }
            try {
                if (l.size() < 2) {
                    throw new IllegalArgumentException("falta a coluna do nome");
                }
                eleicao.cadastrarEleitor(titulo, l.get(1));
                importados++;
            } catch (IllegalArgumentException e) {
                erros.add("Linha " + (i + 1) + ": " + e.getMessage());
            }
        }
        return new Resultado(importados, erros);
    }
}
