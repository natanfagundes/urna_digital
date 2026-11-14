package urna.service;

import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Partido;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula quem foi eleito a partir dos votos de uma urna.
 *
 * <p><b>Cargos majoritários:</b> vencem os mais votados (tantos quantas forem as vagas).</p>
 *
 * <p><b>Cargos proporcionais</b>, de forma simplificada:</p>
 * <ol>
 *   <li>Quociente eleitoral (QE) = votos válidos / vagas (arredondado).</li>
 *   <li>Cada partido recebe (votos do partido / QE) cadeiras.</li>
 *   <li>As vagas que sobrarem vão, uma a uma, para o partido com a maior média
 *       (votos do partido / (cadeiras já obtidas + 1)).</li>
 *   <li>Dentro de cada partido, as cadeiras vão para os candidatos mais votados.</li>
 * </ol>
 * <p>Só pode ocupar uma cadeira o candidato com votos nominais iguais ou superiores
 * a 10% do quociente eleitoral (e pelo menos 1 voto). Se faltarem candidatos
 * aptos, a vaga fica sem ser preenchida.</p>
 */
public class Apuracao {

    private final Urna urna;

    public Apuracao(Urna urna) {
        this.urna = urna;
    }

    /** Eleitos do cargo, do mais votado para o menos votado. */
    public List<Candidato> eleitos(Cargo cargo) {
        return cargo.isProporcional() ? eleitosProporcional(cargo) : eleitosMajoritario(cargo);
    }

    // ------------------------------------------------------------------
    // Majoritário
    // ------------------------------------------------------------------

    /** Os mais votados (com pelo menos 1 voto), até o número de vagas. */
    public List<Candidato> eleitosMajoritario(Cargo cargo) {
        List<Candidato> eleitos = new ArrayList<>();
        for (Candidato c : urna.ranking(cargo)) {
            if (eleitos.size() == cargo.getVagas() || urna.getVotosCandidato(c) == 0) {
                break;
            }
            eleitos.add(c);
        }
        return eleitos;
    }

    /**
     * Candidatos empatados na disputa pela última vaga (lista vazia se não há empate
     * que mude o resultado). Na lei, o empate é decidido pelo candidato mais idoso.
     */
    public List<Candidato> empateNaUltimaVaga(Cargo cargo) {
        List<Candidato> ranking = urna.ranking(cargo);
        if (ranking.size() <= cargo.getVagas()) {
            return Collections.emptyList();
        }
        int votosUltimaVaga = urna.getVotosCandidato(ranking.get(cargo.getVagas() - 1));
        int votosPrimeiroDeFora = urna.getVotosCandidato(ranking.get(cargo.getVagas()));
        if (votosUltimaVaga == 0 || votosUltimaVaga != votosPrimeiroDeFora) {
            return Collections.emptyList();
        }
        List<Candidato> empatados = new ArrayList<>();
        for (Candidato c : ranking) {
            if (urna.getVotosCandidato(c) == votosUltimaVaga) {
                empatados.add(c);
            }
        }
        return empatados;
    }

    // ------------------------------------------------------------------
    // Proporcional
    // ------------------------------------------------------------------

    /**
     * Quociente eleitoral do cargo: 0 se não houver votos válidos; no mínimo 1 se houver
     * (em eleições pequenas, o arredondamento poderia dar 0 e ninguém seria eleito).
     */
    public int quocienteEleitoral(Cargo cargo) {
        int validos = urna.getVotosValidos(cargo);
        if (validos == 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round((double) validos / cargo.getVagas()));
    }

    /** Votos nominais mínimos para um candidato poder ocupar uma cadeira. */
    public int votacaoMinima(Cargo cargo) {
        return votacaoMinima(quocienteEleitoral(cargo));
    }

    private static int votacaoMinima(int quocienteEleitoral) {
        return Math.max(1, (int) Math.ceil(quocienteEleitoral * 0.10));
    }

    /** Quantas cadeiras cada partido conquistou, na ordem de cadastro dos partidos. */
    public Map<Partido, Integer> distribuirCadeiras(Cargo cargo) {
        Map<Partido, Integer> cadeiras = new LinkedHashMap<>();
        List<Partido> partidos = urna.getEleicao().listarPartidos();
        for (Partido p : partidos) {
            cadeiras.put(p, 0);
        }
        int qe = quocienteEleitoral(cargo);
        if (qe == 0) {
            return cadeiras;
        }

        int distribuidas = 0;
        for (Partido p : partidos) {
            int qp = Math.min(urna.getVotosPartido(cargo, p) / qe, candidatosAptos(cargo, p, qe).size());
            cadeiras.put(p, qp);
            distribuidas += qp;
        }

        // Sobras pelo critério da maior média
        while (distribuidas < cargo.getVagas()) {
            Partido melhor = null;
            double maiorMedia = 0;
            for (Partido p : partidos) {
                int obtidas = cadeiras.get(p);
                if (obtidas >= candidatosAptos(cargo, p, qe).size()) {
                    continue; // partido sem candidatos aptos para ocupar mais vagas
                }
                double media = (double) urna.getVotosPartido(cargo, p) / (obtidas + 1);
                if (media > maiorMedia) {
                    maiorMedia = media;
                    melhor = p;
                }
            }
            if (melhor == null) {
                break;
            }
            cadeiras.put(melhor, cadeiras.get(melhor) + 1);
            distribuidas++;
        }
        return cadeiras;
    }

    /** Candidatos eleitos em um cargo proporcional, do mais votado para o menos votado. */
    public List<Candidato> eleitosProporcional(Cargo cargo) {
        List<Candidato> eleitos = new ArrayList<>();
        int qe = quocienteEleitoral(cargo);
        for (Map.Entry<Partido, Integer> e : distribuirCadeiras(cargo).entrySet()) {
            List<Candidato> aptos = candidatosAptos(cargo, e.getKey(), qe);
            eleitos.addAll(aptos.subList(0, e.getValue()));
        }
        eleitos.sort((a, b) -> Integer.compare(urna.getVotosCandidato(b), urna.getVotosCandidato(a)));
        return Collections.unmodifiableList(eleitos);
    }

    /**
     * Candidatos do partido que atingiram a votação mínima,
     * ordenados por votos (ranking da urna).
     */
    private List<Candidato> candidatosAptos(Cargo cargo, Partido partido, int qe) {
        int minimo = votacaoMinima(qe);
        List<Candidato> lista = new ArrayList<>();
        for (Candidato c : urna.ranking(cargo)) {
            if (c.getPartido().equals(partido) && urna.getVotosCandidato(c) >= minimo) {
                lista.add(c);
            }
        }
        return lista;
    }
}
