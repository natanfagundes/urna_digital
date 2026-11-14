package urna.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Fotografia da votação num instante: contagem, quem já votou,
 * horários de abertura e encerramento. É o que fica gravado em disco.
 *
 * Os títulos de quem votou ficam em ordem numérica (e não na ordem de votação),
 * para que não seja possível descobrir em quem cada eleitor votou.
 */
public final class EstadoVotacao {

    private final Contagem contagem;
    private final Set<String> titulosQueVotaram;
    private final LocalDateTime abertura;
    private final LocalDateTime encerramento;

    public EstadoVotacao(Contagem contagem, Set<String> titulosQueVotaram,
                         LocalDateTime abertura, LocalDateTime encerramento) {
        this.contagem = contagem.copia();
        this.titulosQueVotaram = Collections.unmodifiableSet(new TreeSet<>(titulosQueVotaram));
        this.abertura = abertura;
        this.encerramento = encerramento;
    }

    public Contagem getContagem() {
        return contagem.copia();
    }

    public Set<String> getTitulosQueVotaram() {
        return titulosQueVotaram;
    }

    public LocalDateTime getAbertura() {
        return abertura;
    }

    /** Horário de encerramento, ou {@code null} se a votação está em andamento. */
    public LocalDateTime getEncerramento() {
        return encerramento;
    }

    public boolean isEncerrada() {
        return encerramento != null;
    }
}
