package urna.model;

/**
 * Cargos em disputa na eleição.
 * Cada cargo define quantos dígitos o número do candidato possui
 * e se a chapa possui vice.
 */
public enum Cargo {

    VEREADOR("Vereador", 5, false, true),
    PREFEITO("Prefeito", 2, true, false);

    private final String descricao;
    private final int quantidadeDigitos;
    private final boolean possuiVice;
    private final boolean aceitaVotoLegenda;

    Cargo(String descricao, int quantidadeDigitos, boolean possuiVice, boolean aceitaVotoLegenda) {
        this.descricao = descricao;
        this.quantidadeDigitos = quantidadeDigitos;
        this.possuiVice = possuiVice;
        this.aceitaVotoLegenda = aceitaVotoLegenda;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getQuantidadeDigitos() {
        return quantidadeDigitos;
    }

    public boolean possuiVice() {
        return possuiVice;
    }

    /** Cargos proporcionais (ex.: vereador) aceitam voto apenas no partido. */
    public boolean aceitaVotoLegenda() {
        return aceitaVotoLegenda;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
