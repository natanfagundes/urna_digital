package urna.view;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
 * Leitura em voz alta, para eleitores com deficiência visual.
 * Usa o sintetizador de voz do próprio sistema operacional:
 * <ul>
 *   <li>Windows: System.Speech (voz em português, se instalada);</li>
 *   <li>macOS: comando {@code say};</li>
 *   <li>Linux: {@code espeak-ng} ou {@code espeak}.</li>
 * </ul>
 * Se nada disso estiver disponível, a leitura fica desativada sem atrapalhar a urna.
 */
final class Voz {

    /**
     * Script do Windows: lê frases da entrada padrão e fala cada uma,
     * interrompendo a anterior (como um leitor de tela).
     */
    private static final String SCRIPT_WINDOWS =
            "Add-Type -AssemblyName System.Speech;"
                    + "[Console]::InputEncoding = [System.Text.Encoding]::UTF8;"
                    + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer;"
                    + "$s.SetOutputToDefaultAudioDevice();"
                    + "$v = $s.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -eq 'pt-BR' } | Select-Object -First 1;"
                    + "if ($v) { $s.SelectVoice($v.VoiceInfo.Name) };"
                    + "while (($l = [Console]::In.ReadLine()) -ne $null) {"
                    + "  $s.SpeakAsyncCancelAll();"
                    + "  if ($l.Length -gt 0) { [void]$s.SpeakAsync($l) }"
                    + "}";

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private static final boolean MAC = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");

    private boolean ativa;
    private Process processoWindows;
    private BufferedWriter entradaWindows;
    private Process falaAtual;

    Voz() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::encerrar));
    }

    boolean isAtiva() {
        return ativa;
    }

    void setAtiva(boolean ativa) {
        this.ativa = ativa;
        if (!ativa) {
            calar();
        }
    }

    /** Fala o texto (interrompendo a fala anterior). */
    synchronized void falar(String texto) {
        if (!ativa || texto == null) {
            return;
        }
        String frase = texto.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        try {
            if (WINDOWS) {
                falarWindows(frase);
            } else {
                falarComando(frase);
            }
        } catch (IOException e) {
            ativa = false;
        }
    }

    private void falarWindows(String frase) throws IOException {
        if (processoWindows == null || !processoWindows.isAlive()) {
            String codificado = Base64.getEncoder().encodeToString(SCRIPT_WINDOWS.getBytes(StandardCharsets.UTF_16LE));
            processoWindows = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive",
                    "-EncodedCommand", codificado)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            entradaWindows = new BufferedWriter(new OutputStreamWriter(processoWindows.getOutputStream(), StandardCharsets.UTF_8));
        }
        entradaWindows.write(frase);
        entradaWindows.newLine();
        entradaWindows.flush();
    }

    /** macOS / Linux: um processo por frase (o texto vai como argumento, sem passar por shell). */
    private void falarComando(String frase) throws IOException {
        calar();
        if (frase.isEmpty()) {
            return;
        }
        if (MAC) {
            falaAtual = new ProcessBuilder("say", frase).start();
            return;
        }
        try {
            falaAtual = new ProcessBuilder("espeak-ng", "-v", "pt-br", frase).start();
        } catch (IOException semEspeakNg) {
            falaAtual = new ProcessBuilder("espeak", "-v", "pt", frase).start();
        }
    }

    synchronized void calar() {
        if (falaAtual != null) {
            falaAtual.destroy();
            falaAtual = null;
        }
        if (entradaWindows != null) {
            try {
                entradaWindows.newLine(); // linha vazia = interrompe a fala atual
                entradaWindows.flush();
            } catch (IOException ignorada) {
                // processo já terminou
            }
        }
    }

    synchronized void encerrar() {
        calar();
        if (processoWindows != null) {
            processoWindows.destroy();
            processoWindows = null;
            entradaWindows = null;
        }
    }
}
