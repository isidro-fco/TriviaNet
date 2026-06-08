package trivianet;

import java.util.List;

/**
 * Modelo que representa una pregunta de trivia.
 * Contiene el texto, las 4 opciones de respuesta y el índice de la correcta.
 */
public class Question {

    private String text;
    private List<String> options; // 4 opciones (A, B, C, D)
    private int correctIndex;    // 0=A, 1=B, 2=C, 3=D

    public Question(String text, List<String> options, int correctIndex) {
        this.text = text;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    public String getText() {
        return text;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    /** Retorna la letra de la respuesta correcta (A, B, C o D). */
    public String getCorrectLetter() {
        return String.valueOf((char) ('A' + correctIndex));
    }

    /** Retorna el texto de la respuesta correcta. */
    public String getCorrectAnswer() {
        return options.get(correctIndex);
    }

    @Override
    public String toString() {
        return text + " [Respuesta: " + getCorrectLetter() + "]";
    }
}
