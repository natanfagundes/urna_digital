package urna.util;

import java.text.Normalizer;
import java.util.Locale;

/** Funções auxiliares para textos. */
public final class Textos {

    private Textos() {
    }

    public static boolean vazio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    /** "Grêmio Estudantil 2026" -> "gremio-estudantil-2026" (seguro para nome de pasta). */
    public static String slug(String texto) {
        String semAcento = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = semAcento.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? "eleicao" : slug;
    }

    /** "1 voto", "2 votos". */
    public static String votos(int quantidade) {
        return quantidade + (quantidade == 1 ? " voto" : " votos");
    }
}
