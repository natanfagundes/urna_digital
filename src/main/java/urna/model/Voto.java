package urna.model;

/**
 * Representa um voto para um cargo. O voto é anônimo:
 * não guarda nenhuma informação sobre o eleitor.
 */
public class Voto {

    public enum Tipo {
        /** Voto em um candidato existente. */
        NOMINAL,
        /** Número de candidato inexistente, mas de partido válido (só cargos proporcionais). */
        LEGENDA,
        BRANCO,
        NULO
    }

    private final Cargo cargo;
    private final Tipo tipo;
    private final Candidato candidato;
    private final Partido partido;

    private Voto(Cargo cargo, Tipo tipo, Candidato candidato, Partido partido) {
        this.cargo = cargo;
        this.tipo = tipo;
        this.candidato = candidato;
        this.partido = partido;
    }

    public static Voto nominal(Candidato candidato) {
        return new Voto(candidato.getCargo(), Tipo.NOMINAL, candidato, candidato.getPartido());
    }

    public static Voto legenda(Cargo cargo, Partido partido) {
        return new Voto(cargo, Tipo.LEGENDA, null, partido);
    }

    public static Voto branco(Cargo cargo) {
        return new Voto(cargo, Tipo.BRANCO, null, null);
    }

    public static Voto nulo(Cargo cargo) {
        return new Voto(cargo, Tipo.NULO, null, null);
    }

    public Cargo getCargo() {
        return cargo;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** Candidato votado, ou {@code null} se não for voto nominal. */
    public Candidato getCandidato() {
        return candidato;
    }

    /** Partido do voto (nominal ou de legenda), ou {@code null} para branco/nulo. */
    public Partido getPartido() {
        return partido;
    }
}
