/* UI REDESIGN PACKAGE
Objetivo: aplicar fondo con gradientes, glassmorphism, glow,
sombras, timer premium y dashboard moderno sin alterar la lógica.
*/

package trivianet;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

/**
 * Cliente de TriviaNet — Rediseño visual 2025.
 * <p>
 * Temas demostrados:
 * - Swing: interfaz gráfica con CardLayout y componentes personalizados
 * - Threads: hilo lector de mensajes del servidor
 * - Networking: Socket TCP para comunicación con el servidor
 * - Java I/O: BufferedReader/PrintWriter sobre streams del socket
 * <p>
 * CAMBIOS VISUALES (sin modificar lógica ni protocolo):
 * - Paleta oscura: #0F172A fondo, #1E293B tarjetas, naranja #F97316 primario
 * - Tipografía Inter/Segoe UI con jerarquías claras
 * - RoundedPanel reutilizable para todas las tarjetas
 * - Timer circular animado con arco proporcional (reemplaza JLabel plano)
 * - Botones de respuesta con badge de letra + fondo semitransparente
 * - Lobby con tarjetas de jugador individuales (reemplaza JTextArea)
 * - Scoreboard con filas individuales por jugador (reemplaza JTextArea)
 * - Feedback bar con ícono y puntos ganados
 */
public class TriviaClient extends JFrame {

    // ========== Paleta de colores ==========
    static final Color C_BG = new Color(0x0F172A);
    static final Color C_CARD = new Color(0x1E293B);
    static final Color C_CARD2 = new Color(0x263347);
    static final Color C_BORDER = new Color(0x334155);
    static final Color C_ORANGE = new Color(0xF97316);
    static final Color C_BLUE = new Color(0x38BDF8);
    static final Color C_GREEN = new Color(0x22C55E);
    static final Color C_RED = new Color(0xEF4444);
    static final Color C_YELLOW = new Color(0xFACC15);
    static final Color C_PURPLE = new Color(0xA855F7);
    static final Color C_TEXT = new Color(0xF8FAFC);
    static final Color C_MUTED = new Color(0x94A3B8);
    static final Color C_DISABLED = new Color(0x475569);

    // (colores semitransparentes de opciones definidos inline en createGamePanel)

    // Fuentes
    static Font fontTitle(int size) {
        return resolveFont(Font.BOLD, size);
    }

    static Font fontSemi(int size) {
        return resolveFont(Font.BOLD, size);
    }

    static Font fontBody(int size) {
        return resolveFont(Font.PLAIN, size);
    }

    static Font fontMono(int size) {
        return new Font("Consolas", Font.PLAIN, size);
    }

    private static Font resolveFont(int style, int size) {
        String[] candidates = {"Inter", "Segoe UI", "SF Pro Display", "Helvetica Neue", "Arial"};
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> available = new java.util.HashSet<>();
        for (Font f : ge.getAllFonts()) available.add(f.getFamily());
        for (String name : candidates) {
            if (available.contains(name)) return new Font(name, style, size);
        }
        return new Font("SansSerif", style, size);
    }

    // ========== Estado de la conexión ==========
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected;

    // CardLayout para cambiar entre pantallas
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // ===== Pantalla LOGIN =====
    private JTextField nameField;
    private JTextField ipField;
    private JTextField portField;
    private JButton connectButton;

    // ===== Pantalla LOBBY =====
    private JPanel lobbyPlayersPanel;   // antes JTextArea
    private JLabel lobbyStatusLabel;
    private JLabel lobbyCountLabel;

    // ===== Pantalla GAME =====
    private JLabel questionNumLabel;
    private JPanel questionDotsPanel;
    private int currentQuestion = 0;
    private TimerCircle timerCircle;    // antes JLabel plano
    private JLabel scoreLabel;
    private JLabel questionTextLabel;
    private JButton[] answerButtons;
    private RoundedPanel feedbackPanel;
    private JLabel feedbackIconLabel;
    private JLabel feedbackSubLabel;
    private JLabel feedbackPtsLabel;
    private JProgressBar progressBar;
    private int totalQuestions = 10;

    // ===== Pantalla GAMEOVER =====
    private JPanel scoreboardPanel;    // antes JTextArea
    private JLabel winnerLabel;
    private JLabel winnerPtsLabel;

    // Estado
    private String playerName;
    private int currentScore;
    private boolean canAnswer;

    // Colores ciclados para avatares de jugadores (package-private para que TriviaServer pueda usarlos)
    static final Color[] AVATAR_COLORS = {
            new Color(0x38BDF8), new Color(0xA855F7),
            new Color(0x22C55E), new Color(0xF97316),
            new Color(0xFACC15), new Color(0xEF4444)
    };

    // ========================================================
    //  CONSTRUCTOR
    // ========================================================

    public TriviaClient() {
        currentScore = 0;
        connected = false;
        canAnswer = false;
        initGUI();
    }

    // ========================================================
    //  COMPONENTES PERSONALIZADOS REUTILIZABLES
    // ========================================================

    /**
     * Panel con esquinas redondeadas y color de fondo sólido.
     * Sustituye el uso de setBorder(...) + setBackground en paneles.
     */
    static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg;
        private final Color borderColor;
        private final int borderWidth;

        RoundedPanel(int radius, Color bg) {
            this(radius, bg, null, 0);
        }

