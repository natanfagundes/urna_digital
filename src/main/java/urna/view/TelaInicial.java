package urna.view;

import urna.model.Eleicao;
import urna.persistencia.ArmazenamentoEmArquivo;
import urna.persistencia.RepositorioEleicao;
import urna.service.EstadoVotacao;
import urna.service.Exemplos;
import urna.service.Urna;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Primeira tela: criar uma eleição, abrir uma salva ou usar um exemplo.
 */
public class TelaInicial extends JFrame {

    private final JCheckBox chkDidatico = new JCheckBox("Usar o modo didático (para aulas e oficinas)");

    public TelaInicial() {
        super("Urna Digital");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 18));
        raiz.setBorder(BorderFactory.createEmptyBorder(24, 32, 20, 32));
        raiz.setBackground(new Color(0xF4F6F8));

        JPanel cabecalho = new JPanel();
        cabecalho.setOpaque(false);
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Urna Digital");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        JLabel subtitulo = new JLabel("Eleições para escolas, grêmios, associações e aulas de cidadania");
        subtitulo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        subtitulo.setForeground(new Color(0x555555));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        cabecalho.add(titulo);
        cabecalho.add(Box.createVerticalStrut(4));
        cabecalho.add(subtitulo);
        raiz.add(cabecalho, BorderLayout.NORTH);

        JPanel botoes = new JPanel(new GridLayout(4, 1, 0, 10));
        botoes.setOpaque(false);
        botoes.add(botao("Nova eleição", "Cadastre cargos, chapas, candidatos e eleitores", this::novaEleicao));
        botoes.add(botao("Abrir eleição salva", "Continue uma eleição ou uma votação em andamento", this::abrirEleicao));
        botoes.add(botao("Exemplo: grêmio estudantil", "Presidente do grêmio e conselho de representantes",
                () -> abrirExemplo(Exemplos.gremioEstudantil())));
        botoes.add(botao("Exemplo: eleição municipal", "Prefeito e vereadores, como na eleição real",
                () -> abrirExemplo(Exemplos.municipal())));
        raiz.add(botoes, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        chkDidatico.setOpaque(false);
        chkDidatico.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        rodape.add(chkDidatico, BorderLayout.NORTH);
        JLabel credito = new JLabel("Projeto de Extensão - Engenharia de Software");
        credito.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        credito.setForeground(new Color(0x777777));
        credito.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        rodape.add(credito, BorderLayout.SOUTH);
        raiz.add(rodape, BorderLayout.SOUTH);

        setContentPane(raiz);
        setSize(540, 520);
        setMinimumSize(new Dimension(480, 480));
        setLocationRelativeTo(null);
    }

    private JButton botao(String texto, String descricao, Runnable acao) {
        JButton b = new JButton("<html><b style='font-size:13px'>" + texto + "</b><br>"
                + "<span style='color:#555555'>" + descricao + "</span></html>");
        b.setHorizontalAlignment(JButton.LEFT);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.addActionListener(e -> acao.run());
        return b;
    }

    private boolean didatico() {
        return chkDidatico.isSelected();
    }

    private void novaEleicao() {
        String nome = JOptionPane.showInputDialog(this, "Nome da eleição (ex.: Grêmio Estudantil 2026):",
                "Nova eleição", JOptionPane.QUESTION_MESSAGE);
        if (nome == null || nome.trim().isEmpty()) {
            return;
        }
        Path pasta = RepositorioEleicao.pastaPadrao(nome);
        if (Files.exists(pasta)) {
            JOptionPane.showMessageDialog(this, "Já existe uma eleição com esse nome.\n"
                    + "Use \"Abrir eleição salva\" ou escolha outro nome.", "Nova eleição", JOptionPane.WARNING_MESSAGE);
            return;
        }
        abrirConfiguracao(new Eleicao(nome), pasta);
    }

    private void abrirEleicao() {
        JFileChooser seletor = new JFileChooser(RepositorioEleicao.PASTA_ELEICOES.toAbsolutePath().toFile());
        seletor.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        seletor.setDialogTitle("Escolha a pasta da eleição");
        if (seletor.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path pasta = seletor.getSelectedFile().toPath();
        if (!RepositorioEleicao.existeEleicao(pasta)) {
            JOptionPane.showMessageDialog(this, "Esta pasta não contém uma eleição (falta o arquivo eleicao.properties).",
                    "Abrir eleição", JOptionPane.WARNING_MESSAGE);
            return;
        }
        abrirPasta(pasta);
    }

    /** Exemplos ficam salvos numa pasta própria; se já existir, abre o que está salvo. */
    private void abrirExemplo(Eleicao exemplo) {
        Path pasta = RepositorioEleicao.pastaPadrao(exemplo.getNome());
        if (RepositorioEleicao.existeEleicao(pasta)) {
            abrirPasta(pasta);
        } else {
            abrirConfiguracao(exemplo, pasta);
        }
    }

    /**
     * Abre uma eleição salva. Se houver votação gravada, pergunta se retoma
     * (ou mostra o resultado, se já foi encerrada) ou se zera para editar.
     */
    private void abrirPasta(Path pasta) {
        Eleicao eleicao;
        try {
            eleicao = new RepositorioEleicao().carregar(pasta);
        } catch (IOException ex) {
            erro("Não foi possível abrir a eleição", ex.getMessage());
            return;
        }
        ArmazenamentoEmArquivo armazenamento = new ArmazenamentoEmArquivo(RepositorioEleicao.pastaVotacao(pasta));
        if (!armazenamento.existe()) {
            abrirConfiguracao(eleicao, pasta);
            return;
        }

        Optional<EstadoVotacao> estado;
        try {
            estado = armazenamento.carregar();
        } catch (RuntimeException ex) {
            erro("Problema na votação gravada", ex.getMessage()
                    + "\n\nO arquivo está em " + RepositorioEleicao.pastaVotacao(pasta).toAbsolutePath());
            return;
        }
        boolean encerrada = estado.isPresent() && estado.get().isEncerrada();
        int votaram = estado.map(e -> e.getTitulosQueVotaram().size()).orElse(0);
        String[] opcoes = {encerrada ? "Ver resultado" : "Retomar votação", "Zerar votação e editar", "Cancelar"};
        int escolha = JOptionPane.showOptionDialog(this,
                (encerrada ? "A votação desta eleição já foi ENCERRADA." : "Há uma votação EM ANDAMENTO nesta eleição.")
                        + "\nEleitores que votaram: " + votaram + " de " + eleicao.listarEleitores().size(),
                eleicao.getNome(), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[0]);

        if (escolha == 0) {
            try {
                Urna urna = Urna.retomar(eleicao, armazenamento);
                new TelaUrna(urna, pasta, didatico()).setVisible(true);
                dispose();
            } catch (RuntimeException ex) {
                erro("Não foi possível retomar a votação", ex.getMessage());
            }
        } else if (escolha == 1) {
            if (zerarVotacao(this, eleicao, armazenamento)) {
                abrirConfiguracao(eleicao, pasta);
            }
        }
    }

    /**
     * Pede a senha do mesário e guarda a votação atual em outra pasta (nada é apagado).
     *
     * @return {@code true} se a votação foi zerada
     */
    static boolean zerarVotacao(Component pai, Eleicao eleicao, ArmazenamentoEmArquivo armazenamento) {
        JPasswordField campo = new JPasswordField();
        int ok = JOptionPane.showConfirmDialog(pai, new Object[]{
                        "Os votos atuais serão guardados numa pasta de arquivo e uma votação nova começará.",
                        "Senha do mesário:", campo},
                "Zerar votação", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return false;
        }
        if (!eleicao.validarSenhaMesario(new String(campo.getPassword()))) {
            JOptionPane.showMessageDialog(pai, "Senha incorreta.", "Zerar votação", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            Path arquivada = armazenamento.arquivar();
            JOptionPane.showMessageDialog(pai, "A votação anterior foi guardada em:\n" + arquivada.toAbsolutePath(),
                    "Zerar votação", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(pai, "Não foi possível arquivar a votação: " + ex.getMessage(),
                    "Zerar votação", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void abrirConfiguracao(Eleicao eleicao, Path pasta) {
        new TelaConfiguracao(eleicao, pasta, didatico()).setVisible(true);
        dispose();
    }

    private void erro(String titulo, String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, titulo, JOptionPane.ERROR_MESSAGE);
    }
}
