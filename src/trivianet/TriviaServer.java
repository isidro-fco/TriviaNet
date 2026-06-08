/* UI REDESIGN PACKAGE
Objetivo: aplicar fondo con gradientes, glassmorphism, glow,
sombras, timer premium y dashboard moderno sin alterar la lógica.
*/

package trivianet;

import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

/**
 * Servidor principal de TriviaNet — Rediseño visual 2025.
 * <p>
 * Temas demostrados:
 * - Swing: interfaz gráfica del servidor
 * - Threads: hilo aceptador de conexiones + hilo del juego
 * - Networking: ServerSocket para escuchar conexiones
 * - Collections Framework: CopyOnWriteArrayList, ArrayList, etc.
 * - Java I/O: lectura de preguntas desde archivo
 * <p>
 * CAMBIOS VISUALES (sin modificar lógica ni protocolo):
 * - Paleta oscura coherente con TriviaClient
 * - Panel de configuración como tarjeta redondeada
 * - Lista de jugadores con tarjetas individuales y avatares
 * - Log con fondo oscuro, scrollbar invisible, fuente monoespaciada
 * - Botones "Iniciar Servidor" / "Iniciar Partida" con estilo ModernButton
 * - StatusBar inferior con indicador de color dinámico
 */
public class TriviaServer extends JFrame {

    // ========== Constantes del juego ==========
    public static final int DEFAULT_PORT = 5000;
    public static final int TIME_PER_QUESTION = 15;
    public static final int POINTS_CORRECT = 100;
    private static final int MAX_QUESTIONS = 10;
    private static final String QUESTIONS_FILE = "preguntas.txt";

    // ========== Paleta (reutiliza la del cliente) ==========
    static final Color C_BG = TriviaClient.C_BG;
    static final Color C_CARD = TriviaClient.C_CARD;
    static final Color C_CARD2 = TriviaClient.C_CARD2;
    static final Color C_BORDER = TriviaClient.C_BORDER;
    static final Color C_ORANGE = TriviaClient.C_ORANGE;
    static final Color C_BLUE = TriviaClient.C_BLUE;
    static final Color C_GREEN = TriviaClient.C_GREEN;
    static final Color C_RED = TriviaClient.C_RED;
    static final Color C_YELLOW = TriviaClient.C_YELLOW;
    static final Color C_TEXT = TriviaClient.C_TEXT;
    static final Color C_MUTED = TriviaClient.C_MUTED;
    static final Color C_DISABLED = TriviaClient.C_DISABLED;

    // ========== Networking ==========
    private ServerSocket serverSocket;
    private boolean serverRunning;

    // ========== Jugadores (thread-safe) ==========
    private CopyOnWriteArrayList<ClientHandler> players;

    // ========== Preguntas ==========
    private List<Question> questions;

    // ========== Estado del juego ==========
    private boolean gameStarted;
    private volatile boolean allAnsweredFlag;

    // ========== Componentes GUI ==========
    private JTextArea logArea;
    private JPanel playerCardsPanel;   // antes JList
    private JButton startButton;
    private JButton stopButton;
    private JButton listenButton;
    private JLabel statusDot;
    private JLabel statusLabel;
    private JTextField portField;
    private JLabel ipValueLabel;

    // ========================================================
    //  CONSTRUCTOR
    // ========================================================

    public TriviaServer() {
        players = new CopyOnWriteArrayList<>();
        gameStarted = false;
        allAnsweredFlag = false;
        initGUI();
    }

    // ========================================================
    //  CONSTRUCCIÓN DE LA GUI
    // ========================================================

