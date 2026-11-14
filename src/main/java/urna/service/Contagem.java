package urna.service;

import urna.model.Cargo;
import urna.model.Voto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Contadores de votos de uma urna, por cargo. Guarda apenas totais
 * (nunca a sequência dos votos), o que preserva o sigilo do voto.
 *
 * Os cargos são identificados pelo nome; os candidatos e partidos, pelo número.
 */
public class Contagem {

    private final Map<String, Map<Integer, Integer>> nominais = new TreeMap<>();
    private final Map<String, Map<Integer, Integer>> legenda = new TreeMap<>();
    private final Map<String, Integer> brancos = new TreeMap<>();
    private final Map<String, Integer> nulos = new TreeMap<>();

    public void registrar(Voto voto) {
        String cargo = voto.getCargo().getNome();
        switch (voto.getTipo()) {
            case NOMINAL:
                somar(nominais, cargo, voto.getCandidato().getNumero(), 1);
                break;
            case LEGENDA:
                somar(legenda, cargo, voto.getPartido().getNumero(), 1);
                break;
            case BRANCO:
                brancos.merge(cargo, 1, Integer::sum);
                break;
            case NULO:
                nulos.merge(cargo, 1, Integer::sum);
                break;
        }
    }

    public int nominais(Cargo cargo, int numeroCandidato) {
        return nominais.getOrDefault(cargo.getNome(), Collections.emptyMap()).getOrDefault(numeroCandidato, 0);
    }

    public int legenda(Cargo cargo, int numeroPartido) {
        return legenda.getOrDefault(cargo.getNome(), Collections.emptyMap()).getOrDefault(numeroPartido, 0);
    }

    public int brancos(Cargo cargo) {
        return brancos.getOrDefault(cargo.getNome(), 0);
    }

    public int nulos(Cargo cargo) {
        return nulos.getOrDefault(cargo.getNome(), 0);
    }

    /** Soma de todos os votos nominais e de legenda do cargo. */
    public int validos(Cargo cargo) {
        int total = 0;
        for (int v : nominais.getOrDefault(cargo.getNome(), Collections.emptyMap()).values()) total += v;
        for (int v : legenda.getOrDefault(cargo.getNome(), Collections.emptyMap()).values()) total += v;
        return total;
    }

    public Contagem copia() {
        Contagem c = new Contagem();
        nominais.forEach((cargo, m) -> c.nominais.put(cargo, new TreeMap<>(m)));
        legenda.forEach((cargo, m) -> c.legenda.put(cargo, new TreeMap<>(m)));
        c.brancos.putAll(brancos);
        c.nulos.putAll(nulos);
        return c;
    }

    // ------------------------------------------------------------------
    // Acesso direto, usado para salvar e restaurar a contagem
    // ------------------------------------------------------------------

    public Map<String, Map<Integer, Integer>> getNominais() {
        return Collections.unmodifiableMap(nominais);
    }

    public Map<String, Map<Integer, Integer>> getLegenda() {
        return Collections.unmodifiableMap(legenda);
    }

    public Map<String, Integer> getBrancos() {
        return Collections.unmodifiableMap(brancos);
    }

    public Map<String, Integer> getNulos() {
        return Collections.unmodifiableMap(nulos);
    }

    public void definirNominais(String cargo, int numero, int quantidade) {
        somar(nominais, cargo, numero, quantidade);
    }

    public void definirLegenda(String cargo, int numero, int quantidade) {
        somar(legenda, cargo, numero, quantidade);
    }

    public void definirBrancos(String cargo, int quantidade) {
        brancos.put(cargo, quantidade);
    }

    public void definirNulos(String cargo, int quantidade) {
        nulos.put(cargo, quantidade);
    }

    private static void somar(Map<String, Map<Integer, Integer>> mapa, String cargo, int numero, int quantidade) {
        mapa.computeIfAbsent(cargo, k -> new HashMap<>()).merge(numero, quantidade, Integer::sum);
    }
}
