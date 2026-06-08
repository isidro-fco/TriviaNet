package trivianet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Hilo dedicado a manejar la comunicación con un cliente conectado.
 * Cada jugador tiene su propio ClientHandler corriendo en un Thread separado.
 * 
 * Networking (Socket).
 */
public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private TriviaServer server;

    private String playerName;
    private int score;
    private String currentAnswer;
    private boolean answered;
    private boolean connected;

    public ClientHandler(Socket socket, TriviaServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        this.score = 0;
        this.currentAnswer = null;
        this.answered = false;
        this.connected = true;
    }

    @Override
    public void run() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                processMessage(line);
            }
        } catch (IOException e) {
            // El cliente se desconectó
        } finally {
            disconnect();
        }
    }

    /**
     * Procesa un mensaje recibido del cliente.
     * Protocolo:
     *   JOIN|nombre   -> El jugador se une con un nombre
     *   ANSWER|letra  -> El jugador responde (A, B, C o D)
     */
    private void processMessage(String msg) {
        String[] parts = msg.split("\\|", 2);
        String command = parts[0];

        switch (command) {
            case "JOIN":
                if (parts.length > 1) {
                    this.playerName = parts[1].trim();
                    server.registerPlayer(this);
                }
                break;

            case "ANSWER":
                if (parts.length > 1 && !answered) {
                    this.currentAnswer = parts[1].trim().toUpperCase();
                    this.answered = true;
                    server.log(playerName + " respondió: " + currentAnswer);
                    server.checkAllAnswered();
                }
                break;

            default:
                server.log("Mensaje desconocido de " + playerName + ": " + msg);
                break;
        }
    }

    /** Envía un mensaje al cliente. */
    public void sendMessage(String msg) {
        if (connected) {
            out.println(msg);
        }
    }

    /** Desconecta al cliente. */
    public void disconnect() {
        connected = false;
        server.removePlayer(this);
        try {
            socket.close();
        } catch (IOException e) {
            // Ignorar
        }
    }

    // ========== Getters y setters ==========

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public String getCurrentAnswer() {
        return currentAnswer;
    }

    public boolean hasAnswered() {
        return answered;
    }

    public boolean isConnected() {
        return connected;
    }

    /** Reinicia el estado de respuesta para la siguiente pregunta. */
    public void resetForNextQuestion() {
        this.currentAnswer = null;
        this.answered = false;
    }
}
