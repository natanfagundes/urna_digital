package urna.model;

import urna.util.Textos;

import java.util.Locale;
import java.util.Objects;

/**
 * Cargo em disputa (ex.: Prefeito, Vereador, Presidente do Grêmio).
 *
 * <ul>
 *   <li><b>Majoritário:</b> vencem os mais votados (uma vaga = o mais votado).</li>
 *   <li><b>Proporcional:</b> as vagas são divididas entre os partidos/chapas pelo
 *       quociente eleitoral, e aceita voto de legenda.</li>
 * </ul>
 */
public final class Cargo {

    public enum Tipo {
        MAJORITARIO("Majoritário"),
        PROPORCIONAL("Proporcional");

        private final String descricao;

        Tipo(String descricao) {
            this.descricao = descricao;
        }

        @Override
        public String toString() {
            return descricao;
        }
    }

    public static final int MIN_DIGITOS = Partido.DIGITOS;
    public static final int MAX_DIGITOS = 5;

    private final String nome;
    private final int quantidadeDigitos;
    private final Tipo tipo;
    private final int vagas;
    private final String rotuloVice;

    /**
     * @param rotuloVice nome do cargo do vice (ex.: "Vice-prefeito"), ou {@code null} se não há vice
     */
    public Cargo(String nome, int quantidadeDigitos, Tipo tipo, int vagas, String rotuloVice) {
        if (Textos.vazio(nome)) {
            throw new IllegalArgumentException("O nome do cargo é obrigatório.");
        }
        if (quantidadeDigitos < MIN_DIGITOS || quantidadeDigitos > MAX_DIGITOS) {
            throw new IllegalArgumentException("O número dos candidatos deve ter de "
                    + MIN_DIGITOS + " a " + MAX_DIGITOS + " dígitos.");
        }
        if (vagas < 1) {
            throw new IllegalArgumentException("O cargo deve ter pelo menos 1 vaga.");
        }
        this.nome = nome.trim();
        this.quantidadeDigitos = quantidadeDigitos;
        this.tipo = Objects.requireNonNull(tipo, "O tipo do cargo é obrigatório.");
        this.vagas = vagas;
        this.rotuloVice = Textos.vazio(rotuloVice) ? null : rotuloVice.trim();
    }

    public static Cargo majoritario(String nome, int digitos, String rotuloVice) {
        return new Cargo(nome, digitos, Tipo.MAJORITARIO, 1, rotuloVice);
    }

    public static Cargo proporcional(String nome, int digitos, int vagas) {
        return new Cargo(nome, digitos, Tipo.PROPORCIONAL, vagas, null);
    }

    public String getNome() {
        return nome;
    }

    public int getQuantidadeDigitos() {
        return quantidadeDigitos;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getVagas() {
        return vagas;
    }

    public boolean possuiVice() {
        return rotuloVice != null;
    }

    /** Nome do cargo do vice, ou {@code null}. */
    public String getRotuloVice() {
        return rotuloVice;
    }

    public boolean isProporcional() {
        return tipo == Tipo.PROPORCIONAL;
    }

    /**
     * Voto de legenda: digitar um número de candidato inexistente, mas cujos
     * primeiros dígitos são de um partido válido. Só existe em cargos proporcionais
     * cujo número é maior que o número do partido.
     */
    public boolean aceitaVotoLegenda() {
        return isProporcional() && quantidadeDigitos > Partido.DIGITOS;
    }

    /** Cargos são identificados pelo nome, sem diferenciar maiúsculas. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cargo)) return false;
        return chave().equals(((Cargo) o).chave());
    }

    @Override
    public int hashCode() {
        return chave().hashCode();
    }

    private String chave() {
        return nome.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return nome;
    }
}
