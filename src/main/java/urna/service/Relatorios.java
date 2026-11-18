package urna.service;

import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Partido;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static urna.util.Textos.votos;

/** Textos da Zerésima e do Boletim de Urna. */
public final class Relatorios {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Relatorios() {
    }

    /**
     * Zerésima: relatório emitido antes do primeiro voto, provando que
     * todos os candidatos começam com zero votos.
     */
    public static String zeresima(Urna urna) {
        StringBuilder sb = new StringBuilder();
        sb.append("=============== ZERÉSIMA ===============\n");
        sb.append(urna.getEleicao().getNome()).append('\n');
        sb.append("Emitida em: ").append(LocalDateTime.now().format(FORMATO_DATA)).append('\n');
        sb.append("Eleitores aptos: ").append(urna.getEleitoresAptos()).append("\n\n");
        for (Cargo cargo : urna.getEleicao().getCargos()) {
            sb.append("------ ").append(cargo.getNome().toUpperCase()).append(" ------\n");
            for (Candidato c : urna.getEleicao().listarCandidatos(cargo)) {
                sb.append(String.format("%-6d %-24s %-" + larguraSigla(urna) + "s %11s%n",
                        c.getNumero(), c.getNome(), c.getPartido().getSigla(), votos(urna.getVotosCandidato(c))));
            }
            sb.append(String.format("Brancos: %d   Nulos: %d   Total: %d%n%n",
                    urna.getVotosBrancos(cargo), urna.getVotosNulos(cargo), urna.getTotalVotos(cargo)));
        }
        sb.append("Todos os candidatos estão com ZERO votos: ")
                .append(urna.getEleitoresQueVotaram() == 0 ? "SIM" : "NÃO").append('\n');
        sb.append("=========================================\n");
        return sb.toString();
    }

    /** Boletim de Urna com a apuração e os eleitos de todos os cargos. */
    public static String boletim(Urna urna) {
        if (!urna.isEncerrada()) {
            throw new IllegalStateException("O Boletim de Urna só pode ser emitido após o encerramento.");
        }
        Apuracao apuracao = new Apuracao(urna);
        StringBuilder sb = new StringBuilder();
        sb.append("========== BOLETIM DE URNA ==========\n");
        sb.append(urna.getEleicao().getNome()).append('\n');
        sb.append("Abertura:      ").append(urna.getAbertura().format(FORMATO_DATA)).append('\n');
        sb.append("Encerramento:  ").append(urna.getEncerramento().format(FORMATO_DATA)).append('\n');
        sb.append("Eleitores aptos:   ").append(urna.getEleitoresAptos()).append('\n');
        sb.append("Comparecimento:    ").append(urna.getEleitoresQueVotaram()).append('\n');
        sb.append("Abstenções:        ").append(urna.getAbstencoes()).append("\n\n");

        for (Cargo cargo : urna.getEleicao().getCargos()) {
            int validos = urna.getVotosValidos(cargo);
            sb.append("------ ").append(cargo.getNome().toUpperCase())
                    .append(" (").append(cargo.getTipo().toString().toLowerCase())
                    .append(", ").append(cargo.getVagas()).append(cargo.getVagas() == 1 ? " vaga" : " vagas")
                    .append(") ------\n");

            for (Candidato c : urna.ranking(cargo)) {
                int v = urna.getVotosCandidato(c);
                sb.append(String.format("%-6d %-24s %-" + larguraSigla(urna) + "s %11s  %6s%n",
                        c.getNumero(), c.getNome(), c.getPartido().getSigla(), votos(v), percentual(v, validos)));
            }

            if (cargo.isProporcional()) {
                Map<Partido, Integer> cadeiras = apuracao.distribuirCadeiras(cargo);
                sb.append("\nPor partido/chapa (nominais + legenda):\n");
                for (Partido p : urna.getEleicao().listarPartidos()) {
                    sb.append(String.format("%-6d %-" + larguraSigla(urna) + "s %11s  (legenda: %d)  cadeiras: %d%n",
                            p.getNumero(), p.getSigla(), votos(urna.getVotosPartido(cargo, p)),
                            urna.getVotosLegenda(cargo, p), cadeiras.get(p)));
                }
            }

            sb.append('\n');
            sb.append("Votos válidos: ").append(validos).append('\n');
            sb.append("Votos brancos: ").append(urna.getVotosBrancos(cargo)).append('\n');
            sb.append("Votos nulos:   ").append(urna.getVotosNulos(cargo)).append('\n');
            sb.append("Total:         ").append(urna.getTotalVotos(cargo)).append("\n\n");

            if (cargo.isProporcional()) {
                sb.append("Quociente eleitoral: ").append(apuracao.quocienteEleitoral(cargo))
                        .append("   Votação mínima individual: ").append(votos(apuracao.votacaoMinima(cargo)))
                        .append('\n');
            }
            List<Candidato> eleitos = apuracao.eleitos(cargo);
            sb.append(eleitos.size() == 1 ? "ELEITO:" : "ELEITOS:").append(eleitos.isEmpty() ? " nenhum" : "").append('\n');
            for (Candidato c : eleitos) {
                sb.append("  ").append(c).append(" - ").append(votos(urna.getVotosCandidato(c)));
                if (c.getNomeVice() != null) {
                    sb.append(" | ").append(cargo.getRotuloVice()).append(": ").append(c.getNomeVice());
                }
                sb.append('\n');
            }
            if (eleitos.size() < cargo.getVagas()) {
                sb.append("Vagas não preenchidas: ").append(cargo.getVagas() - eleitos.size()).append('\n');
            }
            if (!cargo.isProporcional()) {
                List<Candidato> empatados = apuracao.empateNaUltimaVaga(cargo);
                if (!empatados.isEmpty()) {
                    sb.append("ATENÇÃO: empate entre ");
                    for (int i = 0; i < empatados.size(); i++) {
                        sb.append(i > 0 ? ", " : "").append(empatados.get(i).getNome());
                    }
                    sb.append(". Pela lei, vence o candidato mais idoso.\n");
                }
            }
            sb.append('\n');
        }
        sb.append("=====================================\n");
        return sb.toString();
    }

    /** Largura da coluna de siglas, para alinhar mesmo com siglas longas ("NOVA GERAÇÃO"). */
    private static int larguraSigla(Urna urna) {
        int largura = 6;
        for (Partido p : urna.getEleicao().listarPartidos()) {
            largura = Math.max(largura, p.getSigla().length());
        }
        return largura;
    }

    private static String percentual(int parte, int total) {
        if (total == 0) return "0,0%";
        return String.format("%.1f%%", parte * 100.0 / total);
    }
}
