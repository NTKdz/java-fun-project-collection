import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class Highlighter {
    private static final String[] color = {
            "red",
            "green",
            "blue",
            "yellow",
            "purple",
            "orange",
            "cyan",
            "magenta",
            "lime",
            "pink",
            "teal",
            "lavender",
            "brown",
            "beige",
            "maroon",
            "mint",
            "olive",
            "coral",
            "navy",
            "grey"
    };

    public static String getColor(String word) {
        int hash = Math.abs(word.hashCode());
        return color[hash % color.length];
    }

    public static void main(String[] args) {
        Path codePath = Path.of("code.txt");
        try (BufferedReader reader = Files.newBufferedReader(codePath)) {
            String line;
            StringBuilder highlightedCode = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    System.out.println("[EMPTY LINE / GAP]");
                    highlightedCode.append("\n"); // preserve the gap
                    continue;
                }

                String[] words = line.split("(?<=\\s)|(?=\\s)");
                for (String word : words) {
                    if (word.equals(" ")) {
                        highlightedCode.append(" ");
                        continue;
                    }
                    System.out.println(word + ": " + getColor(word));
                    highlightedCode.append(getColor(word)).append(".").append(word.length());
                }
                highlightedCode.append("\n");
            }
            System.out.println("Highlighted Code: \n" + highlightedCode.toString());

            BufferedWriter writer = Files.newBufferedWriter(Path.of("highlighted_code.txt"));
            writer.write(highlightedCode.toString());
            writer.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