        RoundedPanel(int radius, Color bg, Color borderColor, int borderWidth) {
            this.radius = radius;
            this.bg = bg;
            this.borderColor = borderColor;
            this.borderWidth = borderWidth;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (borderColor != null && borderWidth > 0) {
                g2.setColor(borderColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius + borderWidth, radius + borderWidth);
            }
            g2.setColor(bg);
            int off = (borderColor != null) ? borderWidth : 0;
            g2.fillRoundRect(off, off, getWidth() - off * 2, getHeight() - off * 2, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Campo de texto con estilo oscuro: fondo C_CARD2, borde redondeado,
     * placeholder en color muted.
     */
    static class DarkField extends JTextField {
        private String placeholder;
        private boolean showingPlaceholder;

        DarkField(String placeholder, int cols) {
            super(cols);
            this.placeholder = placeholder;
            this.showingPlaceholder = true;

            setFont(fontBody(15));
            setForeground(C_MUTED);  // Color del placeholder
            setBackground(C_CARD2);
            setCaretColor(C_TEXT);
            setBorder(BorderFactory.createCompoundBorder(
                    new RoundBorder(C_BORDER, 8, 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            setOpaque(true);

            // Mostrar placeholder inicial
            setText(placeholder);

            // Listeners para manejar el placeholder
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (showingPlaceholder) {
                        setText("");
                        setForeground(C_TEXT);
                        showingPlaceholder = false;
                    }
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (getText().trim().isEmpty()) {
                        setText(placeholder);
                        setForeground(C_MUTED);
                        showingPlaceholder = true;
                    }
                }
            });
        }

        @Override
        public String getText() {
            return showingPlaceholder ? "" : super.getText();
        }
    }

    /**
     * Borde redondeado ligero para JTextField.
     */
    static class RoundBorder extends AbstractBorder {
        private final Color color;
        private final int radius, thickness;

        RoundBorder(Color color, int radius, int thickness) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(6, 10, 6, 10);
        }
    }

    /**
     * JButton con estilo moderno: fondo sólido, esquinas redondeadas,
     * efecto hover sutil, sin borde por defecto de Swing.
     */
    static class ModernButton extends JButton {
        private final Color normalBg;
        private final Color hoverBg;
        private boolean hovered = false;

        ModernButton(String text, Color bg, Color fg, int radius) {
            super(text);
            this.normalBg = bg;
            this.hoverBg = bg.brighter();
            setFont(fontTitle(15));
            setForeground(fg);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isEnabled() ? (hovered ? hoverBg : normalBg) : C_DISABLED);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Temporizador circular: muestra el número de segundos dentro de un arco
     * que se vacía conforme pasa el tiempo. Se vuelve rojo cuando quedan <= 5 s.
     */
    class TimerCircle extends JPanel {
        private int seconds = 15;
        private int maxSeconds = 15;
        private static final int SIZE = 88;

        TimerCircle() {
            setPreferredSize(new Dimension(SIZE, SIZE));
            setOpaque(false);
        }

        void update(int secs) {
            this.seconds = secs;
            repaint();
        }

        void setMax(int max) {
            this.maxSeconds = max;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int stroke = 6;
            int pad = stroke / 2 + 2;
            int d = SIZE - pad * 2;

            // Fondo del anillo
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(C_CARD2);
            g2.drawArc(pad, pad, d, d, 0, 360);

            // Arco del progreso
            float ratio = (maxSeconds > 0) ? (float) seconds / maxSeconds : 0f;
            int angle = Math.round(360 * ratio);
            Color arcColor = (seconds <= 5) ? C_RED : C_ORANGE;
            g2.setColor(arcColor);
            g2.drawArc(pad, pad, d, d, 90, -angle);

            // Número centrado (ligeramente hacia arriba para dejar espacio a "seg")
            g2.setFont(fontSemi(23));
            g2.setColor((seconds <= 5) ? C_RED : C_TEXT);
            FontMetrics fmNum = g2.getFontMetrics();
            String txt = String.valueOf(seconds);
            int tx = (SIZE - fmNum.stringWidth(txt)) / 2;
            int ty = SIZE / 2 + fmNum.getAscent() / 2 - 6;
            g2.drawString(txt, tx, ty);

            // Etiqueta "seg" debajo del número
            g2.setFont(fontBody(11));
            g2.setColor(C_MUTED);
            FontMetrics fmSeg = g2.getFontMetrics();
            String seg = "seg";
            int sx = (SIZE - fmSeg.stringWidth(seg)) / 2;
            g2.drawString(seg, sx, ty + fmNum.getDescent() + fmSeg.getAscent() - 2);

            g2.dispose();
        }
    }

    /**
     * Tarjeta de jugador para el lobby: avatar circular + nombre + punto de estado.
     */
    class PlayerCard extends RoundedPanel {
        PlayerCard(String name, boolean isMe, int colorIndex) {
            super(10, C_CARD, C_BORDER, 1);
            setLayout(new BorderLayout(12, 0));
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 14));

            // Avatar circular con inicial
            JPanel avatar = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color base = AVATAR_COLORS[colorIndex % AVATAR_COLORS.length];
                    Color bg = new Color(base.getRed(), base.getGreen(), base.getBlue(), 40);
                    g2.setColor(bg);
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.setColor(base);
                    g2.setFont(fontSemi(15));
                    FontMetrics fm = g2.getFontMetrics();
                    String initial = name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();
                    g2.drawString(initial,
                            (getWidth() - fm.stringWidth(initial)) / 2,
                            (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            };
            avatar.setOpaque(false);
            avatar.setPreferredSize(new Dimension(36, 36));

            // Nombre + etiqueta "Tú"
            JPanel info = new JPanel(new GridLayout(isMe ? 2 : 1, 1, 0, 1));
            info.setOpaque(false);
            JLabel nameLbl = new JLabel(name);
            nameLbl.setFont(fontBody(15));
            nameLbl.setForeground(C_TEXT);
            info.add(nameLbl);
            if (isMe) {
                JLabel youLbl = new JLabel("Tú");
                youLbl.setFont(fontBody(12));
                youLbl.setForeground(C_MUTED);
                info.add(youLbl);
            }

            // Punto de estado verde
            JPanel dot = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(C_GREEN);
                    int s = 8;
                    g2.fillOval((getWidth() - s) / 2, (getHeight() - s) / 2, s, s);
                    g2.dispose();
                }
            };
            dot.setOpaque(false);
            dot.setPreferredSize(new Dimension(16, 16));

            add(avatar, BorderLayout.WEST);
            add(info, BorderLayout.CENTER);
            add(dot, BorderLayout.EAST);
        }
    }

    // ========================================================
    //  CONSTRUCCIÓN DE LA GUI
    // ========================================================

    private void initGUI() {
        // Desactivar Nimbus antes de crear cualquier componente
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        setTitle("TriviaNet");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(520, 460));
        getContentPane().setBackground(C_BG);
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(C_BG);

        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createLobbyPanel(), "LOBBY");
        mainPanel.add(createGamePanel(), "GAME");
        mainPanel.add(createGameOverPanel(), "GAMEOVER");

        add(mainPanel);
        showPanel("LOGIN");
    }

    // ========================================================
    //  PANTALLA: LOGIN
    // ========================================================

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(C_BG);

        // Tarjeta central
        RoundedPanel card = new RoundedPanel(16, C_CARD, C_BORDER, 1);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(340, 430));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 28, 0, 28);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridwidth = 1;

        // --- Título ---
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 28, 2, 28);
        JLabel title = new JLabel("TriviaNet", SwingConstants.CENTER);
        title.setFont(fontTitle(30));
        title.setForeground(C_TEXT);
        // "Net" en naranja mediante HTML
        title.setText("<html><span style='color:#F8FAFC'>Trivia</span><span style='color:#F97316'>Net</span></html>");
        card.add(title, gbc);

        // --- Subtítulo ---
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 28, 22, 28);
        JLabel sub = new JLabel("Conecta y demuéstrate en el juego de trivia", SwingConstants.CENTER);
        sub.setFont(fontBody(13));
        sub.setForeground(C_MUTED);
        card.add(sub, gbc);

        // En createLoginPanel(), después de crear nameField:
