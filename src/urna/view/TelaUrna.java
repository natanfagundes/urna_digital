package urna.view;

import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Eleitor;
import urna.model.Voto;
import urna.service.ArquivoBoletim;
import urna.service.Urna;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Janela que simula a urna eletrônica: tela do eleitor à esquerda
 * e teclado numérico à direita. O menu "Mesário" faz o papel do
 * terminal do mesário (liberar eleitor, zerésima, encerramento).
 *
 * Atalhos de teclado: 0-9 digitam, Backspace = CORRIGE,
 * Enter = CONFIRMA, B = BRANCO, Ctrl+L = liberar eleitor.
 */
public class TelaUrna extends JFrame {

    private static final Color COR_TELA = new Color(0xF2F2F2);
    private static final Color COR_TECLADO = new Color(0x2B2B2B);
    private static final Color COR_TECLA = new Color(0x1A1A1A);
    private static final Color COR_BRANCO = Color.WHITE;
    private static final Color COR_CORRIGE = new Color(0xE8862A);
    private static final Color COR_CONFIRMA = new Color(0x2E9E48);

    private static final Font FONTE_MENSAGEM = new Font(Font.SANS_SERIF, Font.BOLD, 26);

    private enum Estado {
        /** Aguardando o mesário liberar o próximo eleitor. */
        BLOQUEADA,
        VOTANDO,
        /** Tela "FIM" exibida por alguns segundos. */
        FIM,
        ENCERRADA
    }

    /** Ordem em que os cargos são apresentados ao eleitor. */
    private final List<Cargo> etapas = Arrays.asList(Cargo.VEREADOR, Cargo.PREFEITO);

    private final Urna urna;
    private final ArquivoBoletim arquivo = new ArquivoBoletim();

    private Estado estado = Estado.BLOQUEADA;
    private int etapaAtual;
    private final StringBuilder digitos = new StringBuilder();
    private Voto votoPendente;
    private boolean zeresimaEmitida;

    // Componentes da tela do eleitor
    private final JLabel lblSeuVoto = new JLabel(" ");
    private final JLabel lblCargo = new JLabel(" ");
    private final JPanel painelDigitos = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel lblLinha1 = new JLabel(" ");
    private final JLabel lblLinha2 = new JLabel(" ");
    private final JLabel lblLinha3 = new JLabel(" ");
    private final JLabel lblMensagem = new JLabel(" ");
    private final FotoCandidato fotoTitular = new FotoCandidato("Titular");
    private final FotoCandidato fotoVice = new FotoCandidato("Vice");
    private final JPanel rodape = new JPanel();
    private JLabel[] caixasDigito = new JLabel[0];

