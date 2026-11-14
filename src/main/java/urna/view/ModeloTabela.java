package urna.view;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Modelo de tabela genérico: cada coluna é um título e uma função que extrai o valor. */
final class ModeloTabela<T> extends AbstractTableModel {

    private final List<String> titulos = new ArrayList<>();
    private final List<Function<T, Object>> extratores = new ArrayList<>();
    private List<T> linhas = new ArrayList<>();

    ModeloTabela<T> coluna(String titulo, Function<T, Object> extrator) {
        titulos.add(titulo);
        extratores.add(extrator);
        return this;
    }

    void setLinhas(List<T> novasLinhas) {
        this.linhas = new ArrayList<>(novasLinhas);
        fireTableDataChanged();
    }

    T getLinha(int indice) {
        return linhas.get(indice);
    }

    @Override
    public int getRowCount() {
        return linhas.size();
    }

    @Override
    public int getColumnCount() {
        return titulos.size();
    }

    @Override
    public String getColumnName(int coluna) {
        return titulos.get(coluna);
    }

    @Override
    public Object getValueAt(int linha, int coluna) {
        Object valor = extratores.get(coluna).apply(linhas.get(linha));
        return valor == null ? "" : valor;
    }
}
