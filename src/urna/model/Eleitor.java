package urna.model;

import java.util.Objects;

/**
 * Eleitor apto a votar na seção, identificado pelo título de eleitor.
 */
public class Eleitor {

    private final String titulo;
    private final String nome;

    public Eleitor(String titulo, String nome) {
        if (titulo == null || !titulo.trim().matches("\\d+")) {
            throw new IllegalArgumentException("O título de eleitor deve conter apenas números: " + titulo);
        }
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do eleitor é obrigatório.");
        }
        this.titulo = titulo.trim();
        this.nome = nome.trim();
    }

    public String getTitulo() {
        return titulo;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Eleitor)) return false;
        return titulo.equals(((Eleitor) o).titulo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(titulo);
    }

    @Override
    public String toString() {
        return titulo + " - " + nome;
    }
}
