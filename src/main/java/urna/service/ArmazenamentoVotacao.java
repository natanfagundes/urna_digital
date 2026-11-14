package urna.service;

import java.util.Optional;

/**
 * Onde a urna grava o estado da votação, para que nada se perca se o
 * programa fechar ou o computador desligar no meio da votação.
 */
public interface ArmazenamentoVotacao {

    /** Grava o estado. Deve ser atômico: ou grava tudo, ou nada muda. */
    void salvar(EstadoVotacao estado);

    /** Estado gravado anteriormente, se houver. */
    Optional<EstadoVotacao> carregar();

    /** Armazenamento que não grava nada (a votação fica só na memória). Útil em testes. */
    ArmazenamentoVotacao NENHUM = new ArmazenamentoVotacao() {
        @Override
        public void salvar(EstadoVotacao estado) {
        }

        @Override
        public Optional<EstadoVotacao> carregar() {
            return Optional.empty();
        }
    };
}
