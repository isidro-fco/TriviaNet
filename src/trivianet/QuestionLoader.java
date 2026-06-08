package trivianet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga preguntas desde un archivo de texto delimitado por pipes (|).
 * Formato: pregunta|opcionA|opcionB|opcionC|opcionD|indiceCorrecta
 * 
 * Demuestra el uso de Java I/O: BufferedReader, FileReader.
 */
public class QuestionLoader {

    /**
     * Lee las preguntas desde un archivo de texto.
     * Las líneas vacías o que empiecen con # se ignoran.
     *
     * @param path ruta al archivo de preguntas
     * @return lista de objetos Question
     * @throws IOException si hay error al leer el archivo
     */
    public static List<Question> loadFromFile(String path) throws IOException {
        List<Question> questions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();

                // Ignorar líneas vacías y comentarios
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");

                if (parts.length != 6) {
                    System.err.println("Advertencia: línea " + lineNum
                            + " tiene formato incorrecto, se omitirá.");
                    continue;
                }

                try {
                    String text = parts[0].trim();
                    List<String> options = new ArrayList<>();
                    options.add(parts[1].trim()); // A
                    options.add(parts[2].trim()); // B
                    options.add(parts[3].trim()); // C
                    options.add(parts[4].trim()); // D
                    int correctIndex = Integer.parseInt(parts[5].trim());

                    if (correctIndex < 0 || correctIndex > 3) {
                        System.err.println("Advertencia: línea " + lineNum
                                + " tiene índice fuera de rango (0-3), se omitirá.");
                        continue;
                    }

                    questions.add(new Question(text, options, correctIndex));
                } catch (NumberFormatException e) {
                    System.err.println("Advertencia: línea " + lineNum
                            + " tiene índice no numérico, se omitirá.");
                }
            }
        }

        System.out.println("Se cargaron " + questions.size() + " preguntas.");
        return questions;
    }
}
