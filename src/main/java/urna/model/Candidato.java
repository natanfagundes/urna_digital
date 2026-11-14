package urna.model;

import urna.util.Textos;

import java.nio.file.Path;
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
    private final Path foto;

    public Candidato(int numero, String nome, Partido partido, Cargo cargo) {
        this(numero, nome, partido, cargo, null, null);
    }

    public Candidato(int numero, String nome, Partido partido, Cargo cargo, String nomeVice) {
        this(numero, nome, partido, cargo, nomeVice, null);
    }

    /**
     * @param nomeVice obrigatório se o cargo possui vice; ignorado caso contrário
     * @param foto     arquivo de imagem do candidato, ou {@code null}
     */
    public Candidato(int numero, String nome, Partido partido, Cargo cargo, String nomeVice, Path foto) {
        if (Textos.vazio(nome)) {
            throw new IllegalArgumentException("O nome do candidato é obrigatório.");
        }
        Objects.requireNonNull(partido, "O partido/chapa é obrigatório.");
        Objects.requireNonNull(cargo, "O cargo é obrigatório.");

        String numeroTexto = String.valueOf(numero);
        if (numeroTexto.length() != cargo.getQuantidadeDigitos()) {
            throw new IllegalArgumentException("Candidato a " + cargo + " deve ter "
                    + cargo.getQuantidadeDigitos() + " dígitos: " + numero);
        }
        if (!numeroTexto.startsWith(String.valueOf(partido.getNumero()))) {
            throw new IllegalArgumentException("O número " + numero
                    + " deve começar com o número do partido/chapa (" + partido.getNumero() + ").");
        }
        if (cargo.possuiVice() && Textos.vazio(nomeVice)) {
            throw new IllegalArgumentException("Candidato a " + cargo + " precisa de "
                    + cargo.getRotuloVice() + ".");
        }

        this.numero = numero;
        this.nome = nome.trim();
        this.partido = partido;
        this.cargo = cargo;
        this.nomeVice = cargo.possuiVice() ? nomeVice.trim() : null;
        this.foto = foto;
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

    /** Arquivo da foto, ou {@code null}. */
    public Path getFoto() {
        return foto;
    }

    /** Cópia deste candidato com outra foto. */
    public Candidato comFoto(Path novaFoto) {
        return new Candidato(numero, nome, partido, cargo, nomeVice, novaFoto);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Candidato)) return false;
        Candidato outro = (Candidato) o;
        return numero == outro.numero && cargo.equals(outro.cargo);
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
