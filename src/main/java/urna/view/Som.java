package urna.view;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sons da urna, gerados na hora (sem arquivos de áudio):
 * bipe das teclas, confirmação e a sequência de "FIM".
 */
final class Som {

    private static final float TAXA = 22050f;

    private final ExecutorService fila = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "som-urna");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean ativo = true;
    private volatile boolean disponivel = true;

    boolean isAtivo() {
        return ativo;
    }

    void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    void tecla() {
        tocar(new int[][]{{1050, 70}});
    }

    void confirma() {
        tocar(new int[][]{{1050, 90}, {0, 30}, {1300, 140}});
    }

    /** Sequência curta e longa, parecida com a da urna real ao terminar. */
    void fim() {
        tocar(new int[][]{{1200, 90}, {0, 40}, {1200, 90}, {0, 40}, {1200, 90}, {0, 40}, {1200, 90}, {0, 40}, {1200, 500}});
    }

    void erro() {
        tocar(new int[][]{{400, 250}});
    }

    /** Cada nota é {frequência em Hz (0 = silêncio), duração em ms}. */
    private void tocar(int[][] notas) {
        if (!ativo || !disponivel) {
            return;
        }
        fila.submit(() -> {
            try {
                AudioFormat formato = new AudioFormat(TAXA, 16, 1, true, false);
                try (SourceDataLine linha = AudioSystem.getSourceDataLine(formato)) {
                    linha.open(formato);
                    linha.start();
                    for (int[] nota : notas) {
                        byte[] dados = gerar(nota[0], nota[1]);
                        linha.write(dados, 0, dados.length);
                    }
                    linha.drain();
                }
            } catch (Exception | LinkageError e) {
                disponivel = false; // computador sem saída de áudio: segue sem som
            }
        });
    }

    private static byte[] gerar(int frequencia, int duracaoMs) {
        int amostras = (int) (TAXA * duracaoMs / 1000);
        byte[] dados = new byte[amostras * 2];
        int rampa = Math.min(amostras / 4, (int) (TAXA * 0.005)); // evita estalos
        for (int i = 0; i < amostras; i++) {
            double volume = 0.35;
            if (i < rampa) volume *= (double) i / rampa;
            if (i > amostras - rampa) volume *= (double) (amostras - i) / rampa;
            double valor = frequencia == 0 ? 0 : Math.sin(2 * Math.PI * frequencia * i / TAXA) * volume;
            short s = (short) (valor * Short.MAX_VALUE);
            dados[2 * i] = (byte) s;
            dados[2 * i + 1] = (byte) (s >> 8);
        }
        return dados;
    }
}