// --- Campo: Nombre (obligatorio) ---
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 28, 6, 28);

// Label con indicador de obligatorio
        JPanel nameLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        nameLabelPanel.setOpaque(false);
        JLabel nameLabel = new JLabel("Tu nombre");
        nameLabel.setFont(fontBody(13));
        nameLabel.setForeground(C_MUTED);
        nameLabelPanel.add(nameLabel);

        JLabel required = new JLabel("*");
        required.setFont(fontBody(13));
        required.setForeground(C_RED);
        nameLabelPanel.add(required);

        card.add(nameLabelPanel, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 28, 14, 28);
        nameField = new DarkField("ej: Jugador1", 18);
        card.add(nameField, gbc);

// --- Campo: IP ---
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 28, 6, 28);
        card.add(fieldLabel("IP del servidor"), gbc);
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 28, 14, 28);
        ipField = new DarkField("ej: localhost", 18);  // Placeholder descriptivo
        card.add(ipField, gbc);

// --- Campo: Puerto ---
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 28, 6, 28);
        card.add(fieldLabel("Puerto"), gbc);
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 28, 20, 28);
        portField = new DarkField("5000", 18);  // Valor por defecto
// Hacer que el puerto no muestre placeholder (ya tiene valor)
        portField.setForeground(C_TEXT);
        card.add(portField, gbc);

        // --- Botón Conectar ---
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 28, 28, 28);
        connectButton = new ModernButton("CONECTAR", C_ORANGE, Color.WHITE, 10);
        connectButton.setFont(fontTitle(16));
        connectButton.setPreferredSize(new Dimension(280, 44));
        connectButton.addActionListener(e -> connect());
        card.add(connectButton, gbc);

        nameField.addActionListener(e -> connect());
        portField.addActionListener(e -> connect());

        panel.add(card);
        return panel;
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(fontBody(13));
        lbl.setForeground(C_MUTED);
        return lbl;
    }

    // ========================================================
    //  PANTALLA: LOBBY
    // ========================================================

    private JPanel createLobbyPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(C_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 28, 24, 28));

        // --- Header ---
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("Sala de Espera");
        title.setFont(fontTitle(26));
        title.setForeground(C_TEXT);
        header.add(title, BorderLayout.NORTH);

        lobbyCountLabel = new JLabel("0 jugadores conectados");
        lobbyCountLabel.setFont(fontBody(14));
        lobbyCountLabel.setForeground(C_MUTED);
        header.add(lobbyCountLabel, BorderLayout.SOUTH);

        panel.add(header, BorderLayout.NORTH);

        // --- Sección de jugadores ---
        JPanel playersSection = new JPanel(new BorderLayout(0, 8));
        playersSection.setOpaque(false);

        JLabel sectionLabel = sectionTitle("JUGADORES");
        playersSection.add(sectionLabel, BorderLayout.NORTH);

        lobbyPlayersPanel = new JPanel();
        lobbyPlayersPanel.setLayout(new BoxLayout(lobbyPlayersPanel, BoxLayout.Y_AXIS));
        lobbyPlayersPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(lobbyPlayersPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        playersSection.add(scroll, BorderLayout.CENTER);

        panel.add(playersSection, BorderLayout.CENTER);

        // --- Footer de estado ---
        RoundedPanel footer = new RoundedPanel(10, C_CARD, C_BORDER, 1);
        footer.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));

        lobbyStatusLabel = new JLabel("Esperando a que el host inicie la partida...");
        lobbyStatusLabel.setFont(fontBody(14));
        lobbyStatusLabel.setForeground(C_MUTED);
        footer.add(lobbyStatusLabel);

        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(fontBody(12).getFamily(), Font.BOLD, 10));
        lbl.setForeground(C_MUTED);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return lbl;
    }

    // ========================================================
    //  PANTALLA: JUEGO
    // ========================================================

    private JPanel createGamePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(C_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        // ---- Barra superior con marca TriviaNet ----
        RoundedPanel topBar = new RoundedPanel(10, C_CARD, C_BORDER, 1);
        topBar.setLayout(new BorderLayout(0, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        // Izquierda: marca "TriviaNet" (sin subtítulo)
        JLabel brandMain = new JLabel(
                "<html><span style='color:#F8FAFC'>Trivia</span>"
                        + "<span style='color:#F97316'>Net</span></html>"
        );
        brandMain.setFont(fontTitle(21));

        // Centro: segmentos de progreso — un rectángulo por pregunta
        questionNumLabel = new JLabel(""); // se mantiene por compatibilidad
        questionDotsPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int total = totalQuestions;
                if (total <= 0) total = 10;
                int gap = 4;
                int h = 8;
                int totalGaps = gap * (total - 1);
                int segW = (getWidth() - totalGaps) / total;
                int y = (getHeight() - h) / 2;
                for (int i = 0; i < total; i++) {
                    int x = i * (segW + gap);
                    if (i < currentQuestion) {
                        g2.setColor(C_ORANGE);
                    } else if (i == currentQuestion - 1) {
                        g2.setColor(C_ORANGE);
                    } else {
                        g2.setColor(C_CARD2);
                    }
                    g2.fillRoundRect(x, y, segW, h, 4, 4);
                }
                g2.dispose();
            }
        };
        questionDotsPanel.setOpaque(false);
        questionDotsPanel.setPreferredSize(new Dimension(0, 24));

        JPanel centerInfo = new JPanel(new BorderLayout());
        centerInfo.setOpaque(false);
        centerInfo.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        centerInfo.add(questionDotsPanel, BorderLayout.CENTER);

        // Mantener progressBar como campo (no se muestra, solo para compatibilidad)
        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);

        // Derecha: puntaje
        scoreLabel = new JLabel("Puntaje: 0");
        scoreLabel.setFont(fontSemi(14));
        scoreLabel.setForeground(C_YELLOW);

        topBar.add(brandMain, BorderLayout.WEST);
        topBar.add(centerInfo, BorderLayout.CENTER);
        topBar.add(scoreLabel, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        // ---- Centro: tarjeta pregunta (con timer integrado) + respuestas ----
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setOpaque(false);

        // Timer circular — ahora dentro de la tarjeta de pregunta
        timerCircle = new TimerCircle();

        // Tarjeta de pregunta: timer a la izquierda, texto centrado
        RoundedPanel questionCard = new RoundedPanel(14, C_CARD, C_BORDER, 1);
        questionCard.setLayout(new BorderLayout(16, 0));
        questionCard.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 20));

        // Envolver el timer para centrarlo verticalmente
        JPanel timerWrap = new JPanel(new GridBagLayout());
        timerWrap.setOpaque(false);
        timerWrap.add(timerCircle);

        questionTextLabel = new JLabel("<html><center>¿Pregunta?</center></html>", SwingConstants.CENTER);
        questionTextLabel.setFont(fontSemi(20));
        questionTextLabel.setForeground(C_TEXT);
        questionTextLabel.setVerticalAlignment(SwingConstants.CENTER);
        questionTextLabel.setHorizontalAlignment(SwingConstants.CENTER);

        questionCard.add(timerWrap, BorderLayout.WEST);
        questionCard.add(questionTextLabel, BorderLayout.CENTER);

        centerPanel.add(questionCard, BorderLayout.NORTH);

        // Grid de respuestas 2×2
        JPanel answersPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        answersPanel.setOpaque(false);

        // Configuración visual de cada opción: color de fondo alfa y de badge
        Color[] ansAlpha = {
                new Color(239, 68, 68, 30),    // A rojo
                new Color(56, 189, 248, 30),   // B azul
                new Color(250, 204, 21, 30),   // C amarillo
                new Color(34, 197, 94, 30)     // D verde
        };
        Color[] ansBorder = {C_RED, C_BLUE, C_YELLOW, C_GREEN};
        Color[] ansLetterFg = {Color.WHITE, new Color(0x0F172A), new Color(0x0F172A), Color.WHITE};
        String[] letters = {"A", "B", "C", "D"};

        answerButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            answerButtons[i] = createAnswerButton(letters[i], "...",
                    ansAlpha[i], ansBorder[i], ansBorder[i], ansLetterFg[i]);
            answerButtons[i].addActionListener(e -> submitAnswer(letters[idx]));
            answersPanel.add(answerButtons[i]);
        }

        centerPanel.add(answersPanel, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        feedbackPanel = buildFeedbackPanel();
        panel.add(feedbackPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Crea un botón de respuesta con badge de letra y fondo semitransparente.
     * El texto de la opción se guarda en el nombre del botón para poder actualizarlo.
     */
    private JButton createAnswerButton(String letter, String optionText,
                                       Color bgAlpha, Color borderColor,
                                       Color letterBg, Color letterFg) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fondo
                Color currentBg = isEnabled() ? bgAlpha : new Color(30, 41, 59, 60);
                if (getModel().isRollover() && isEnabled()) {
                    currentBg = new Color(
                            Math.min(255, bgAlpha.getRed() + 20),
                            Math.min(255, bgAlpha.getGreen() + 20),
                            Math.min(255, bgAlpha.getBlue() + 20),
                            Math.min(255, bgAlpha.getAlpha() + 20)
                    );
                }
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                // Borde
                g2.setColor(isEnabled() ? borderColor : C_BORDER);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                // Badge de letra
                int badgeSize = 28;
                int badgeX = 10;
                int badgeY = (getHeight() - badgeSize) / 2;
                g2.setColor(isEnabled() ? letterBg : C_DISABLED);
                g2.fillRoundRect(badgeX, badgeY, badgeSize, badgeSize, 7, 7);

                g2.setFont(fontSemi(14));
                g2.setColor(isEnabled() ? letterFg : C_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(letter,
                        badgeX + (badgeSize - fm.stringWidth(letter)) / 2,
                        badgeY + (badgeSize + fm.getAscent() - fm.getDescent()) / 2);

                // Texto de la opción
                g2.setFont(fontBody(15));
                FontMetrics fm2 = g2.getFontMetrics();
                g2.setColor(isEnabled() ? C_TEXT : C_MUTED);
                String txt = getText();
                int textX = badgeX + badgeSize + 10;
                int maxW = getWidth() - textX - 8;
                // Truncar si es muy largo
                while (fm2.stringWidth(txt) > maxW && txt.length() > 3) {
                    txt = txt.substring(0, txt.length() - 4) + "...";
                }
                g2.drawString(txt, textX,
                        (getHeight() + fm2.getAscent() - fm2.getDescent()) / 2);

                g2.dispose();
            }
        };

        btn.setText(optionText);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(0, 60));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Guardar referencia al color de borde para resaltar correcta/incorrecta
        btn.putClientProperty("borderColor", borderColor);
        btn.putClientProperty("bgAlpha", bgAlpha);
        btn.putClientProperty("letter", letter);
        return btn;
    }

    /**
     * Panel de retroalimentación (correcto / incorrecto)
     */
    private RoundedPanel buildFeedbackPanel() {
        // Fondo verde semi-transparente por defecto; showResult lo cambia si es incorrecto
        RoundedPanel fp = new RoundedPanel(12, new Color(34, 197, 94, 25),
                new Color(34, 197, 94, 80), 1);
        fp.setLayout(new BorderLayout(12, 0));
        fp.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // Izquierda: círculo ícono + textos
        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);

        // Círculo con check/x — se dibuja en showResult actualizando el label
        feedbackIconLabel = new JLabel("¡Correcto!");
        feedbackIconLabel.setFont(fontSemi(18));
        feedbackIconLabel.setForeground(C_GREEN);

        feedbackSubLabel = new JLabel("Respuesta correcta: —");
        feedbackSubLabel.setFont(fontBody(14));
        feedbackSubLabel.setForeground(C_MUTED);

        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 3));
        texts.setOpaque(false);
        texts.add(feedbackIconLabel);
        texts.add(feedbackSubLabel);
        left.add(texts, BorderLayout.CENTER);

        // Derecha: puntos ganados
        feedbackPtsLabel = new JLabel("+100 puntos");
        feedbackPtsLabel.setFont(fontSemi(19));
        feedbackPtsLabel.setForeground(C_GREEN);
        feedbackPtsLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        fp.add(left, BorderLayout.CENTER);
        fp.add(feedbackPtsLabel, BorderLayout.EAST);

        return fp;
    }

    // ========================================================
    //  PANTALLA: GAME OVER
    // ========================================================

    private JPanel createGameOverPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(C_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 28, 24, 28));

        // --- Header con trofeo ---
        JPanel header = new JPanel(new GridLayout(3, 1, 0, 4));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JLabel titleLbl = new JLabel(
                "<html><center>"
                        + "<span style='color:#F8FAFC'>¡Partida </span>"
                        + "<span style='color:#F97316'>finalizada!</span>"
                        + "</center></html>",
                SwingConstants.CENTER);
        titleLbl.setFont(fontTitle(26));
        header.add(titleLbl);

        JLabel subLbl = new JLabel("Resultado de la partida", SwingConstants.CENTER);
        subLbl.setFont(fontBody(14));
        subLbl.setForeground(C_MUTED);
        header.add(subLbl);

        panel.add(header, BorderLayout.NORTH);

        // --- Banner ganador ---
        RoundedPanel winnerBanner = new RoundedPanel(12, new Color(249, 115, 22, 20),
                new Color(249, 115, 22, 60), 1);
        winnerBanner.setLayout(new GridLayout(2, 1, 0, 2));
        winnerBanner.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        JLabel winnerTitle = new JLabel("GANADOR", SwingConstants.CENTER);
        winnerTitle.setFont(new Font(fontBody(11).getFamily(), Font.BOLD, 9));
        winnerTitle.setForeground(C_MUTED);
        winnerBanner.add(winnerTitle);

        winnerLabel = new JLabel("—", SwingConstants.CENTER);
        winnerLabel.setFont(fontTitle(21));
        winnerLabel.setForeground(C_ORANGE);
        winnerBanner.add(winnerLabel);

        winnerPtsLabel = new JLabel("", SwingConstants.CENTER);
        winnerPtsLabel.setFont(fontBody(14));
        winnerPtsLabel.setForeground(C_MUTED);

        // Envolvemos banner + pts en un panel vertical
        JPanel topScore = new JPanel(new BorderLayout(0, 6));
        topScore.setOpaque(false);
        topScore.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        topScore.add(winnerBanner, BorderLayout.CENTER);
        topScore.add(winnerPtsLabel, BorderLayout.SOUTH);

        // --- Sección de marcador ---
        JPanel scoreSection = new JPanel(new BorderLayout(0, 8));
        scoreSection.setOpaque(false);

        JLabel sectionLbl = sectionTitle("MARCADOR FINAL");
        scoreSection.add(sectionLbl, BorderLayout.NORTH);

        scoreboardPanel = new JPanel();
        scoreboardPanel.setLayout(new BoxLayout(scoreboardPanel, BoxLayout.Y_AXIS));
        scoreboardPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(scoreboardPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scoreSection.add(scroll, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setOpaque(false);
        center.add(topScore, BorderLayout.NORTH);
        center.add(scoreSection, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        // --- Botón volver ---
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        ModernButton backBtn = new ModernButton("VOLVER AL LOBBY", C_CARD2, C_TEXT, 10);
        backBtn.setFont(fontSemi(15));
        backBtn.setPreferredSize(new Dimension(0, 42));
        backBtn.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(C_BORDER, 10, 1),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        backBtn.addActionListener(e -> showPanel("LOBBY"));
        bottom.add(backBtn, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ========================================================
    //  ACTUALIZACIÓN DE PANTALLAS (lógica sin cambios)
    // ========================================================

    /**
     * Muestra una nueva pregunta en pantalla.
     */
    private void showQuestion(String num, String total, String text,
                              String optA, String optB, String optC, String optD) {

        int n = Integer.parseInt(num);
        int t = Integer.parseInt(total);
        totalQuestions = t;
        timerCircle.setMax(15);   // TIME_PER_QUESTION por defecto

        questionNumLabel.setText("Pregunta " + num + " de " + total);
        currentQuestion = n;
        totalQuestions = t;
        if (questionDotsPanel != null) questionDotsPanel.repaint();
        questionTextLabel.setText("<html><center>" + text + "</center></html>");

        String[] options = {optA, optB, optC, optD};
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setText(options[i]);
            answerButtons[i].setEnabled(true);
        }

        feedbackIconLabel.setText("");
        feedbackSubLabel.setText("");
        feedbackPtsLabel.setText("");
        canAnswer = true;
    }

    /**
     * Actualiza el temporizador circular.
     */
    private void updateTimer(String seconds) {
        int t = Integer.parseInt(seconds);
        timerCircle.update(t);
    }

    /**
     * Envía la respuesta del jugador al servidor.
     */
    private void submitAnswer(String letter) {
        if (!canAnswer) return;
        canAnswer = false;

        for (JButton btn : answerButtons) {
            btn.setEnabled(false);
            // Resaltar visualmente el botón seleccionado
            String btnLetter = (String) btn.getClientProperty("letter");
            if (letter.equals(btnLetter)) {
                btn.putClientProperty("bgAlpha",
                        new Color(148, 163, 184, 40));
                btn.repaint();
            }
        }
        out.println("ANSWER|" + letter);
    }

    /**
     * Muestra el resultado de la pregunta con feedback visual.
     */
    private void showResult(String correctLetter, String myAnswer,
                            String resultado, String totalScore) {

        boolean correct = "CORRECTO".equals(resultado);

        if (correct) {
            feedbackIconLabel.setText("¡Correcto!");
            feedbackIconLabel.setForeground(C_GREEN);
            feedbackPtsLabel.setText("+100 puntos");
            feedbackPtsLabel.setForeground(C_GREEN);
        } else {
            feedbackIconLabel.setText("Incorrecto");
            feedbackIconLabel.setForeground(C_RED);
            feedbackPtsLabel.setText("+0 puntos");
            feedbackPtsLabel.setForeground(C_MUTED);
        }
        feedbackSubLabel.setText("Respuesta correcta: " + correctLetter
                + "  ·  Tu respuesta: " + myAnswer);

        // Resaltar botón correcto en verde, botón incorrecto en rojo tenue
        for (JButton btn : answerButtons) {
            String bl = (String) btn.getClientProperty("letter");
            if (correctLetter.equals(bl)) {
                btn.putClientProperty("bgAlpha", new Color(34, 197, 94, 40));
                btn.putClientProperty("borderColor", C_GREEN);
            } else if (!correct && myAnswer.equals(bl)) {
                btn.putClientProperty("bgAlpha", new Color(239, 68, 68, 40));
                btn.putClientProperty("borderColor", C_RED);
            }
            btn.repaint();
        }

        currentScore = Integer.parseInt(totalScore);
        scoreLabel.setText("Puntaje: " + currentScore);
    }

    /**
     * Muestra la pantalla de fin de partida con tarjetas individuales por jugador.
     */
    private void showGameOver(String scoreData) {
        showPanel("GAMEOVER");
        scoreboardPanel.removeAll();

        String[] entries = scoreData.split(",");
        boolean first = true;
        int position = 1;

        for (String entry : entries) {
            String[] pair = entry.split(":");
            if (pair.length == 2) {
                String name = pair[0];
                String pts = pair[1];

                if (first) {
                    winnerLabel.setText(name);
                    winnerPtsLabel.setText(pts + " puntos totales");
                    first = false;
                }

                // Fila del marcador
                JPanel row = buildScoreRow(position, name, pts, position - 1);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
                scoreboardPanel.add(row);
                scoreboardPanel.add(Box.createRigidArea(new Dimension(0, 6)));
                position++;
            }
        }
        scoreboardPanel.revalidate();
        scoreboardPanel.repaint();
    }

    /**
     * Construye una fila del marcador final.
     */
    private JPanel buildScoreRow(int pos, String name, String pts, int colorIdx) {
        Color rowBg = (pos == 1) ? new Color(250, 204, 21, 20) : C_CARD;
        Color rowBorder = (pos == 1) ? new Color(250, 204, 21, 60) : C_BORDER;

        RoundedPanel row = new RoundedPanel(10, rowBg, rowBorder, 1);
        row.setLayout(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 14));

        // Badge de posición
        JPanel posBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill;
                Color fg;
                switch (pos) {
                    case 1:
                        fill = C_YELLOW;
                        fg = new Color(0x0F172A);
                        break;
                    case 2:
                        fill = C_MUTED;
                        fg = new Color(0x0F172A);
                        break;
                    case 3:
                        fill = new Color(0xCD7F32);
                        fg = Color.WHITE;
                        break;
                    default:
                        fill = C_CARD2;
                        fg = C_MUTED;
                        break;
                }
                g2.setColor(fill);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setFont(fontSemi(13));
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                String t = String.valueOf(pos);
                g2.drawString(t,
                        (getWidth() - fm.stringWidth(t)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        posBadge.setOpaque(false);
        posBadge.setPreferredSize(new Dimension(28, 28));

        // Avatar
        JPanel avWrap = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = AVATAR_COLORS[colorIdx % AVATAR_COLORS.length];
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 40));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(base);
                g2.setFont(fontSemi(14));
                FontMetrics fm = g2.getFontMetrics();
                String initial = name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();
                g2.drawString(initial,
                        (getWidth() - fm.stringWidth(initial)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avWrap.setOpaque(false);
        avWrap.setPreferredSize(new Dimension(30, 30));

        // Nombre
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(fontBody(15));
        nameLbl.setForeground(C_TEXT);

        // Puntos
        Color ptsColor;
        switch (pos) {
            case 1:
                ptsColor = C_YELLOW;
                break;
            case 2:
                ptsColor = C_BLUE;
                break;
            case 3:
                ptsColor = C_PURPLE;
                break;
            default:
                ptsColor = C_MUTED;
                break;
        }
        JLabel ptsLbl = new JLabel(pts);
        ptsLbl.setFont(fontSemi(18));
        ptsLbl.setForeground(ptsColor);
        ptsLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        // Ensamblar
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(posBadge);
        left.add(avWrap);
        left.add(nameLbl);

        row.add(left, BorderLayout.WEST);
        row.add(ptsLbl, BorderLayout.EAST);
        return row;
    }

    /**
     * Actualiza el lobby con la lista de jugadores recibida del servidor.
     */
    private void updateLobbyPlayers(String[] names) {
        lobbyPlayersPanel.removeAll();
        for (int i = 0; i < names.length; i++) {
            boolean isMe = names[i].equals(playerName);
            PlayerCard card = new PlayerCard(names[i], isMe, i);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
            lobbyPlayersPanel.add(card);
            lobbyPlayersPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        lobbyCountLabel.setText(names.length + " jugador" + (names.length == 1 ? "" : "es") + " conectado" + (names.length == 1 ? "" : "s"));
        lobbyPlayersPanel.revalidate();
        lobbyPlayersPanel.repaint();
    }

    /**
     * Cambia a un panel específico del CardLayout.
     */
    private void showPanel(String name) {
        cardLayout.show(mainPanel, name);
    }

    // ========================================================
    //  CONEXIÓN DE RED (sin cambios en la lógica)
    // ========================================================

    private void connect() {
        String name = nameField.getText().trim();
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();

        // Validar nombre (obligatorio)
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, ingresa tu nombre para jugar.",
                    "Nombre requerido",
                    JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return;
        }

        if (name.contains("|") || name.contains(",") || name.contains(":")) {
            JOptionPane.showMessageDialog(this,
                    "El nombre no puede contener los caracteres: | , :",
                    "Nombre inválido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Valores por defecto si están vacíos
        if (ip.isEmpty()) {
            ip = "localhost";
            SwingUtilities.invokeLater(() -> {
                ipField.setText("localhost");
                ipField.setForeground(C_TEXT);
            });
        }

        if (portStr.isEmpty()) {
            portStr = "5000";
            SwingUtilities.invokeLater(() -> {
                portField.setText("5000");
                portField.setForeground(C_TEXT);
            });
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "El puerto debe ser un número válido.",
                    "Puerto inválido",
                    JOptionPane.WARNING_MESSAGE);
            portField.requestFocus();
            return;
        }

        playerName = name;
        connectButton.setEnabled(false);
        connectButton.setText("Conectando...");

        final String finalIp = ip;
        final int finalPort = port;

        new Thread(() -> {
            try {
                socket = new Socket(finalIp, finalPort);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                connected = true;
                out.println("JOIN|" + playerName);
                setTitle("TriviaNet — " + playerName);
                SwingUtilities.invokeLater(() -> showPanel("LOBBY"));
                startReaderThread();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "No se pudo conectar a " + finalIp + ":" + finalPort
                                    + "\n" + e.getMessage(),
                            "Error de conexión",
                            JOptionPane.ERROR_MESSAGE);
                    connectButton.setEnabled(true);
                    connectButton.setText("CONECTAR");
                });
            }
        }).start();
    }

    private void startReaderThread() {
        Thread reader = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    processServerMessage(line);
                }
            } catch (IOException e) {
                if (connected) {
                    connected = false;
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                "Se perdió la conexión con el servidor.");
                        showPanel("LOGIN");
                        connectButton.setEnabled(true);
                        connectButton.setText("CONECTAR");
                    });
                }
            }
        }, "ReaderThread");
        reader.setDaemon(true);
        reader.start();
    }

    // ========================================================
    //  PROCESAMIENTO DE MENSAJES (protocolo sin cambios)
    // ========================================================

    private void processServerMessage(String msg) {
        String[] parts = msg.split("\\|");
        String command = parts[0];

        SwingUtilities.invokeLater(() -> {
            switch (command) {

                case "WELCOME":
                    lobbyStatusLabel.setText("Conectado. Esperando que inicie la partida...");
                    break;

                case "PLAYERLIST":
                    if (parts.length > 1) {
                        String[] names = parts[1].split(",");
                        updateLobbyPlayers(names);
                    }
                    break;

                case "START":
                    currentScore = 0;
                    showPanel("GAME");
                    feedbackIconLabel.setText("");
                    feedbackSubLabel.setText("");
                    feedbackPtsLabel.setText("");
                    scoreLabel.setText("Puntaje: 0");
                    break;

                case "QUESTION":
                    if (parts.length >= 8) {
                        showQuestion(parts[1], parts[2], parts[3],
                                parts[4], parts[5], parts[6], parts[7]);
                    }
                    break;

                case "TIMER":
                    if (parts.length > 1) {
                        updateTimer(parts[1]);
                    }
                    break;

                case "RESULT":
                    if (parts.length >= 5) {
                        showResult(parts[1], parts[2], parts[3], parts[4]);
                    }
                    break;

                case "SCOREBOARD":
                    // Se procesa en GAMEOVER
                    break;

                case "GAMEOVER":
                    if (parts.length > 1) {
                        showGameOver(parts[1]);
                    }
                    break;

                case "MSG":
                    if (parts.length > 1) {
                        JOptionPane.showMessageDialog(this, parts[1]);
                    }
                    break;

                default:
                    break;
            }
        });
    }

    // ========================================================
    //  MAIN
    // ========================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TriviaClient client = new TriviaClient();
            client.setVisible(true);
        });
    }

    // ========================================================
    //  SCROLLBAR PERSONALIZADA (igual que TriviaServer)
    // ========================================================

    static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {

        private static final Color THUMB_COLOR       = new Color(0x475569);
        private static final Color THUMB_HOVER_COLOR = new Color(0x64748B);
        private static final Color TRACK_COLOR       = new Color(0x1E293B);

        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = THUMB_COLOR;
            this.trackColor = TRACK_COLOR;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }

        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0x1E293B, true));
            int trackWidth = 4;
            int x = trackBounds.x + (trackBounds.width - trackWidth) / 2;
            g2.fillRoundRect(x, trackBounds.y + 4, trackWidth, trackBounds.height - 8, trackWidth, trackWidth);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = isThumbRollover() ? THUMB_HOVER_COLOR : THUMB_COLOR;
            g2.setColor(color);
            int thumbWidth = 4;
            int x = thumbBounds.x + (thumbBounds.width - thumbWidth) / 2;
            g2.fillRoundRect(x, thumbBounds.y + 2, thumbWidth, thumbBounds.height - 4, 6, 6);
            g2.dispose();
        }
    }
}