    private void initGUI() {
        setTitle("TriviaNet — Servidor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(600, 460));
        getContentPane().setBackground(C_BG);

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        setLayout(new BorderLayout(0, 0));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    // ---- Panel superior: configuración del servidor ----
    private JPanel buildTopPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(C_BG);
        outer.setBorder(BorderFactory.createEmptyBorder(18, 20, 10, 20));

        TriviaClient.RoundedPanel card =
                new TriviaClient.RoundedPanel(12, C_CARD, C_BORDER, 1);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // Lado izquierdo: logo + título
        JLabel logoText = new JLabel(
                "<html><span style='color:#F8FAFC'>Trivia</span>"
                        + "<span style='color:#F97316'>Net</span>"
                        + "<span style='color:#94A3B8; font-size:10px;'> Servidor</span></html>"
        );

        logoText.setFont(TriviaClient.fontTitle(21));

        card.add(logoText, BorderLayout.WEST);

        // Centro: IP + Puerto
        JPanel configRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        configRow.setOpaque(false);

        String localIP;
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            localIP = "127.0.0.1";
        }

        configRow.add(infoChip("IP", localIP));
        configRow.add(infoChip("Puerto", ""));

        // Campo de puerto editable
        portField = new TriviaClient.DarkField(String.valueOf(DEFAULT_PORT), 5);
        portField.setFont(TriviaClient.fontBody(14));
        portField.setPreferredSize(new Dimension(70, 45));
        configRow.add(portField);

        card.add(configRow, BorderLayout.CENTER);

        // Lado derecho: botones Iniciar / Detener
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        listenButton = new TriviaClient.ModernButton("Iniciar servidor", C_ORANGE, Color.WHITE, 8);
        listenButton.setFont(TriviaClient.fontSemi(14));
        listenButton.setPreferredSize(new Dimension(138, 34));
        listenButton.addActionListener(e -> startServer());
        btnRow.add(listenButton);

        stopButton = new TriviaClient.ModernButton("Detener", C_CARD2, C_TEXT, 8);
        stopButton.setFont(TriviaClient.fontBody(14));
        stopButton.setPreferredSize(new Dimension(82, 34));
        stopButton.setBorder(new TriviaClient.RoundBorder(C_BORDER, 8, 1));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopServer());
        btnRow.add(stopButton);

