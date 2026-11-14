package urna.service;

import java.util.Optional;

/** Armazenamento de teste: guarda o último estado na memória e conta as gravações. */
class ArmazenamentoEmMemoria implements ArmazenamentoVotacao {

    private EstadoVotacao estado;
    int gravacoes;
    boolean falhar;

    @Override
    public void salvar(EstadoVotacao novo) {
        if (falhar) {
            throw new java.io.UncheckedIOException(new java.io.IOException("disco cheio (simulado)"));
        }
        estado = novo;
        gravacoes++;
    }

    @Override
    public Optional<EstadoVotacao> carregar() {
        return Optional.ofNullable(estado);
    }
}
