package urna;

import urna.service.CadastroEleitoral;
import urna.service.Urna;
import urna.view.TelaUrna;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    /** Senha usada pelo mesário para encerrar a votação. */
    private static final String SENHA_MESARIO = "1234";

    public static void main(String[] args) {
        try {
            // Visual "Metal" respeita as cores definidas nos botões em qualquer sistema
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Mantém o visual padrão
        }

        CadastroEleitoral cadastro = CadastroEleitoral.comDadosDeExemplo();
        Urna urna = new Urna(cadastro, SENHA_MESARIO);

        SwingUtilities.invokeLater(() -> new TelaUrna(urna).setVisible(true));
    }
}
