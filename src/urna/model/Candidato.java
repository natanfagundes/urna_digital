package urna.model;

import java.util.Objects;

/**
 * Candidato a um cargo. O número do candidato deve ter a quantidade
 * de dígitos exigida pelo cargo e começar com o número do partido.
 */
public class Candidato {

    private final int numero;
    private final String nome;
    private final Partido partido;
    private final Cargo cargo;
    private final String nomeVice;

    public Candidato(int numero, String nome, Partido partido, Cargo cargo) {
        this(numero, nome, partido, cargo, null);
    }

    public Candidato(int numero, String nome, Partido partido, Cargo cargo, String nomeVice) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do candidato é obrigatório.");
        }
        Objects.requireNonNull(partido, "O partido é obrigatório.");
        Objects.requireNonNull(cargo, "O cargo é obrigatório.");

        String numeroTexto = String.valueOf(numero);
        if (numeroTexto.length() != cargo.getQuantidadeDigitos()) {
            throw new IllegalArgumentException("Candidato a " + cargo + " deve ter "
                    + cargo.getQuantidadeDigitos() + " dígitos: " + numero);
        }
        if (!numeroTexto.startsWith(String.valueOf(partido.getNumero()))) {
            throw new IllegalArgumentException("O número " + numero
                    + " deve começar com o número do partido " + partido.getNumero());
        }
        if (cargo.possuiVice() && (nomeVice == null || nomeVice.trim().isEmpty())) {
            throw new IllegalArgumentException("Candidato a " + cargo + " precisa de um vice.");
        }

        this.numero = numero;
        this.nome = nome.trim();
        this.partido = partido;
        this.cargo = cargo;
        this.nomeVice = cargo.possuiVice() ? nomeVice.trim() : null;
    }

    public int getNumero() {
        return numero;
    }

    public String getNome() {
        return nome;
    }

    public Partido getPartido() {
        return partido;
    }

    public Cargo getCargo() {
        return cargo;
    }

    /** Nome do vice, ou {@code null} se o cargo não possui vice. */
    public String getNomeVice() {
        return nomeVice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Candidato)) return false;
        Candidato outro = (Candidato) o;
        return numero == outro.numero && cargo == outro.cargo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero, cargo);
    }

    @Override
    public String toString() {
        return numero + " - " + nome + " (" + partido.getSigla() + ")";
    }
}
