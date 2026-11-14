package urna.model;

import java.util.Objects;

/**
 * Partido político. O número do partido possui sempre 2 dígitos
 * e é o prefixo do número de todos os seus candidatos.
 */
public class Partido {

    private final int numero;
    private final String sigla;
    private final String nome;

    public Partido(int numero, String sigla, String nome) {
        if (numero < 10 || numero > 99) {
            throw new IllegalArgumentException("O número do partido deve ter 2 dígitos: " + numero);
        }
        if (sigla == null || sigla.trim().isEmpty()) {
            throw new IllegalArgumentException("A sigla do partido é obrigatória.");
        }
        this.numero = numero;
        this.sigla = sigla.trim().toUpperCase();
        this.nome = nome;
    }

    public int getNumero() {
        return numero;
    }

    public String getSigla() {
        return sigla;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Partido)) return false;
        return numero == ((Partido) o).numero;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return numero + " - " + sigla + " (" + nome + ")";
    }
}
