package urna.view;

import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Eleicao;
import urna.model.Eleitor;
import urna.model.Partido;
import urna.persistencia.ArmazenamentoEmArquivo;
import urna.persistencia.Csv;
import urna.persistencia.ImportadorEleitores;
import urna.persistencia.RepositorioEleicao;
import urna.service.Urna;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Cadastro da eleição: dados gerais, cargos, partidos/chapas, candidatos e eleitores.
 * Daqui se salva a eleição e se inicia a votação.
 */
public class TelaConfiguracao extends JFrame {

    private final Eleicao eleicao;
    private final Path pasta;
    private final boolean didatico;
    private boolean alterado;

    private final JTabbedPane abas = new JTabbedPane();
    private final JTextField txtNome = new JTextField(30);
    private final JPasswordField txtSenha = new JPasswordField(12);
    private final JPasswordField txtSenhaConfirmacao = new JPasswordField(12);
    private final JTextArea txtResumo = new JTextArea(10, 40);
    private final JLabel lblStatus = new JLabel(" ");

    private final ModeloTabela<Cargo> modeloCargos = new ModeloTabela<Cargo>()
            .coluna("Ordem", this::ordemDoCargo)
            .coluna("Cargo", Cargo::getNome)
            .coluna("Dígitos", Cargo::getQuantidadeDigitos)
            .coluna("Tipo", Cargo::getTipo)
            .coluna("Vagas", Cargo::getVagas)
            .coluna("Vice", c -> c.possuiVice() ? c.getRotuloVice() : "-");
    private final ModeloTabela<Partido> modeloPartidos = new ModeloTabela<Partido>()
            .coluna("Número", Partido::getNumero)
            .coluna("Sigla", Partido::getSigla)
            .coluna("Nome", Partido::getNome);
    private final ModeloTabela<Candidato> modeloCandidatos = new ModeloTabela<Candidato>()
            .coluna("Cargo", Candidato::getCargo)
            .coluna("Número", Candidato::getNumero)
            .coluna("Nome", Candidato::getNome)
            .coluna("Partido/Chapa", c -> c.getPartido().getSigla())
            .coluna("Vice", Candidato::getNomeVice)
            .coluna("Foto", c -> c.getFoto() == null ? "-" : c.getFoto().getFileName());
    private final ModeloTabela<Eleitor> modeloEleitores = new ModeloTabela<Eleitor>()
            .coluna("Título / matrícula", Eleitor::getTitulo)
            .coluna("Nome", Eleitor::getNome);

    private final JTable tabCargos = tabela(modeloCargos);
    private final JTable tabPartidos = tabela(modeloPartidos);
    private final JTable tabCandidatos = tabela(modeloCandidatos);
    private final JTable tabEleitores = tabela(modeloEleitores);
    private final JLabel lblTotalEleitores = new JLabel();