        card.add(btnRow, BorderLayout.EAST);
        outer.add(card);
        return outer;
    }

    /**
     * Crea una etiqueta "clave: valor" con estilo chip.
     */
    private JPanel infoChip(String key, String value) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        chip.setOpaque(false);
        JLabel k = new JLabel(key + ":");
        k.setFont(TriviaClient.fontBody(13));
        k.setForeground(C_MUTED);
        chip.add(k);
        if (!value.isEmpty()) {
            ipValueLabel = new JLabel(value);
            ipValueLabel.setFont(TriviaClient.fontSemi(13));
            ipValueLabel.setForeground(C_TEXT);
            chip.add(ipValueLabel);
        }
        return chip;
    }

    // ---- Panel central: jugadores (izquierda) + log (derecha) ----
    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(C_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        // ---- Columna izquierda: jugadores ----
        JPanel leftCol = new JPanel(new BorderLayout(0, 8));
        leftCol.setOpaque(false);
        leftCol.setPreferredSize(new Dimension(200, 0));

        JLabel playersTitle = sectionLabel("JUGADORES");
        leftCol.add(playersTitle, BorderLayout.NORTH);

        playerCardsPanel = new JPanel();
        playerCardsPanel.setLayout(new BoxLayout(playerCardsPanel, BoxLayout.Y_AXIS));
        playerCardsPanel.setOpaque(false);

        JScrollPane pScroll = new JScrollPane(playerCardsPanel);
        pScroll.setOpaque(false);
        pScroll.getViewport().setOpaque(false);
        pScroll.setBorder(null);

        // Scrollbar vertical personalizado
        pScroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        pScroll.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
        pScroll.getVerticalScrollBar().setBackground(C_BG);
        pScroll.getVerticalScrollBar().setOpaque(false);
        pScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftCol.add(pScroll, BorderLayout.CENTER);

        panel.add(leftCol, BorderLayout.WEST);

        // ---- Columna derecha: log ----
        JPanel rightCol = new JPanel(new BorderLayout(0, 8));
        rightCol.setOpaque(false);

        JLabel logTitle = sectionLabel("REGISTRO DE EVENTOS");
        rightCol.add(logTitle, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(TriviaClient.fontMono(13));
        logArea.setBackground(C_CARD);
        logArea.setForeground(new Color(0x94A3B8));
        logArea.setCaretColor(C_TEXT);
        logArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane lScroll = new JScrollPane(logArea);
        lScroll.setOpaque(false);
        lScroll.setBorder(new TriviaClient.RoundBorder(C_BORDER, 10, 1));
        lScroll.getViewport().setBackground(C_CARD);

        // Scrollbar vertical personalizado
        lScroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        lScroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        lScroll.getVerticalScrollBar().setBackground(C_CARD);
        lScroll.getVerticalScrollBar().setOpaque(false);
        lScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rightCol.add(lScroll, BorderLayout.CENTER);

        panel.add(rightCol, BorderLayout.CENTER);
        return panel;
    }

    // ---- Panel inferior: botón iniciar partida + status ----
    private JPanel buildBottomPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(C_BG);
        outer.setBorder(BorderFactory.createEmptyBorder(0, 20, 18, 20));

        TriviaClient.RoundedPanel bar =
                new TriviaClient.RoundedPanel(10, C_CARD, C_BORDER, 1);
        bar.setLayout(new BorderLayout(14, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        // Indicador de estado
        JPanel statusSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusSection.setOpaque(false);

        statusDot = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getForeground());
                g2.fillOval(0, (getHeight() - 10) / 2, 10, 10);
                g2.dispose();
            }
        };
        statusDot.setPreferredSize(new Dimension(12, 20));
        statusDot.setForeground(C_RED);
        statusSection.add(statusDot);

        statusLabel = new JLabel("Servidor detenido");
        statusLabel.setFont(TriviaClient.fontBody(14));
        statusLabel.setForeground(C_MUTED);
        statusSection.add(statusLabel);

        bar.add(statusSection, BorderLayout.WEST);

        // Botón "Iniciar Partida"
        startButton = new TriviaClient.ModernButton("Iniciar Partida", C_GREEN, Color.WHITE, 8);
        startButton.setFont(TriviaClient.fontSemi(15));
        startButton.setPreferredSize(new Dimension(160, 38));
        startButton.setEnabled(false);
        startButton.addActionListener(e -> startGame());
        bar.add(startButton, BorderLayout.EAST);

        outer.add(bar);
        return outer;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(TriviaClient.fontBody(11).getFamily(), Font.BOLD, 9));
        lbl.setForeground(C_MUTED);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return lbl;
    }

    // ========================================================
    //  ACTUALIZACIÓN VISUAL DE JUGADORES
    // ========================================================

    /**
     * Reconstruye las tarjetas de jugador en la columna izquierda.
     */
    private void updatePlayerCards() {
        playerCardsPanel.removeAll();
        int idx = 0;
        for (ClientHandler p : players) {
            JPanel card = buildPlayerCard(p.getPlayerName(), p.getScore(), idx++);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            playerCardsPanel.add(card);
            playerCardsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        playerCardsPanel.revalidate();
        playerCardsPanel.repaint();
    }

    private JPanel buildPlayerCard(String name, int score, int colorIdx) {
        TriviaClient.RoundedPanel card =
                new TriviaClient.RoundedPanel(9, C_CARD, C_BORDER, 1);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 12));

        // Avatar
        JPanel av = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int padding = 3; // Ajusta este valor (2-5) según qué tan pequeño quieras el círculo

                // Calcular el tamaño del círculo basado en la dimensión más pequeña
                int size = Math.min(getWidth(), getHeight()) - (padding * 2);
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                Color base = TriviaClient.AVATAR_COLORS[colorIdx % TriviaClient.AVATAR_COLORS.length];

                // Círculo de fondo
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 40));
                g2.fillOval(x, y, size, size);

                // Borde del círculo
                g2.setColor(base);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x, y, size, size);

                // Letra inicial
                g2.setFont(TriviaClient.fontSemi(13));
                FontMetrics fm = g2.getFontMetrics();
                String init = name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();
                g2.drawString(init,
                        (getWidth() - fm.stringWidth(init)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);

                g2.dispose();
            }
        };

        av.setOpaque(false);
        av.setPreferredSize(new Dimension(28, 28));

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(TriviaClient.fontBody(14));
        nameLbl.setForeground(C_TEXT);

        JLabel ptsLbl = new JLabel(score + " pts");
        ptsLbl.setFont(TriviaClient.fontSemi(13));
        ptsLbl.setForeground(C_YELLOW);
        ptsLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        card.add(av, BorderLayout.WEST);
        card.add(nameLbl, BorderLayout.CENTER);
        card.add(ptsLbl, BorderLayout.EAST);
        return card;
    }

    // ========================================================
    //  LÓGICA DEL SERVIDOR (sin cambios)
    // ========================================================

    private void startServer() {
        String portText = portField.getText().trim();
        int resolvedPort;
        if (portText.isEmpty()) {
            resolvedPort = DEFAULT_PORT;
        } else {
            try {
                resolvedPort = Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Puerto inválido.");
                return;
            }
        }
        final int port = resolvedPort;

        try {
            questions = QuestionLoader.loadFromFile(QUESTIONS_FILE);
            if (questions.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No se encontraron preguntas en " + QUESTIONS_FILE);
                return;
            }
            log("Se cargaron " + questions.size() + " preguntas.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar preguntas: " + e.getMessage()
                            + "\nAsegúrate de que el archivo '" + QUESTIONS_FILE
                            + "' existe en el directorio del proyecto.");
            return;
        }

        serverRunning = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Servidor iniciado en puerto " + port);
                updateStatus("Esperando jugadores...", C_YELLOW);

                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                    stopButton.setEnabled(true);
                    listenButton.setEnabled(false);
                    portField.setEnabled(false);
                });

                while (serverRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        log("Nueva conexión desde: "
                                + clientSocket.getInetAddress().getHostAddress());
                        ClientHandler handler = new ClientHandler(clientSocket, this);
                        Thread t = new Thread(handler);
                        t.setDaemon(true);
                        t.start();
                    } catch (IOException e) {
                        if (serverRunning) log("Error aceptando conexión: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                log("Error iniciando servidor: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "No se pudo iniciar el servidor.\n" + e.getMessage());
            }
        }, "AcceptorThread").start();
    }

    private void stopServer() {
        serverRunning = false;
        gameStarted = false;

        for (ClientHandler p : players) {
            p.sendMessage("MSG|El servidor se ha detenido.");
            p.disconnect();
        }
        players.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) { /* Ignorar */ }

        SwingUtilities.invokeLater(() -> {
            updatePlayerCards();
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            listenButton.setEnabled(true);
            portField.setEnabled(true);
        });

        log("Servidor detenido.");
        updateStatus("Servidor detenido", C_RED);
    }

    // ========================================================
    //  GESTIÓN DE JUGADORES
    // ========================================================

    public void registerPlayer(ClientHandler player) {
        players.add(player);
        player.sendMessage("WELCOME|" + player.getPlayerName());
        log("Jugador registrado: " + player.getPlayerName());
        updatePlayerList();
        broadcastPlayerList();
        updateStatus("Jugadores: " + players.size(), C_GREEN);
    }

    public void removePlayer(ClientHandler player) {
        if (players.remove(player)) {
            log("Jugador desconectado: " + player.getPlayerName());
            updatePlayerList();
            broadcastPlayerList();
        }
    }

    private void updatePlayerList() {
        SwingUtilities.invokeLater(this::updatePlayerCards);
    }

    private void broadcastPlayerList() {
        StringBuilder sb = new StringBuilder("PLAYERLIST|");
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(players.get(i).getPlayerName());
        }
        broadcast(sb.toString());
    }

    // ========================================================
    //  LÓGICA DEL JUEGO (sin cambios)
    // ========================================================

    private void startGame() {
        if (players.size() < 1) {
            JOptionPane.showMessageDialog(this,
                    "Se necesita al menos 1 jugador para iniciar.");
            return;
        }
        if (gameStarted) {
            JOptionPane.showMessageDialog(this, "Ya hay una partida en curso.");
            return;
        }

        gameStarted = true;
        startButton.setEnabled(false);
        log("=== PARTIDA INICIADA ===");
        updateStatus("Partida en curso", C_ORANGE);

        for (ClientHandler p : players) {
            p.addScore(-p.getScore());
            p.resetForNextQuestion();
        }

        new Thread(() -> {
            List<Question> gameQuestions = new ArrayList<>(questions);
            Collections.shuffle(gameQuestions);
            int totalQ = Math.min(gameQuestions.size(), MAX_QUESTIONS);

            broadcast("START|" + totalQ);
            log("Enviando " + totalQ + " preguntas...");
            sleep(2000);

            for (int i = 0; i < totalQ && gameStarted; i++) {
                Question q = gameQuestions.get(i);
                allAnsweredFlag = false;

                for (ClientHandler p : players) {
                    p.resetForNextQuestion();
                }

                String qMsg = String.format("QUESTION|%d|%d|%s|%s|%s|%s|%s",
                        i + 1, totalQ, q.getText(),
                        q.getOptions().get(0), q.getOptions().get(1),
                        q.getOptions().get(2), q.getOptions().get(3));
                broadcast(qMsg);
                log("Pregunta " + (i + 1) + "/" + totalQ + ": " + q.getText());

                for (int t = TIME_PER_QUESTION; t >= 0 && !allAnsweredFlag; t--) {
                    broadcast("TIMER|" + t);
                    if (t > 0 && !allAnsweredFlag) sleep(1000);
                }

                String correctLetter = q.getCorrectLetter();
                for (ClientHandler p : players) {
                    String answer = p.getCurrentAnswer();
                    boolean correct = correctLetter.equals(answer);
                    if (correct) p.addScore(POINTS_CORRECT);
                    p.sendMessage(String.format("RESULT|%s|%s|%s|%d",
                            correctLetter,
                            answer != null ? answer : "-",
                            correct ? "CORRECTO" : "INCORRECTO",
                            p.getScore()));
                }

                log("Respuesta correcta: " + correctLetter + " (" + q.getCorrectAnswer() + ")");
                broadcast("SCOREBOARD|" + buildScoreboard());
                updatePlayerList();
                sleep(4000);
            }

            broadcast("GAMEOVER|" + buildScoreboard());
            log("=== PARTIDA FINALIZADA ===");
            log("Marcador final: " + buildScoreboard());

            gameStarted = false;
            SwingUtilities.invokeLater(() -> {
                startButton.setEnabled(true);
                updateStatus("Partida finalizada — esperando jugadores", C_YELLOW);
            });

        }, "GameThread").start();
    }

    public synchronized void checkAllAnswered() {
        for (ClientHandler p : players) {
            if (!p.hasAnswered()) return;
        }
        allAnsweredFlag = true;
        log("Todos los jugadores respondieron.");
    }

    private String buildScoreboard() {
        List<ClientHandler> sorted = new ArrayList<>(players);
        sorted.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(sorted.get(i).getPlayerName())
                    .append(":").append(sorted.get(i).getScore());
        }
        return sb.toString();
    }

    // ========================================================
    //  UTILIDADES
    // ========================================================

    private void broadcast(String msg) {
        for (ClientHandler p : players) p.sendMessage(msg);
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[LOG] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updateStatus(String text, Color dotColor) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusDot.setForeground(dotColor);
            statusDot.repaint();
        });
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========================================================
//  SCROLLBAR PERSONALIZADO
// ========================================================

    /**
     * Scrollbar moderno y minimalista que se integra con el tema oscuro.
     * Sin botones, con thumb redondeado y track transparente.
     */
    private static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {

        private static final Color THUMB_COLOR = new Color(0x475569);
        private static final Color THUMB_HOVER_COLOR = new Color(0x64748B);
        private static final Color TRACK_COLOR = new Color(0x1E293B);

        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = THUMB_COLOR;
            this.trackColor = TRACK_COLOR;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

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

            // Track semi-transparente
            g2.setColor(new Color(0x1E293B, true));
            int trackWidth = 4; // Ancho del track
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

            // Thumb redondeado
            g2.fillRoundRect(x, thumbBounds.y + 2, thumbWidth, thumbBounds.height - 4, 6, 6);

            g2.dispose();
        }
    }

    // ========================================================
    //  MAIN
    // ========================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TriviaServer server = new TriviaServer();
            server.setVisible(true);
        });
    }
}