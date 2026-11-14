package urna.view;

import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Eleicao;
import urna.model.Eleitor;
import urna.model.Partido;
import urna.model.Voto;
import urna.persistencia.ArquivoRelatorio;
import urna.persistencia.RepositorioEleicao;
import urna.service.Relatorios;
import urna.service.Urna;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Janela que simula a urna eletrônica: tela do eleitor à esquerda
 * e teclado numérico à direita. O menu "Mesário" faz o papel do
 * terminal do mesário (liberar eleitor, zerésima, encerramento).
 *
 * <p>Teclado: 0-9 digitam, Backspace = CORRIGE, Enter = CONFIRMA, B = BRANCO,
 * Ctrl+L = liberar eleitor, F2 = alto contraste, F3 = fonte grande,
 * F4 = leitura em voz alta, F5 = modo didático.</p>
 */
public class TelaUrna extends JFrame {

    private static final Color COR_TECLADO = new Color(0x2B2B2B);
    private static final Color COR_TECLA = new Color(0x1A1A1A);
    private static final Color COR_BRANCO = Color.WHITE;
    private static final Color COR_CORRIGE = new Color(0xE8862A);
    private static final Color COR_CONFIRMA = new Color(0x2E9E48);

    private static final String PROP_FONTE = "urna.fonteBase";

    private enum Estado {
        /** Aguardando o mesário liberar o próximo eleitor. */
        BLOQUEADA,
        VOTANDO,
        /** Tela "FIM" exibida por alguns segundos. */
        FIM,
        ENCERRADA
    }

    private final Urna urna;
    private final Eleicao eleicao;
    private final List<Cargo> etapas;
    private final Path pastaEleicao;
    private final ArquivoRelatorio arquivoRelatorio;
    private final Som som = new Som();
    private final Voz voz = new Voz();

    private Estado estado = Estado.BLOQUEADA;
    private int etapaAtual;
    private final StringBuilder digitos = new StringBuilder();
    private Voto votoPendente;
    private boolean zeresimaEmitida;

    // Acessibilidade e modo didático
    private boolean altoContraste;
    private double escala = 1.0;
    private boolean didatico;

    // Componentes da tela do eleitor
    private final JPanel tela = new JPanel(new BorderLayout(12, 0));
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
    private final JPanel painelDidatico = new JPanel(new BorderLayout());
    private final JTextArea txtDidatico = new JTextArea();
    private JLabel[] caixasDigito = new JLabel[0];
    private final KeyEventDispatcher atalhos = this::tratarTecla;

