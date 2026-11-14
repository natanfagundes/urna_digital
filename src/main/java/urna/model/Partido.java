package urna.model;

import urna.util.Textos;

import java.util.Objects;

/**
 * Partido político ou chapa. O número possui sempre 2 dígitos
 * e é o prefixo do número de todos os seus candidatos.
 */
public class Partido {

    public static final int DIGITOS = 2;

    private final int numero;
    private final String sigla;
    private final String nome;

    public Partido(int numero, String sigla, String nome) {
        if (numero < 10 || numero > 99) {
            throw new IllegalArgumentException("O número do partido/chapa deve ter 2 dígitos (10 a 99): " + numero);
        }
        if (Textos.vazio(sigla)) {
            throw new IllegalArgumentException("A sigla do partido/chapa é obrigatória.");
        }
        this.numero = numero;
        this.sigla = sigla.trim().toUpperCase();
        this.nome = Textos.vazio(nome) ? this.sigla : nome.trim();
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
        return numero + " - " + sigla + (nome.equals(sigla) ? "" : " (" + nome + ")");
    }
}
