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
 * Cargos majoritários (Prefeito): vence o candidato com mais votos.
 *
 * Cargos proporcionais (Vereador), de forma simplificada:
 *  1. Quociente eleitoral (QE) = votos válidos / vagas (arredondado).
 *  2. Cada partido recebe (votos do partido / QE) cadeiras.
 *  3. As vagas que sobrarem vão, uma a uma, para o partido com a maior média
 *     (votos do partido / (cadeiras já obtidas + 1)).
 *  4. Dentro de cada partido, as cadeiras vão para os candidatos mais votados.
 *
 * Só pode ocupar uma cadeira o candidato com votos nominais iguais ou superiores
 * a 10% do quociente eleitoral (e pelo menos 1 voto). Se faltarem candidatos
 * aptos, a vaga fica sem ser preenchida.
 */
public class Apuracao {

    private final Urna urna;

    public Apuracao(Urna urna) {
        this.urna = urna;
    }

    /**
     * Vencedor de um cargo majoritário. Retorna lista vazia se ninguém recebeu votos
     * e mais de um candidato em caso de empate.
     */
    public List<Candidato> vencedores(Cargo cargo) {
        List<Candidato> vencedores = new ArrayList<>();
        int maior = 0;
        for (Candidato c : urna.getCadastro().listarCandidatos(cargo)) {
            int votos = urna.getVotosCandidato(c);
            if (votos > maior) {
                maior = votos;
                vencedores.clear();
                vencedores.add(c);
            } else if (votos == maior && votos > 0) {
                vencedores.add(c);
            }
        }
        return vencedores;
    }

    /** Quociente eleitoral do cargo (0 se não houver votos válidos). */
    public int quocienteEleitoral(Cargo cargo, int vagas) {
        return (int) Math.round((double) urna.getVotosValidos(cargo) / vagas);
    }

    /** Quantas cadeiras cada partido conquistou, na ordem de cadastro dos partidos. */
    public Map<Partido, Integer> distribuirCadeiras(Cargo cargo, int vagas) {
        Map<Partido, Integer> cadeiras = new LinkedHashMap<>();
        List<Partido> partidos = urna.getCadastro().listarPartidos();
        for (Partido p : partidos) {
            cadeiras.put(p, 0);
        }
        int qe = quocienteEleitoral(cargo, vagas);
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
        while (distribuidas < vagas) {
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
    public List<Candidato> eleitosProporcional(Cargo cargo, int vagas) {
        List<Candidato> eleitos = new ArrayList<>();
        int qe = quocienteEleitoral(cargo, vagas);
        for (Map.Entry<Partido, Integer> e : distribuirCadeiras(cargo, vagas).entrySet()) {
            List<Candidato> aptos = candidatosAptos(cargo, e.getKey(), qe);
            eleitos.addAll(aptos.subList(0, e.getValue()));
        }
        eleitos.sort((a, b) -> Integer.compare(urna.getVotosCandidato(b), urna.getVotosCandidato(a)));
        return Collections.unmodifiableList(eleitos);
    }

    /** Votos nominais mínimos para um candidato poder ocupar uma cadeira. */
    public int votacaoMinima(Cargo cargo, int vagas) {
        return votacaoMinima(quocienteEleitoral(cargo, vagas));
    }

    private static int votacaoMinima(int quocienteEleitoral) {
        return Math.max(1, (int) Math.ceil(quocienteEleitoral * 0.10));
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