    public TelaUrna(Urna urna, Path pastaEleicao, boolean didatico) {
        super("Urna Digital - " + urna.getEleicao().getNome());
        this.urna = urna;
        this.eleicao = urna.getEleicao();
        this.etapas = eleicao.getCargos();
        this.pastaEleicao = pastaEleicao;
        this.arquivoRelatorio = new ArquivoRelatorio(pastaEleicao == null ? Paths.get("relatorios")
                : RepositorioEleicao.pastaRelatorios(pastaEleicao));
        this.didatico = didatico;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sair(false);
            }
        });
        setLayout(new BorderLayout());
        add(criarTela(), BorderLayout.CENTER);
        add(criarTeclado(), BorderLayout.EAST);
        add(criarPainelDidatico(), BorderLayout.SOUTH);
        setJMenuBar(criarMenu());
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(atalhos);

        setSize(1080, 600);
        setMinimumSize(new Dimension(980, 540));
        setLocationRelativeTo(null);

        aplicarTema();
        if (urna.isEncerrada()) {
            mostrarEncerrada();
        } else {
            mostrarBloqueada();
        }
    }

    @Override
    public void dispose() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(atalhos);
        voz.encerrar();
        super.dispose();
    }

    // ------------------------------------------------------------------
    // Montagem da interface
    // ------------------------------------------------------------------

    private JPanel criarTela() {
        tela.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(16, 16, 16, 8, COR_TECLADO),
                BorderFactory.createEmptyBorder(16, 20, 12, 20)));

        JPanel conteudo = new JPanel();
        conteudo.setOpaque(false);
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));

        fonte(lblSeuVoto, Font.BOLD, 16);
        fonte(lblCargo, Font.BOLD, 30);
        painelDigitos.setOpaque(false);
        for (JLabel l : new JLabel[]{lblLinha1, lblLinha2, lblLinha3}) {
            fonte(l, Font.PLAIN, 20);
        }
        fonte(lblMensagem, Font.BOLD, 26);

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

        JLabel titulo = new JLabel("URNA DIGITAL", SwingConstants.CENTER);
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
        b.getAccessibleContext().setAccessibleName("Tecla " + numero);
        b.addActionListener(e -> digitar(numero));
        return b;
    }

    private JButton criarTeclaAcao(String texto, Color fundo, ActionListener acao) {
        JButton b = new JButton(texto);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        b.setMargin(new Insets(2, 2, 2, 2));
        b.setBackground(fundo);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.addActionListener(acao);
        return b;
    }

    private JPanel criarPainelDidatico() {
        JLabel titulo = new JLabel("  MODO DIDÁTICO - o que está acontecendo?");
        fonte(titulo, Font.BOLD, 13);
        txtDidatico.setEditable(false);
        txtDidatico.setFocusable(false);
        txtDidatico.setLineWrap(true);
        txtDidatico.setWrapStyleWord(true);
        txtDidatico.setRows(3);
        txtDidatico.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));
        fonte(txtDidatico, Font.PLAIN, 14);
        painelDidatico.add(titulo, BorderLayout.NORTH);
        painelDidatico.add(txtDidatico, BorderLayout.CENTER);
        painelDidatico.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, COR_TECLADO));
        painelDidatico.setVisible(didatico);
        return painelDidatico;
    }

    private JMenuBar criarMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu mesario = new JMenu("Mesário");
        mesario.add(item("Liberar eleitor...", KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK),
                this::liberarEleitor));
        mesario.add(item("Cancelar eleitor atual (desistência)", null, this::cancelarEleitor));
        mesario.addSeparator();
        mesario.add(item("Eleitores", null, () -> mostrarTexto("Eleitores", textoEleitores())));
        mesario.add(item("Candidatos", null, () -> mostrarTexto("Candidatos", textoCandidatos())));
        mesario.addSeparator();
        mesario.add(item("Emitir Zerésima", null, this::emitirZeresima));
        mesario.add(item("Encerrar votação e emitir Boletim de Urna...", null, this::encerrarVotacao));
        mesario.add(item("Ver Boletim de Urna", null, this::verBoletim));
        mesario.addSeparator();
        mesario.add(item("Voltar à tela inicial", null, () -> sair(true)));
        mesario.add(item("Sair", null, () -> sair(false)));

        JMenu acessibilidade = new JMenu("Acessibilidade");
        acessibilidade.add(opcao("Alto contraste", KeyEvent.VK_F2, altoContraste, ativo -> {
            altoContraste = ativo;
            aplicarTema();
        }));
        acessibilidade.add(opcao("Fonte grande", KeyEvent.VK_F3, false, ativo -> {
            escala = ativo ? 1.4 : 1.0;
            aplicarTema();
        }));
        acessibilidade.add(opcao("Leitura em voz alta", KeyEvent.VK_F4, false, ativo -> {
            voz.setAtiva(ativo);
            voz.falar(ativo ? "Leitura em voz alta ativada." : "");
        }));
        acessibilidade.add(opcao("Sons das teclas", 0, true, som::setAtivo));

        JMenu ajuda = new JMenu("Ajuda");
        ajuda.add(opcao("Modo didático", KeyEvent.VK_F5, didatico, ativo -> {
            didatico = ativo;
            painelDidatico.setVisible(ativo);
            revalidate();
        }));
        ajuda.add(item("Como votar", null, () -> mostrarTexto("Como votar", textoComoVotar())));

        barra.add(mesario);
        barra.add(acessibilidade);
        barra.add(ajuda);
        return barra;
    }

    private static JMenuItem item(String texto, KeyStroke atalho, Runnable acao) {
        JMenuItem item = new JMenuItem(texto);
        if (atalho != null) {
            item.setAccelerator(atalho);
        }
        item.addActionListener(e -> acao.run());
        return item;
    }

    private static JCheckBoxMenuItem opcao(String texto, int teclaF, boolean inicial,
                                           java.util.function.Consumer<Boolean> aoMudar) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(texto, inicial);
        if (teclaF != 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(teclaF, 0));
        }
        item.addActionListener(e -> aoMudar.accept(item.isSelected()));
        return item;
    }

    private boolean tratarTecla(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED || !isFocused() || e.isControlDown() || e.isAltDown()) {
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
    }

    // ------------------------------------------------------------------
    // Tema (alto contraste e tamanho da fonte)
    // ------------------------------------------------------------------

    private Color corFundo() {
        return altoContraste ? Color.BLACK : new Color(0xF2F2F2);
    }

    private Color corTexto() {
        return altoContraste ? Color.YELLOW : Color.BLACK;
    }

    private Color corBorda() {
        return altoContraste ? Color.WHITE : Color.BLACK;
    }

    private Color corDestaque() {
        return altoContraste ? new Color(0x505050) : new Color(0xD9D9D9);
    }

    /** Define a fonte guardando o tamanho base, para poder aumentar depois. */
    private void fonte(JComponent c, int estilo, int tamanhoBase) {
        fonte(c, Font.SANS_SERIF, estilo, tamanhoBase);
    }

    private void fonte(JComponent c, String familia, int estilo, int tamanhoBase) {
        c.putClientProperty(PROP_FONTE, new Object[]{familia, estilo, tamanhoBase});
        c.setFont(new Font(familia, estilo, (int) Math.round(tamanhoBase * escala)));
    }

    private void reaplicarFontes(Container raiz) {
        for (Component c : raiz.getComponents()) {
            if (c instanceof JComponent) {
                Object[] base = (Object[]) ((JComponent) c).getClientProperty(PROP_FONTE);
                if (base != null) {
                    fonte((JComponent) c, (String) base[0], (Integer) base[1], (Integer) base[2]);
                }
            }
            if (c instanceof Container) {
                reaplicarFontes((Container) c);
            }
        }
    }

    private void pintarTextos(Container raiz, Color cor) {
        for (Component c : raiz.getComponents()) {
            if (c instanceof JLabel || c instanceof JTextArea) {
                c.setForeground(cor);
            }
            if (c instanceof Container) {
                pintarTextos((Container) c, cor);
            }
        }
    }

    private void aplicarTema() {
        tela.setBackground(corFundo());
        pintarTextos(tela, corTexto());
        painelDidatico.setBackground(altoContraste ? Color.BLACK : new Color(0xFFF6D5));
        txtDidatico.setBackground(painelDidatico.getBackground());
        pintarTextos(painelDidatico, altoContraste ? Color.WHITE : Color.BLACK);
        reaplicarFontes(getContentPane());
        if (estado == Estado.VOTANDO && painelDigitos.isVisible()) {
            criarCaixasDigito();
        }
        if (escala > 1) {
            aumentarJanela();
        }
        revalidate();
        repaint();
    }

    /** Com a fonte grande, a janela cresce (até o tamanho da tela) para o conteúdo caber. */
    private void aumentarJanela() {
        Dimension monitor = Toolkit.getDefaultToolkit().getScreenSize();
        int largura = Math.min(monitor.width, (int) (1080 * escala * 0.9));
        int altura = Math.min(monitor.height - 40, (int) (600 * escala));
        if (getWidth() < largura || getHeight() < altura) {
            setSize(Math.max(getWidth(), largura), Math.max(getHeight(), altura));
            setLocationRelativeTo(null);
        }
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
        fonte(lblMensagem, Font.BOLD, 26);
        lblMensagem.setText(" ");
        fotoTitular.ocultar();
        fotoVice.ocultar();
        mostrarRodape(false);
    }

    private void explicar(String texto) {
        txtDidatico.setText(texto);
        txtDidatico.setCaretPosition(0);
    }

    private void mostrarBloqueada() {
        estado = Estado.BLOQUEADA;
        limparTela();
        lblCargo.setText("URNA BLOQUEADA");
        lblLinha1.setText("Aguardando o mesário liberar o próximo eleitor.");
        lblLinha3.setText("Eleitores que já votaram: " + urna.getEleitoresQueVotaram()
                + " de " + urna.getEleitoresAptos());
        explicar(Didatica.bloqueada());
    }

    private void mostrarEncerrada() {
        estado = Estado.ENCERRADA;
        limparTela();
        lblCargo.setText("VOTAÇÃO ENCERRADA");
        lblLinha1.setText("Comparecimento: " + urna.getEleitoresQueVotaram()
                + " de " + urna.getEleitoresAptos() + " eleitores.");
        lblLinha2.setText("Boletim de Urna: menu Mesário → Ver Boletim de Urna.");
        explicar(Didatica.encerrada());
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
        lblCargo.setText(cargo.getNome());
        criarCaixasDigito();
        painelDigitos.setVisible(true);
        explicar(Didatica.etapa(cargo));
        voz.falar("Seu voto para " + cargo.getNome() + ". Digite " + cargo.getQuantidadeDigitos() + " números.");
    }

    /** (Re)cria as caixas dos dígitos do cargo atual, mantendo o que já foi digitado. */
    private void criarCaixasDigito() {
        painelDigitos.removeAll();
        caixasDigito = new JLabel[cargoAtual().getQuantidadeDigitos()];
        for (int i = 0; i < caixasDigito.length; i++) {
            JLabel caixa = new JLabel(i < digitos.length() ? String.valueOf(digitos.charAt(i)) : " ",
                    SwingConstants.CENTER);
            fonte(caixa, Font.MONOSPACED, Font.BOLD, 30);
            caixa.setForeground(corTexto());
            caixa.setPreferredSize(new Dimension((int) (42 * escala), (int) (52 * escala)));
            caixa.setBorder(BorderFactory.createLineBorder(corBorda(), 2));
            caixasDigito[i] = caixa;
            painelDigitos.add(caixa);
        }
        destacarProximaCaixa();
        // Não deixa o layout esticar o painel dos dígitos na vertical
        painelDigitos.setMaximumSize(new Dimension(Integer.MAX_VALUE, painelDigitos.getPreferredSize().height));
        painelDigitos.revalidate();
        painelDigitos.repaint();
    }

    private void digitar(int numero) {
        if (estado != Estado.VOTANDO || votoPendente != null) {
            return;
        }
        Cargo cargo = cargoAtual();
        if (digitos.length() >= cargo.getQuantidadeDigitos()) {
            return;
        }
        som.tecla();
        digitos.append(numero);
        caixasDigito[digitos.length() - 1].setText(String.valueOf(numero));
        destacarProximaCaixa();

        if (digitos.length() == cargo.getQuantidadeDigitos()) {
            votoPendente = urna.interpretar(cargo, digitos.toString());
            mostrarVotoPendente();
        } else {
            voz.falar(String.valueOf(numero));
            if (cargo.aceitaVotoLegenda() && digitos.length() == Partido.DIGITOS) {
                Optional<Partido> partido = urna.buscarPartidoPeloInicio(digitos.toString());
                partido.ifPresent(p -> {
                    lblLinha2.setText("Partido: " + p.getSigla() + " - " + p.getNome());
                    explicar(Didatica.partidoDigitado(p));
                });
            }
        }
    }

    private void branco() {
        if (estado != Estado.VOTANDO || votoPendente != null) {
            return;
        }
        if (digitos.length() > 0) {
            som.erro();
            fonte(lblMensagem, Font.BOLD, 18);
            lblMensagem.setText("<html>Para votar em BRANCO, o campo de voto<br>"
                    + "deve estar vazio. Aperte CORRIGE.</html>");
            explicar(Didatica.brancoComDigitos());
            voz.falar("Para votar em branco, o campo de voto deve estar vazio. Aperte corrige.");
            return;
        }
        som.tecla();
        votoPendente = Voto.branco(cargoAtual());
        painelDigitos.setVisible(false);
        fonte(lblMensagem, Font.BOLD, 32);
        lblMensagem.setText("VOTO EM BRANCO");
        mostrarRodape(true);
        explicar(Didatica.branco());
        voz.falar("Voto em branco. " + instrucaoConfirmar());
    }

    private void corrige() {
        if (estado != Estado.VOTANDO) {
            return;
        }
        som.tecla();
        iniciarEtapa();
    }

    private void confirma() {
        if (estado != Estado.VOTANDO || votoPendente == null) {
            return;
        }
        urna.registrar(votoPendente);
        etapaAtual++;
        if (etapaAtual < etapas.size()) {
            som.confirma();
            iniciarEtapa();
            if (didatico) {
                explicar(Didatica.confirmado() + "\n\n" + Didatica.etapa(cargoAtual()));
            }
            return;
        }
        try {
            urna.finalizarEleitor();
        } catch (UncheckedIOException ex) {
            urna.cancelarEleitor();
            som.erro();
            JOptionPane.showMessageDialog(this, "Não foi possível gravar o voto:\n" + ex.getMessage()
                            + "\n\nO voto deste eleitor NÃO foi contado. Verifique o disco e libere o eleitor novamente.",
                    "Erro ao gravar", JOptionPane.ERROR_MESSAGE);
            mostrarBloqueada();
            return;
        }
        som.fim();
        mostrarFim();
    }

    private String instrucaoConfirmar() {
        return "Aperte confirma para confirmar, ou corrige para reiniciar este voto.";
    }

    private void mostrarVotoPendente() {
        Cargo cargo = cargoAtual();
        switch (votoPendente.getTipo()) {
            case NOMINAL:
                Candidato c = votoPendente.getCandidato();
                lblLinha1.setText("Nome: " + c.getNome());
                lblLinha2.setText("Partido: " + c.getPartido().getSigla() + " - " + c.getPartido().getNome());
                String falaVice = "";
                if (c.getNomeVice() != null) {
                    lblLinha3.setText(cargo.getRotuloVice() + ": " + c.getNomeVice());
                    fotoVice.mostrar(c.getNomeVice(), c.getPartido(), null);
                    falaVice = cargo.getRotuloVice() + ": " + c.getNomeVice() + ". ";
                }
                fotoTitular.mostrar(c.getNome(), c.getPartido(), c.getFoto());
                explicar(Didatica.nominal());
                voz.falar(c.getNome() + ". Partido: " + c.getPartido().getNome() + ". " + falaVice + instrucaoConfirmar());
                break;
            case LEGENDA:
                lblLinha1.setText("NÚMERO ERRADO");
                lblLinha2.setText("Partido: " + votoPendente.getPartido().getSigla()
                        + " - " + votoPendente.getPartido().getNome());
                lblMensagem.setText("VOTO DE LEGENDA");
                explicar(Didatica.legenda());
                voz.falar("Número errado. Voto de legenda no partido " + votoPendente.getPartido().getNome()
                        + ". " + instrucaoConfirmar());
                break;
            case NULO:
                lblLinha1.setText("NÚMERO ERRADO");
                lblMensagem.setText("VOTO NULO");
                explicar(Didatica.nulo());
                voz.falar("Número errado. Voto nulo. " + instrucaoConfirmar());
                break;
            default:
                break;
        }
        mostrarRodape(true);
    }

    private void mostrarFim() {
        estado = Estado.FIM;
        limparTela();
        fonte(lblMensagem, Font.BOLD, 90);
        lblMensagem.setText("FIM");
        explicar(Didatica.fim());
        voz.falar("Fim. Obrigado por votar.");

        Timer timer = new Timer(didatico ? 6000 : 3000, e -> mostrarBloqueada());
        timer.setRepeats(false);
        timer.start();
    }

    private void destacarProximaCaixa() {
        for (int i = 0; i < caixasDigito.length; i++) {
            boolean proxima = i == digitos.length();
            caixasDigito[i].setOpaque(proxima);
            caixasDigito[i].setBackground(proxima ? corDestaque() : null);
        }
    }

    private void mostrarRodape(boolean visivel) {
        rodape.removeAll();
        if (visivel) {
            JSeparator separador = new JSeparator();
            separador.setForeground(corBorda());
            rodape.add(separador);
            JLabel instrucoes = new JLabel("<html>Aperte a tecla:<br>"
                    + "&nbsp;&nbsp;<b>CONFIRMA</b> para CONFIRMAR este voto<br>"
                    + "&nbsp;&nbsp;<b>CORRIGE</b> para REINICIAR este voto</html>");
            fonte(instrucoes, Font.PLAIN, 14);
            instrucoes.setForeground(corTexto());
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
        String titulo = JOptionPane.showInputDialog(this, "Título de eleitor / matrícula:",
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

    private void cancelarEleitor() {
        if (estado != Estado.VOTANDO) {
            JOptionPane.showMessageDialog(this, "Não há eleitor votando agora.",
                    "Cancelar eleitor", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(this,
                "O eleitor " + urna.getEleitorAtual().getNome() + " desistiu de votar?\n"
                        + "Os votos que ele já confirmou serão descartados e ele poderá votar depois.",
                "Cancelar eleitor", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resposta == JOptionPane.YES_OPTION) {
            urna.cancelarEleitor();
            voz.calar();
            mostrarBloqueada();
        }
    }

    private void emitirZeresima() {
        if (urna.getEleitoresQueVotaram() > 0 || estado != Estado.BLOQUEADA) {
            JOptionPane.showMessageDialog(this,
                    "A Zerésima só pode ser emitida antes do primeiro eleitor votar.",
                    "Zerésima", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String texto = Relatorios.zeresima(urna);
        zeresimaEmitida = true;
        mostrarTexto("Zerésima", texto + salvarRelatorio("zeresima", texto));
    }

    private void encerrarVotacao() {
        if (estado == Estado.ENCERRADA) {
            verBoletim();
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
        try {
            urna.encerrar();
        } catch (UncheckedIOException ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível gravar o encerramento:\n" + ex.getMessage(),
                    "Erro ao gravar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        mostrarEncerrada();
        String boletim = Relatorios.boletim(urna);
        mostrarTexto("Boletim de Urna", boletim + salvarRelatorio("boletim_urna", boletim));
    }

    private void verBoletim() {
        if (!urna.isEncerrada()) {
            JOptionPane.showMessageDialog(this, "O Boletim de Urna só existe depois do encerramento da votação.",
                    "Boletim de Urna", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        mostrarTexto("Boletim de Urna", Relatorios.boletim(urna));
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
            Path caminho = arquivoRelatorio.salvar(prefixo, texto);
            return "\nArquivo salvo em: " + caminho.toAbsolutePath() + "\n";
        } catch (IOException ex) {
            return "\nNão foi possível salvar o arquivo: " + ex.getMessage() + "\n";
        }
    }

    /**
     * Fecha a urna. A votação já está gravada em disco, então pode ser retomada
     * depois abrindo a mesma eleição.
     */
    private void sair(boolean voltarAoInicio) {
        if (estado == Estado.VOTANDO) {
            JOptionPane.showMessageDialog(this, "Há um eleitor votando. Aguarde ele terminar "
                    + "ou use Mesário → Cancelar eleitor atual.", "Sair", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (estado != Estado.ENCERRADA) {
            String onde = pastaEleicao == null ? "" : "\n(" + pastaEleicao.toAbsolutePath() + ")";
            int resposta = JOptionPane.showConfirmDialog(this,
                    "A votação NÃO foi encerrada, mas está gravada e pode ser retomada\n"
                            + "abrindo esta eleição novamente." + onde + "\n\nFechar a urna?",
                    "Sair", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dispose();
        if (voltarAoInicio) {
            new TelaInicial().setVisible(true);
        } else {
            System.exit(0);
        }
    }

    // ------------------------------------------------------------------
    // Textos
    // ------------------------------------------------------------------

    private String textoEleitores() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s %-26s %s%n", "TÍTULO", "NOME", "SITUAÇÃO"));
        for (Eleitor e : eleicao.listarEleitores()) {
            String situacao = urna.jaVotou(e) ? "VOTOU"
                    : e.equals(urna.getEleitorAtual()) ? "VOTANDO" : "-";
            sb.append(String.format("%-10s %-26s %s%n", e.getTitulo(), e.getNome(), situacao));
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
        sb.append("PARTIDOS / CHAPAS\n");
        eleicao.listarPartidos().forEach(p -> sb.append("  ").append(p).append('\n'));
        for (Cargo cargo : etapas) {
            sb.append('\n').append(cargo.getNome().toUpperCase())
                    .append(" (").append(cargo.getQuantidadeDigitos()).append(" dígitos)\n");
            for (Candidato c : eleicao.listarCandidatos(cargo)) {
                sb.append("  ").append(c);
                if (c.getNomeVice() != null) {
                    sb.append("  | ").append(cargo.getRotuloVice()).append(": ").append(c.getNomeVice());
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String textoComoVotar() {
        StringBuilder sb = new StringBuilder();
        sb.append("COMO VOTAR\n\n");
        sb.append("1. O mesário libera a urna com o seu título ou matrícula.\n");
        sb.append("2. Você vota nos cargos, nesta ordem:\n");
        for (int i = 0; i < etapas.size(); i++) {
            Cargo c = etapas.get(i);
            sb.append("     ").append(i + 1).append(") ").append(c.getNome())
                    .append(" - ").append(c.getQuantidadeDigitos()).append(" números\n");
        }
        sb.append("3. Digite o número do candidato e confira nome, partido e foto.\n");
        sb.append("4. Aperte CONFIRMA (verde) para confirmar, ou CORRIGE (laranja) para apagar.\n");
        sb.append("5. Para votar em BRANCO, aperte a tecla BRANCO com o campo vazio.\n");
        sb.append("6. Quando aparecer FIM, seu voto foi registrado.\n\n");
        sb.append("TECLADO DO COMPUTADOR\n");
        sb.append("  Números = teclas numéricas   Enter = CONFIRMA\n");
        sb.append("  Backspace = CORRIGE           B = BRANCO\n\n");
        sb.append("ACESSIBILIDADE\n");
        sb.append("  F2 = alto contraste   F3 = fonte grande\n");
        sb.append("  F4 = leitura em voz alta   F5 = modo didático\n");
        return sb.toString();
    }

    private void mostrarTexto(String titulo, String texto) {
        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, (int) Math.round(13 * escala)));
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(760, 480));
        JOptionPane.showMessageDialog(this, scroll, titulo, JOptionPane.PLAIN_MESSAGE);
    }
}