    public TelaConfiguracao(Eleicao eleicao, Path pasta, boolean didatico) {
        super("Configurar eleição - " + eleicao.getNome());
        this.eleicao = eleicao;
        this.pasta = pasta;
        this.didatico = didatico;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (podeSair()) {
                    System.exit(0);
                }
            }
        });

        abas.addTab("1. Eleição", abaEleicao());
        abas.addTab("2. Cargos", abaCargos());
        abas.addTab("3. Partidos / Chapas", abaPartidos());
        abas.addTab("4. Candidatos", abaCandidatos());
        abas.addTab("5. Eleitores", abaEleitores());
        abas.addChangeListener(e -> atualizarResumo());

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
        lblStatus.setForeground(new java.awt.Color(0x2E7D32));
        barra.add(lblStatus, BorderLayout.CENTER);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.add(botao("Voltar", this::voltar));
        botoes.add(botao("Salvar", this::salvarComAviso));
        JButton iniciar = botao("Iniciar votação ▶", this::iniciarVotacao);
        iniciar.setFont(iniciar.getFont().deriveFont(Font.BOLD));
        botoes.add(iniciar);
        barra.add(botoes, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(abas, BorderLayout.CENTER);
        add(barra, BorderLayout.SOUTH);

        atualizarTabelas();
        setSize(900, 580);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // Abas
    // ------------------------------------------------------------------

    private JPanel abaEleicao() {
        txtNome.setText(eleicao.getNome());
        JPanel form = formulario(
                new String[]{"Nome da eleição:", "Senha do mesário:", "Repita a senha:"},
                new JComponent[]{txtNome, txtSenha, txtSenhaConfirmacao});

        JLabel dicaSenha = new JLabel("<html>A senha é pedida para encerrar a votação e para zerá-la. "
                + (eleicao.possuiSenhaMesario() ? "Deixe em branco para manter a senha atual." : "")
                + "<br>Pasta da eleição: " + pasta.toAbsolutePath() + "</html>");
        dicaSenha.setFont(dicaSenha.getFont().deriveFont(Font.PLAIN, 12f));

        txtResumo.setEditable(false);
        txtResumo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane resumo = new JScrollPane(txtResumo);
        resumo.setBorder(BorderFactory.createTitledBorder("Resumo e pendências"));

        JPanel topo = new JPanel(new BorderLayout(0, 6));
        topo.add(form, BorderLayout.NORTH);
        topo.add(dicaSenha, BorderLayout.CENTER);

        JPanel aba = new JPanel(new BorderLayout(0, 12));
        aba.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        aba.add(topo, BorderLayout.NORTH);
        aba.add(resumo, BorderLayout.CENTER);
        return aba;
    }

    private JPanel abaCargos() {
        return abaComTabela(tabCargos,
                "Cargos na ordem em que o eleitor vota. Majoritário: vence o mais votado. "
                        + "Proporcional: as vagas são divididas entre as chapas (aceita voto de legenda).",
                botao("Adicionar...", this::adicionarCargo),
                botao("Remover", this::removerCargo),
                botao("Subir ▲", () -> moverCargo(-1)),
                botao("Descer ▼", () -> moverCargo(1)));
    }

    private JPanel abaPartidos() {
        return abaComTabela(tabPartidos,
                "Cada partido ou chapa tem um número de 2 dígitos. O número dos candidatos começa por ele.",
                botao("Adicionar...", this::adicionarPartido),
                botao("Remover", this::removerPartido));
    }

    private JPanel abaCandidatos() {
        return abaComTabela(tabCandidatos,
                "A foto é opcional (JPG ou PNG). Sem foto, a urna mostra as iniciais do candidato.",
                botao("Adicionar...", this::adicionarCandidato),
                botao("Trocar foto...", this::trocarFoto),
                botao("Remover", this::removerCandidato));
    }

    private JPanel abaEleitores() {
        JPanel aba = abaComTabela(tabEleitores,
                "Importe a lista de uma planilha (CSV com as colunas: título ou matrícula; nome).",
                botao("Adicionar...", this::adicionarEleitor),
                botao("Importar planilha CSV...", this::importarEleitores),
                botao("Salvar planilha modelo...", this::salvarModeloPlanilha),
                botao("Remover selecionados", this::removerEleitores));
        lblTotalEleitores.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 0));
        aba.add(lblTotalEleitores, BorderLayout.SOUTH);
        return aba;
    }

    private JPanel abaComTabela(JTable tabela, String dica, JButton... botoes) {
        JLabel lblDica = new JLabel("<html>" + dica + "</html>");
        lblDica.setFont(lblDica.getFont().deriveFont(Font.PLAIN, 12f));
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (JButton b : botoes) {
            barra.add(b);
        }
        JPanel topo = new JPanel(new BorderLayout(0, 8));
        topo.add(lblDica, BorderLayout.NORTH);
        topo.add(barra, BorderLayout.CENTER);

        JPanel aba = new JPanel(new BorderLayout(0, 8));
        aba.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        aba.add(topo, BorderLayout.NORTH);
        aba.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return aba;
    }

    // ------------------------------------------------------------------
    // Cargos
    // ------------------------------------------------------------------

    private void adicionarCargo() {
        JTextField nome = new JTextField(20);
        JComboBox<Cargo.Tipo> tipo = new JComboBox<>(Cargo.Tipo.values());
        JSpinner digitos = new JSpinner(new SpinnerNumberModel(2, Cargo.MIN_DIGITOS, Cargo.MAX_DIGITOS, 1));
        JSpinner vagas = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        JCheckBox temVice = new JCheckBox("O cargo tem vice");
        JTextField rotuloVice = new JTextField(20);
        rotuloVice.setEnabled(false);
        temVice.addActionListener(e -> {
            rotuloVice.setEnabled(temVice.isSelected());
            if (temVice.isSelected() && rotuloVice.getText().trim().isEmpty()) {
                rotuloVice.setText("Vice-" + nome.getText().trim().toLowerCase());
            }
        });
        tipo.addActionListener(e -> {
            boolean proporcional = tipo.getSelectedItem() == Cargo.Tipo.PROPORCIONAL;
            vagas.setValue(proporcional ? 5 : 1);
            digitos.setValue(proporcional ? 3 : 2);
        });
        JPanel form = formulario(
                new String[]{"Nome do cargo:", "Tipo:", "Dígitos do número:", "Vagas:", "", "Nome do cargo do vice:"},
                new JComponent[]{nome, tipo, digitos, vagas, temVice, rotuloVice});

        while (dialogo(form, "Adicionar cargo")) {
            try {
                eleicao.adicionarCargo(new Cargo(nome.getText(), (Integer) digitos.getValue(),
                        (Cargo.Tipo) tipo.getSelectedItem(), (Integer) vagas.getValue(),
                        temVice.isSelected() ? rotuloVice.getText() : null));
                modificado();
                return;
            } catch (IllegalArgumentException ex) {
                erro(ex.getMessage());
            }
        }
    }

    private void removerCargo() {
        Cargo cargo = selecionado(tabCargos, modeloCargos);
        if (cargo != null && confirmar("Remover o cargo " + cargo.getNome() + "?")) {
            tentar(() -> eleicao.removerCargo(cargo));
        }
    }

    private void moverCargo(int deslocamento) {
        Cargo cargo = selecionado(tabCargos, modeloCargos);
        if (cargo == null) {
            return;
        }
        eleicao.moverCargo(cargo, deslocamento);
        modificado();
        int linha = eleicao.getCargos().indexOf(cargo);
        tabCargos.setRowSelectionInterval(linha, linha);
    }

    // ------------------------------------------------------------------
    // Partidos
    // ------------------------------------------------------------------

    private void adicionarPartido() {
        JSpinner numero = new JSpinner(new SpinnerNumberModel(sugerirNumeroPartido(), 10, 99, 1));
        JTextField sigla = new JTextField(10);
        JTextField nome = new JTextField(24);
        JPanel form = formulario(new String[]{"Número (2 dígitos):", "Sigla / nome curto:", "Nome completo:"},
                new JComponent[]{numero, sigla, nome});
        while (dialogo(form, "Adicionar partido / chapa")) {
            try {
                eleicao.cadastrarPartido((Integer) numero.getValue(), sigla.getText(), nome.getText());
                modificado();
                return;
            } catch (IllegalArgumentException ex) {
                erro(ex.getMessage());
            }
        }
    }

    private int sugerirNumeroPartido() {
        for (int n = 10; n <= 90; n += 10) {
            if (!eleicao.buscarPartido(n).isPresent()) {
                return n;
            }
        }
        return 11;
    }

    private void removerPartido() {
        Partido partido = selecionado(tabPartidos, modeloPartidos);
        if (partido != null && confirmar("Remover " + partido.getSigla() + "?")) {
            tentar(() -> eleicao.removerPartido(partido));
        }
    }

    // ------------------------------------------------------------------
    // Candidatos
    // ------------------------------------------------------------------

    private void adicionarCandidato() {
        if (eleicao.getCargos().isEmpty() || eleicao.listarPartidos().isEmpty()) {
            erro("Cadastre antes pelo menos um cargo e um partido/chapa.");
            return;
        }
        JComboBox<Cargo> cargo = new JComboBox<>(eleicao.getCargos().toArray(new Cargo[0]));
        JComboBox<Partido> partido = new JComboBox<>(eleicao.listarPartidos().toArray(new Partido[0]));
        JTextField numero = new JTextField(8);
        JTextField nome = new JTextField(24);
        JTextField vice = new JTextField(24);
        JLabel dica = new JLabel();
        dica.setFont(dica.getFont().deriveFont(Font.PLAIN, 11f));
        Path[] foto = {null};
        JButton escolherFoto = new JButton("Escolher foto...");
        escolherFoto.addActionListener(e -> {
            Path p = escolherImagem();
            if (p != null) {
                foto[0] = p;
                escolherFoto.setText(p.getFileName().toString());
            }
        });

        Runnable atualizar = () -> {
            Cargo c = (Cargo) cargo.getSelectedItem();
            Partido p = (Partido) partido.getSelectedItem();
            vice.setEnabled(c.possuiVice());
            dica.setText(c.getQuantidadeDigitos() + " dígitos, começando com " + p.getNumero()
                    + (c.possuiVice() ? "  |  vice: " + c.getRotuloVice() : ""));
            String atual = numero.getText().trim();
            if (atual.length() <= Partido.DIGITOS || !atual.startsWith(String.valueOf(p.getNumero()))) {
                numero.setText(String.valueOf(p.getNumero()));
            }
        };
        cargo.addActionListener(e -> atualizar.run());
        partido.addActionListener(e -> atualizar.run());
        atualizar.run();

        JPanel form = formulario(
                new String[]{"Cargo:", "Partido / chapa:", "Número:", "", "Nome:", "Nome do vice:", "Foto:"},
                new JComponent[]{cargo, partido, numero, dica, nome, vice, escolherFoto});
        while (dialogo(form, "Adicionar candidato")) {
            try {
                int n;
                try {
                    n = Integer.parseInt(numero.getText().trim());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Digite o número do candidato.");
                }
                eleicao.cadastrarCandidato(new Candidato(n, nome.getText(), (Partido) partido.getSelectedItem(),
                        (Cargo) cargo.getSelectedItem(), vice.getText(), foto[0]));
                modificado();
                return;
            } catch (IllegalArgumentException ex) {
                erro(ex.getMessage());
            }
        }
    }

    private void trocarFoto() {
        Candidato candidato = selecionado(tabCandidatos, modeloCandidatos);
        if (candidato == null) {
            return;
        }
        Path p = escolherImagem();
        if (p != null) {
            tentar(() -> eleicao.atualizarCandidato(candidato.comFoto(p)));
        }
    }

    private Path escolherImagem() {
        JFileChooser seletor = new JFileChooser();
        seletor.setFileFilter(new FileNameExtensionFilter("Imagens (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif", "bmp"));
        return seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? seletor.getSelectedFile().toPath() : null;
    }

    private void removerCandidato() {
        Candidato candidato = selecionado(tabCandidatos, modeloCandidatos);
        if (candidato != null && confirmar("Remover o candidato " + candidato.getNome() + "?")) {
            tentar(() -> eleicao.removerCandidato(candidato));
        }
    }

    // ------------------------------------------------------------------
    // Eleitores
    // ------------------------------------------------------------------

    private void adicionarEleitor() {
        JTextField titulo = new JTextField(14);
        JTextField nome = new JTextField(24);
        JPanel form = formulario(new String[]{"Título / matrícula (só números):", "Nome:"},
                new JComponent[]{titulo, nome});
        while (dialogo(form, "Adicionar eleitor")) {
            try {
                eleicao.cadastrarEleitor(titulo.getText(), nome.getText());
                modificado();
                return;
            } catch (IllegalArgumentException ex) {
                erro(ex.getMessage());
            }
        }
    }

    private void importarEleitores() {
        JFileChooser seletor = new JFileChooser();
        seletor.setFileFilter(new FileNameExtensionFilter("Planilha CSV", "csv", "txt"));
        if (seletor.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            ImportadorEleitores.Resultado r = new ImportadorEleitores().importar(seletor.getSelectedFile().toPath(), eleicao);
            modificado();
            StringBuilder msg = new StringBuilder(r.getImportados() + " eleitor(es) importado(s).");
            if (!r.getErros().isEmpty()) {
                msg.append("\n\n").append(r.getErros().size()).append(" linha(s) recusada(s):\n");
                r.getErros().stream().limit(15).forEach(e -> msg.append(e).append('\n'));
                if (r.getErros().size() > 15) {
                    msg.append("...");
                }
            }
            JOptionPane.showMessageDialog(this, msg.toString(), "Importar eleitores",
                    r.getErros().isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            erro("Não foi possível ler a planilha: " + ex.getMessage());
        }
    }

    private void salvarModeloPlanilha() {
        JFileChooser seletor = new JFileChooser();
        seletor.setSelectedFile(new File("eleitores_modelo.csv"));
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Csv.escrever(seletor.getSelectedFile().toPath(), Arrays.asList(
                    Arrays.asList("titulo", "nome"),
                    Arrays.asList("2026001", "Maria da Silva"),
                    Arrays.asList("2026002", "João Santos")));
            JOptionPane.showMessageDialog(this, "Modelo salvo. Abra no Excel, preencha e importe.",
                    "Planilha modelo", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            erro("Não foi possível salvar: " + ex.getMessage());
        }
    }

    private void removerEleitores() {
        int[] linhas = tabEleitores.getSelectedRows();
        if (linhas.length == 0 || !confirmar("Remover " + linhas.length + " eleitor(es)?")) {
            return;
        }
        List<Eleitor> remover = new ArrayList<>();
        for (int l : linhas) {
            remover.add(modeloEleitores.getLinha(tabEleitores.convertRowIndexToModel(l)));
        }
        remover.forEach(eleicao::removerEleitor);
        modificado();
    }

    // ------------------------------------------------------------------
    // Salvar e iniciar
    // ------------------------------------------------------------------

    /** Copia nome e senha dos campos para a eleição. */
    private boolean aplicarDadosGerais() {
        try {
            if (!txtNome.getText().trim().equals(eleicao.getNome())) {
                eleicao.setNome(txtNome.getText());
                setTitle("Configurar eleição - " + eleicao.getNome());
                alterado = true;
            }
            String senha = new String(txtSenha.getPassword());
            String confirmacao = new String(txtSenhaConfirmacao.getPassword());
            if (!senha.isEmpty() || !confirmacao.isEmpty()) {
                if (!senha.equals(confirmacao)) {
                    throw new IllegalArgumentException("As senhas digitadas não são iguais.");
                }
                eleicao.setSenhaMesario(senha);
                txtSenha.setText("");
                txtSenhaConfirmacao.setText("");
                alterado = true;
            }
            return true;
        } catch (IllegalArgumentException ex) {
            abas.setSelectedIndex(0);
            erro(ex.getMessage());
            return false;
        }
    }

    private boolean salvar() {
        if (!aplicarDadosGerais()) {
            return false;
        }
        try {
            new RepositorioEleicao().salvar(eleicao, pasta);
            alterado = false;
            lblStatus.setText("Salvo em " + pasta.toAbsolutePath());
            atualizarTabelas();
            return true;
        } catch (IOException ex) {
            erro("Não foi possível salvar a eleição: " + ex.getMessage());
            return false;
        }
    }

    private void salvarComAviso() {
        if (salvar()) {
            JOptionPane.showMessageDialog(this, "Eleição salva em:\n" + pasta.toAbsolutePath()
                            + "\n\nOs arquivos CSV dessa pasta podem ser abertos no Excel.",
                    "Salvar", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void iniciarVotacao() {
        if (!aplicarDadosGerais()) {
            return;
        }
        List<String> pendencias = eleicao.verificarPendencias();
        if (!pendencias.isEmpty()) {
            abas.setSelectedIndex(0);
            atualizarResumo();
            erro("Antes de iniciar a votação:\n• " + String.join("\n• ", pendencias));
            return;
        }
        if (!salvar()) {
            return;
        }
        ArmazenamentoEmArquivo armazenamento = new ArmazenamentoEmArquivo(RepositorioEleicao.pastaVotacao(pasta));
        if (armazenamento.existe()) {
            JOptionPane.showMessageDialog(this, "Já existe uma votação gravada para esta eleição.\n"
                    + "Para começar outra, ela será guardada em uma pasta de arquivo.", "Iniciar votação",
                    JOptionPane.INFORMATION_MESSAGE);
            if (!TelaInicial.zerarVotacao(this, eleicao, armazenamento)) {
                return;
            }
        }
        if (!confirmar("Iniciar a votação?\n\nDepois de iniciada, a configuração fica bloqueada "
                + "(só pode ser alterada zerando a votação).")) {
            return;
        }
        try {
            Urna urna = Urna.nova(eleicao, armazenamento);
            new TelaUrna(urna, pasta, didatico).setVisible(true);
            dispose();
        } catch (UncheckedIOException | IllegalStateException ex) {
            erro("Não foi possível iniciar a votação: " + ex.getMessage());
        }
    }

    private void voltar() {
        if (podeSair()) {
            dispose();
            new TelaInicial().setVisible(true);
        }
    }

    /** Pergunta se salva as alterações pendentes. */
    private boolean podeSair() {
        if (!alterado && txtNome.getText().trim().equals(eleicao.getNome())) {
            return true;
        }
        int r = JOptionPane.showConfirmDialog(this, "Salvar as alterações antes de sair?", "Sair",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            return salvar();
        }
        return r == JOptionPane.NO_OPTION;
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private void modificado() {
        alterado = true;
        lblStatus.setText("Alterações não salvas");
        atualizarTabelas();
    }

    private int ordemDoCargo(Cargo cargo) {
        return eleicao.getCargos().indexOf(cargo) + 1;
    }

    private void atualizarTabelas() {
        modeloCargos.setLinhas(eleicao.getCargos());
        modeloPartidos.setLinhas(eleicao.listarPartidos());
        modeloCandidatos.setLinhas(eleicao.listarTodosCandidatos());
        modeloEleitores.setLinhas(eleicao.listarEleitores());
        lblTotalEleitores.setText(eleicao.listarEleitores().size() + " eleitor(es) cadastrado(s)");
        atualizarResumo();
    }

    private void atualizarResumo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cargos (ordem de votação):\n");
        for (Cargo c : eleicao.getCargos()) {
            sb.append(String.format("  %-28s %-13s %d vaga(s), %d candidato(s)%n", c.getNome(),
                    c.getTipo(), c.getVagas(), eleicao.listarCandidatos(c).size()));
        }
        sb.append("Partidos/chapas: ").append(eleicao.listarPartidos().size()).append('\n');
        sb.append("Eleitores:       ").append(eleicao.listarEleitores().size()).append('\n');
        sb.append("Senha do mesário: ").append(eleicao.possuiSenhaMesario() ? "definida" : "NÃO definida").append("\n\n");
        List<String> pendencias = eleicao.verificarPendencias();
        if (pendencias.isEmpty()) {
            sb.append("✔ Tudo pronto para iniciar a votação.");
        } else {
            sb.append("Pendências:\n");
            pendencias.forEach(p -> sb.append("  • ").append(p).append('\n'));
        }
        txtResumo.setText(sb.toString());
    }

    private void tentar(Runnable acao) {
        try {
            acao.run();
            modificado();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            erro(ex.getMessage());
        }
    }

    private static JTable tabela(ModeloTabela<?> modelo) {
        JTable t = new JTable(modelo);
        t.setRowHeight(24);
        t.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        t.setAutoCreateRowSorter(true);
        t.setFillsViewportHeight(true);
        return t;
    }

    private <T> T selecionado(JTable tabela, ModeloTabela<T> modelo) {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma linha da tabela.", getTitle(),
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return modelo.getLinha(tabela.convertRowIndexToModel(linha));
    }

    private static JButton botao(String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> acao.run());
        return b;
    }

    private static JPanel formulario(String[] rotulos, JComponent[] campos) {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < campos.length; i++) {
            g.gridy = i;
            g.gridx = 0;
            g.fill = GridBagConstraints.NONE;
            form.add(new JLabel(rotulos[i]), g);
            g.gridx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            form.add(campos[i], g);
        }
        return form;
    }

    private boolean dialogo(JPanel form, String titulo) {
        return JOptionPane.showConfirmDialog(this, form, titulo, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private boolean confirmar(String pergunta) {
        return JOptionPane.showConfirmDialog(this, pergunta, getTitle(), JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    private void erro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, getTitle(), JOptionPane.WARNING_MESSAGE);
    }
}