    public TelaUrna(Urna urna) {
        super("Urna Eletrônica - Projeto de Extensão");
        this.urna = urna;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sair();
            }
        });
        setLayout(new BorderLayout());
        add(criarTela(), BorderLayout.CENTER);
        add(criarTeclado(), BorderLayout.EAST);
        setJMenuBar(criarMenu());
        registrarAtalhosTeclado();

        setSize(1040, 540);
        setMinimumSize(new Dimension(960, 500));
        setLocationRelativeTo(null);

        mostrarBloqueada();
    }

    // ------------------------------------------------------------------
    // Montagem da interface
    // ------------------------------------------------------------------

    private JPanel criarTela() {
        JPanel tela = new JPanel(new BorderLayout(12, 0));
        tela.setBackground(COR_TELA);
        tela.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(16, 16, 16, 8, COR_TECLADO),
                BorderFactory.createEmptyBorder(16, 20, 12, 20)));

        JPanel conteudo = new JPanel();
        conteudo.setOpaque(false);
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));

        lblSeuVoto.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        lblCargo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        painelDigitos.setOpaque(false);
        for (JLabel l : new JLabel[]{lblLinha1, lblLinha2, lblLinha3}) {
            l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        }
        lblMensagem.setFont(FONTE_MENSAGEM);

        for (JComponent c : new JComponent[]{lblSeuVoto, lblCargo, painelDigitos,
                lblLinha1, lblLinha2, lblLinha3, lblMensagem}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            conteudo.add(c);
            conteudo.add(Box.createVerticalStrut(10));
        }

        JPanel fotos = new JPanel();
        fotos.setOpaque(false);
        fotos.setLayout(new BoxLayout(fotos, BoxLayout.Y_AXIS));
        fotos.add(fotoTitular);
        fotos.add(Box.createVerticalStrut(10));
        fotos.add(fotoVice);
        fotoTitular.ocultar();
        fotoVice.ocultar();

        rodape.setOpaque(false);
        rodape.setLayout(new BoxLayout(rodape, BoxLayout.Y_AXIS));

        tela.add(conteudo, BorderLayout.CENTER);
        tela.add(fotos, BorderLayout.EAST);
        tela.add(rodape, BorderLayout.SOUTH);
        return tela;
    }

    private JPanel criarTeclado() {
        JPanel teclado = new JPanel(new BorderLayout(0, 14));
        teclado.setBackground(COR_TECLADO);
        teclado.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        teclado.setPreferredSize(new Dimension(320, 0));

        JLabel titulo = new JLabel("JUSTIÇA ELEITORAL", SwingConstants.CENTER);
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        teclado.add(titulo, BorderLayout.NORTH);

        JPanel numeros = new JPanel(new GridLayout(4, 3, 10, 10));
        numeros.setOpaque(false);
        for (int i = 1; i <= 9; i++) {
            numeros.add(criarTeclaNumerica(i));
        }
        numeros.add(new JLabel());
        numeros.add(criarTeclaNumerica(0));
        numeros.add(new JLabel());
        teclado.add(numeros, BorderLayout.CENTER);

        JPanel acoes = new JPanel(new GridLayout(1, 3, 8, 0));
        acoes.setOpaque(false);
        acoes.setPreferredSize(new Dimension(0, 56));
        acoes.add(criarTeclaAcao("BRANCO", COR_BRANCO, e -> branco()));
        acoes.add(criarTeclaAcao("CORRIGE", COR_CORRIGE, e -> corrige()));
        acoes.add(criarTeclaAcao("CONFIRMA", COR_CONFIRMA, e -> confirma()));
        teclado.add(acoes, BorderLayout.SOUTH);

        return teclado;
    }

    private JButton criarTeclaNumerica(int numero) {
        JButton b = new JButton(String.valueOf(numero));
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        b.setBackground(COR_TECLA);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.addActionListener(e -> digitar(numero));
        return b;
    }

    private JButton criarTeclaAcao(String texto, Color fundo, ActionListener acao) {
        JButton b = new JButton(texto);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        b.setBackground(fundo);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.addActionListener(acao);
        return b;
    }

    private JMenuBar criarMenu() {
        JMenuBar barra = new JMenuBar();
        JMenu mesario = new JMenu("Mesário");

        JMenuItem liberar = new JMenuItem("Liberar eleitor...");
        liberar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        liberar.addActionListener(e -> liberarEleitor());

        JMenuItem eleitores = new JMenuItem("Eleitores da seção");
        eleitores.addActionListener(e -> mostrarTexto("Eleitores da seção", textoEleitores()));

        JMenuItem candidatos = new JMenuItem("Candidatos cadastrados");
        candidatos.addActionListener(e -> mostrarTexto("Candidatos cadastrados", textoCandidatos()));

        JMenuItem zeresima = new JMenuItem("Emitir Zerésima");
        zeresima.addActionListener(e -> emitirZeresima());

        JMenuItem encerrar = new JMenuItem("Encerrar votação e emitir Boletim de Urna...");
        encerrar.addActionListener(e -> encerrarVotacao());

        JMenuItem sair = new JMenuItem("Sair");
        sair.addActionListener(e -> sair());

        mesario.add(liberar);
        mesario.add(eleitores);
        mesario.add(candidatos);
        mesario.addSeparator();
        mesario.add(zeresima);
        mesario.add(encerrar);
        mesario.addSeparator();
        mesario.add(sair);
        barra.add(mesario);
        return barra;
    }

    private void registrarAtalhosTeclado() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED || !isFocused() || e.isControlDown()) {
                return false;
            }
            int k = e.getKeyCode();
            if (k >= KeyEvent.VK_0 && k <= KeyEvent.VK_9) {
                digitar(k - KeyEvent.VK_0);
            } else if (k >= KeyEvent.VK_NUMPAD0 && k <= KeyEvent.VK_NUMPAD9) {
                digitar(k - KeyEvent.VK_NUMPAD0);
            } else if (k == KeyEvent.VK_BACK_SPACE) {
                corrige();
            } else if (k == KeyEvent.VK_ENTER) {
                confirma();
            } else if (k == KeyEvent.VK_B) {
                branco();
            } else {
                return false;
            }
            return true;
        });
    }

    // ------------------------------------------------------------------
    // Telas
    // ------------------------------------------------------------------

    private void limparTela() {
        lblSeuVoto.setText(" ");
        lblCargo.setText(" ");
        painelDigitos.setVisible(false);
        lblLinha1.setText(" ");
        lblLinha2.setText(" ");
        lblLinha3.setText(" ");
        lblMensagem.setFont(FONTE_MENSAGEM);
        lblMensagem.setText(" ");
        fotoTitular.ocultar();
        fotoVice.ocultar();
        mostrarRodape(false);
    }

    private void mostrarBloqueada() {
        estado = Estado.BLOQUEADA;
        limparTela();
        lblCargo.setText("URNA BLOQUEADA");
        lblLinha1.setText("Aguardando o mesário liberar o próximo eleitor.");
        lblLinha3.setText("Eleitores que já votaram: " + urna.getEleitoresQueVotaram()
                + " de " + urna.getEleitoresAptos());
    }

    private void mostrarEncerrada() {
        estado = Estado.ENCERRADA;
        limparTela();
        lblCargo.setText("VOTAÇÃO ENCERRADA");
        lblLinha1.setText("Comparecimento: " + urna.getEleitoresQueVotaram()
                + " de " + urna.getEleitoresAptos() + " eleitores.");
        lblLinha2.setText("O Boletim de Urna foi emitido.");
    }

    // ------------------------------------------------------------------
    // Fluxo de votação
    // ------------------------------------------------------------------

    private Cargo cargoAtual() {
        return etapas.get(etapaAtual);
    }

    /** Prepara a tela para votar no cargo atual. */
    private void iniciarEtapa() {
        digitos.setLength(0);
        votoPendente = null;
        limparTela();

        Cargo cargo = cargoAtual();
        lblSeuVoto.setText("SEU VOTO PARA");
        lblCargo.setText(cargo.getDescricao());

        painelDigitos.removeAll();
        caixasDigito = new JLabel[cargo.getQuantidadeDigitos()];
        for (int i = 0; i < caixasDigito.length; i++) {
            JLabel caixa = new JLabel(" ", SwingConstants.CENTER);
            caixa.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
            caixa.setPreferredSize(new Dimension(42, 52));
            caixa.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            caixasDigito[i] = caixa;
            painelDigitos.add(caixa);
        }
        painelDigitos.setVisible(true);
        destacarProximaCaixa();

        painelDigitos.revalidate();
        repaint();
    }

    private void digitar(int numero) {
        if (estado != Estado.VOTANDO || votoPendente != null) {
            return;
        }
        Cargo cargo = cargoAtual();
        if (digitos.length() >= cargo.getQuantidadeDigitos()) {
            return;
        }
        digitos.append(numero);
        caixasDigito[digitos.length() - 1].setText(String.valueOf(numero));
        destacarProximaCaixa();

        if (digitos.length() == cargo.getQuantidadeDigitos()) {
            votoPendente = urna.interpretar(cargo, digitos.toString());
            mostrarVotoPendente();
        }
    }

    private void branco() {
        if (estado != Estado.VOTANDO) {
            return;
        }
        if (digitos.length() > 0) {
            lblMensagem.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            lblMensagem.setText("<html>Para votar em BRANCO, o campo de voto<br>"
                    + "deve estar vazio. Aperte CORRIGE.</html>");
            return;
        }
        votoPendente = Voto.branco(cargoAtual());
        painelDigitos.setVisible(false);
        lblMensagem.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        lblMensagem.setText("VOTO EM BRANCO");
        mostrarRodape(true);
    }

    private void corrige() {
        if (estado != Estado.VOTANDO) {
            return;
        }
        iniciarEtapa();
    }

    private void confirma() {
        if (estado != Estado.VOTANDO || votoPendente == null) {
            return;
        }
        urna.registrar(votoPendente);
        Toolkit.getDefaultToolkit().beep();

        etapaAtual++;
        if (etapaAtual < etapas.size()) {
            iniciarEtapa();
        } else {
            urna.finalizarEleitor();
            mostrarFim();
        }
    }

    private void mostrarVotoPendente() {
        switch (votoPendente.getTipo()) {
            case NOMINAL:
                Candidato c = votoPendente.getCandidato();
                lblLinha1.setText("Nome: " + c.getNome());
                lblLinha2.setText("Partido: " + c.getPartido().getSigla()
                        + " - " + c.getPartido().getNome());
                if (c.getNomeVice() != null) {
                    lblLinha3.setText("Vice-" + c.getCargo().getDescricao() + ": " + c.getNomeVice());
                    fotoVice.mostrar(c.getNomeVice(), c.getPartido());
                }
                fotoTitular.mostrar(c.getNome(), c.getPartido());
                break;
            case LEGENDA:
                lblLinha1.setText("NÚMERO ERRADO");
                lblLinha2.setText("Partido: " + votoPendente.getPartido().getSigla()
                        + " - " + votoPendente.getPartido().getNome());
                lblMensagem.setText("VOTO DE LEGENDA");
                break;
            case NULO:
                lblLinha1.setText("NÚMERO ERRADO");
                lblMensagem.setText("VOTO NULO");
                break;
            default:
                break;
        }
        mostrarRodape(true);
    }

    private void mostrarFim() {
        estado = Estado.FIM;
        limparTela();
        lblMensagem.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 90));
        lblMensagem.setText("FIM");

        Timer timer = new Timer(3000, e -> mostrarBloqueada());
        timer.setRepeats(false);
        timer.start();
    }

    private void destacarProximaCaixa() {
        for (int i = 0; i < caixasDigito.length; i++) {
            boolean proxima = i == digitos.length();
            caixasDigito[i].setOpaque(proxima);
            caixasDigito[i].setBackground(proxima ? new Color(0xD9D9D9) : null);
        }
    }

    private void mostrarRodape(boolean visivel) {
        rodape.removeAll();
        if (visivel) {
            rodape.add(new JSeparator());
            JLabel instrucoes = new JLabel("<html>Aperte a tecla:<br>"
                    + "&nbsp;&nbsp;<b>CONFIRMA</b> para CONFIRMAR este voto<br>"
                    + "&nbsp;&nbsp;<b>CORRIGE</b> para REINICIAR este voto</html>");
            instrucoes.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            rodape.add(instrucoes);
        }
        rodape.revalidate();
        rodape.repaint();
    }

    // ------------------------------------------------------------------
    // Funções do mesário
    // ------------------------------------------------------------------

    private void liberarEleitor() {
        if (estado != Estado.BLOQUEADA) {
            String motivo = estado == Estado.ENCERRADA ? "A votação já foi encerrada."
                    : "Aguarde o eleitor atual terminar de votar.";
            JOptionPane.showMessageDialog(this, motivo, "Liberar eleitor", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String titulo = JOptionPane.showInputDialog(this, "Título de eleitor:",
                "Liberar eleitor", JOptionPane.QUESTION_MESSAGE);
        if (titulo == null || titulo.trim().isEmpty()) {
            return;
        }
        try {
            Eleitor eleitor = urna.liberarEleitor(titulo.trim());
            JOptionPane.showMessageDialog(this, "Urna liberada para:\n" + eleitor.getNome(),
                    "Eleitor liberado", JOptionPane.INFORMATION_MESSAGE);
            estado = Estado.VOTANDO;
            etapaAtual = 0;
            iniciarEtapa();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Não foi possível liberar",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void emitirZeresima() {
        if (urna.getEleitoresQueVotaram() > 0 || estado != Estado.BLOQUEADA) {
            JOptionPane.showMessageDialog(this,
                    "A Zerésima só pode ser emitida antes do primeiro eleitor votar.",
                    "Zerésima", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String texto = urna.gerarZeresima();
        zeresimaEmitida = true;
        mostrarTexto("Zerésima", texto + salvarRelatorio("zeresima", texto));
    }

    private void encerrarVotacao() {
        if (estado == Estado.ENCERRADA) {
            mostrarTexto("Boletim de Urna", urna.gerarBoletim());
            return;
        }
        if (estado != Estado.BLOQUEADA) {
            JOptionPane.showMessageDialog(this, "Aguarde o eleitor atual terminar de votar.",
                    "Encerrar votação", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!pedirSenhaMesario("Encerrar votação")) {
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(this,
                "Encerrar a votação? Depois disso nenhum voto poderá ser registrado.",
                "Encerrar votação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        urna.encerrar();
        mostrarEncerrada();
        String boletim = urna.gerarBoletim();
        mostrarTexto("Boletim de Urna", boletim + salvarRelatorio("boletim_urna", boletim));
    }

    private boolean pedirSenhaMesario(String titulo) {
        JPasswordField campo = new JPasswordField();
        int ok = JOptionPane.showConfirmDialog(this, new Object[]{"Senha do mesário:", campo},
                titulo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return false;
        }
        if (!urna.validarSenhaMesario(new String(campo.getPassword()))) {
            JOptionPane.showMessageDialog(this, "Senha incorreta.", titulo, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /** Salva o relatório em arquivo e devolve uma linha informando onde ficou. */
    private String salvarRelatorio(String prefixo, String texto) {
        try {
            Path caminho = arquivo.salvar(prefixo, texto);
            return "\nArquivo salvo em: " + caminho.toAbsolutePath() + "\n";
        } catch (IOException ex) {
            return "\nNão foi possível salvar o arquivo: " + ex.getMessage() + "\n";
        }
    }

    private void sair() {
        if (estado != Estado.ENCERRADA) {
            int resposta = JOptionPane.showConfirmDialog(this,
                    "A votação ainda não foi encerrada e os votos serão perdidos. Sair mesmo assim?",
                    "Sair", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
        }
        System.exit(0);
    }

    private String textoEleitores() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s %-22s %s%n", "TÍTULO", "NOME", "SITUAÇÃO"));
        for (Eleitor e : urna.getCadastro().listarEleitores()) {
            String situacao = urna.jaVotou(e) ? "VOTOU"
                    : e.equals(urna.getEleitorAtual()) ? "VOTANDO" : "-";
            sb.append(String.format("%-8s %-22s %s%n", e.getTitulo(), e.getNome(), situacao));
        }
        sb.append(String.format("%nCompareceram: %d de %d%n",
                urna.getEleitoresQueVotaram(), urna.getEleitoresAptos()));
        if (!zeresimaEmitida && urna.getEleitoresQueVotaram() == 0) {
            sb.append("Lembrete: emita a Zerésima antes do primeiro voto.\n");
        }
        return sb.toString();
    }

    private String textoCandidatos() {
        StringBuilder sb = new StringBuilder();
        sb.append("PARTIDOS\n");
        urna.getCadastro().listarPartidos().forEach(p -> sb.append("  ").append(p).append('\n'));
        for (Cargo cargo : etapas) {
            sb.append('\n').append(cargo.getDescricao().toUpperCase()).append('\n');
            for (Candidato c : urna.getCadastro().listarCandidatos(cargo)) {
                sb.append("  ").append(c);
                if (c.getNomeVice() != null) {
                    sb.append("  | Vice: ").append(c.getNomeVice());
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private void mostrarTexto(String titulo, String texto) {
        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(680, 460));
        JOptionPane.showMessageDialog(this, scroll, titulo, JOptionPane.PLAIN_MESSAGE);
    }
}
