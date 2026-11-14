package urna.view;

import urna.model.Partido;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Foto do candidato. Usa a imagem cadastrada, se houver; caso contrário,
 * desenha um retrato estilizado com as iniciais, na cor do partido.
 */
public class FotoCandidato extends JComponent {

    private static final Color[] CORES = {
            new Color(0x1E5AA8), new Color(0xC0392B), new Color(0x27AE60),
            new Color(0x8E44AD), new Color(0xD35400), new Color(0x16A085)
    };
    private static final Map<Path, BufferedImage> CACHE = new HashMap<>();

    private final String legenda;
    private String nome;
    private Partido partido;
    private BufferedImage imagem;

    public FotoCandidato(String legenda) {
        this.legenda = legenda;
        setPreferredSize(new Dimension(120, 160));
        setMaximumSize(new Dimension(120, 160));
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }

    /** Mostra a foto da pessoa. Com {@code nome == null}, a foto fica oculta. */
    public void mostrar(String nome, Partido partido, Path foto) {
        this.nome = nome;
        this.partido = partido;
        this.imagem = carregar(foto);
        setVisible(nome != null);
        repaint();
    }

    public void ocultar() {
        mostrar(null, null, null);
    }

    private static BufferedImage carregar(Path foto) {
        if (foto == null || !Files.isRegularFile(foto)) {
            return null;
        }
        return CACHE.computeIfAbsent(foto, p -> {
            try {
                return ImageIO.read(p.toFile());
            } catch (IOException e) {
                return null;
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (nome == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int w = getWidth();
        int h = getHeight();
        int alturaLegenda = 20;
        int alturaFoto = h - alturaLegenda;

        if (imagem != null) {
            desenharImagem(g2, w, alturaFoto);
        } else {
            desenharAvatar(g2, w, alturaFoto);
        }

        g2.setColor(Color.WHITE);
        g2.fillRect(0, alturaFoto, w, alturaLegenda);
        g2.setColor(Color.BLACK);
        g2.drawLine(0, alturaFoto, w, alturaFoto);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(legenda, (w - fm.stringWidth(legenda)) / 2, h - 6);
        g2.dispose();
    }

    /** Preenche a área recortando o excesso, sem distorcer a imagem. */
    private void desenharImagem(Graphics2D g2, int w, int h) {
        double escala = Math.max((double) w / imagem.getWidth(), (double) h / imagem.getHeight());
        int lw = (int) Math.round(imagem.getWidth() * escala);
        int lh = (int) Math.round(imagem.getHeight() * escala);
        g2.setClip(0, 0, w, h);
        g2.drawImage(imagem, (w - lw) / 2, (h - lh) / 2, lw, lh, null);
        g2.setClip(null);
    }

    private void desenharAvatar(Graphics2D g2, int w, int h) {
        Color cor = CORES[partido.getNumero() % CORES.length];
        g2.setColor(cor.brighter());
        g2.fillRect(0, 0, w, h);

        g2.setColor(cor.darker());
        int cabeca = w / 2;
        g2.fillOval((w - cabeca) / 2, 18, cabeca, cabeca);
        g2.fillArc(10, 18 + cabeca, w - 20, cabeca + 30, 0, 180);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        String iniciais = iniciais(nome);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(iniciais, (w - fm.stringWidth(iniciais)) / 2, 18 + cabeca / 2 + fm.getAscent() / 2 - 3);
    }

    private static String iniciais(String nomeCompleto) {
        String[] partes = nomeCompleto.trim().split("\\s+");
        String primeira = partes[0].substring(0, 1);
        String ultima = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
        return (primeira + ultima).toUpperCase();
    }
}
