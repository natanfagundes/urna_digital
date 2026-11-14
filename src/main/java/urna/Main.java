package urna;

import urna.view.TelaInicial;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        try {
            // Visual "Metal" respeita as cores definidas nos botões em qualquer sistema
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Mantém o visual padrão
        }
        SwingUtilities.invokeLater(() -> new TelaInicial().setVisible(true));
    }
}
