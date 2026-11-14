package urna.view;

import urna.model.Partido;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * "Foto" do candidato: um retrato estilizado com as iniciais do nome,
 * na cor do partido. Dispensa arquivos de imagem.
 */
public class FotoCandidato extends JComponent {

    private static final Color[] CORES = {
            new Color(0x1E5AA8), new Color(0xC0392B), new Color(0x27AE60),
            new Color(0x8E44AD), new Color(0xD35400), new Color(0x16A085)
    };

    private final String legenda;
    private String nome;
    private Partido partido;

    public FotoCandidato(String legenda) {
        this.legenda = legenda;
        setPreferredSize(new Dimension(120, 160));
        setMaximumSize(new Dimension(120, 160));
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }

    /** Mostra a foto da pessoa; com {@code nome == null} a foto fica oculta. */
    public void mostrar(String nome, Partido partido) {
        this.nome = nome;
        this.partido = partido;
        setVisible(nome != null);
        repaint();
    }

    public void ocultar() {
        mostrar(null, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (nome == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int alturaLegenda = 20;

        Color cor = CORES[partido.getNumero() % CORES.length];
        g2.setColor(cor.brighter());
        g2.fillRect(0, 0, w, h - alturaLegenda);

        // Silhueta: cabeça e ombros
        g2.setColor(cor.darker());
        int cabeca = w / 2;
        g2.fillOval((w - cabeca) / 2, 18, cabeca, cabeca);
        g2.fillArc(10, 18 + cabeca, w - 20, cabeca + 30, 0, 180);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        String iniciais = iniciais(nome);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(iniciais, (w - fm.stringWidth(iniciais)) / 2, 18 + cabeca / 2 + fm.getAscent() / 2 - 3);

        g2.setColor(Color.WHITE);
        g2.fillRect(0, h - alturaLegenda, w, alturaLegenda);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(0, h - alturaLegenda, w, h - alturaLegenda);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        fm = g2.getFontMetrics();
        g2.drawString(legenda, (w - fm.stringWidth(legenda)) / 2, h - 6);
        g2.dispose();
    }

    private static String iniciais(String nomeCompleto) {
        String[] partes = nomeCompleto.trim().split("\\s+");
        String primeira = partes[0].substring(0, 1);
        String ultima = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
        return (primeira + ultima).toUpperCase();
    }
}
