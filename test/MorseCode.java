import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MorseCode {
    private static final Map<String, String> codeToCharacter = Map.<String, String>ofEntries(
            Map.entry(".-", "A"),
            Map.entry("-...", "B"),
            Map.entry("-.-.", "C"),
            Map.entry("-..", "D"),
            Map.entry(".", "E"),
            Map.entry("..-.", "F"),
            Map.entry("--.", "G"),
            Map.entry("....", "H"),
            Map.entry("..", "I"),
            Map.entry(".---", "J"),
            Map.entry("-.-", "K"),
            Map.entry(".-..", "L"),
            Map.entry("--", "M"),
            Map.entry("-.", "N"),
            Map.entry("---", "O"),
            Map.entry(".--.", "P"),
            Map.entry("--.-", "Q"),
            Map.entry(".-.", "R"),
            Map.entry("...", "S"),
            Map.entry("-", "T"),
            Map.entry("..-", "U"),
            Map.entry("...-", "V"),
            Map.entry(".--", "W"),
            Map.entry("-..-", "X"),
            Map.entry("-.--", "Y"),
            Map.entry("--..", "Z"),

            Map.entry("-----", "0"),
            Map.entry(".----", "1"),
            Map.entry("..---", "2"),
            Map.entry("...--", "3"),
            Map.entry("....-", "4"),
            Map.entry(".....", "5"),
            Map.entry("-....", "6"),
            Map.entry("--...", "7"),
            Map.entry("---..", "8"),
            Map.entry("----.", "9"),

            Map.entry(".-.-.-", "."),
            Map.entry("--..--", ","),
            Map.entry("..--..", "?"),
            Map.entry(".----.", "'"),
            Map.entry("-.-.--", "!"),
            Map.entry("-..-.", "/"),
            Map.entry("-.--.", "("),
            Map.entry("-.--.-", ")"),
            Map.entry(".-...", "&"),
            Map.entry("---...", ":"),
            Map.entry("-.-.-.", ";"),
            Map.entry("-...-", "="),
            Map.entry(".-.-.", "+"),
            Map.entry("-....-", "-"),
            Map.entry("..--.-", "_"),
            Map.entry(".-..-.", "\""),
            Map.entry(".--.-.", "@")
    );

    private static final Map<String, String> characterToCode = Map.<String, String>ofEntries(
            Map.entry("B", "-..."),
            Map.entry("X", "-..-"),
            Map.entry("L", ".-.."),
            Map.entry("+", ".-.-."),
            Map.entry(".", ".-.-.-"),
            Map.entry("2", "..---"),
            Map.entry("Z", "--.."),
            Map.entry("Q", "--.-"),
            Map.entry("=", "-...-"),
            Map.entry("1", ".----"),
            Map.entry(";", "-.-.-."),
            Map.entry("S", "..."),
            Map.entry("U", "..-"),
            Map.entry("!", "-.-.--"),
            Map.entry("6", "-...."),
            Map.entry("I", ".."),
            Map.entry("A", ".-"),
            Map.entry("7", "--..."),
            Map.entry("D", "-.."),
            Map.entry("K", "-.-"),
            Map.entry("F", "..-."),
            Map.entry("\"", ".-..-."),
            Map.entry("8", "---.."),
            Map.entry("@", ".--.-."),
            Map.entry("C", "-.-."),
            Map.entry("Y", "-.--"),
            Map.entry("P", ".--."),
            Map.entry("J", ".---"),
            Map.entry(":", "---..."),
            Map.entry("'", ".----."),
            Map.entry("R", ".-."),
            Map.entry("W", ".--"),
            Map.entry("/", "-..-."),
            Map.entry("N", "-."),
            Map.entry("M", "--"),
            Map.entry("G", "--."),
            Map.entry("O", "---"),
            Map.entry("(", "-.--."),
            Map.entry("9", "----."),
            Map.entry("0", "-----"),
            Map.entry("_", "..--.-"),
            Map.entry(",", "--..--"),
            Map.entry("?", "..--.."),
            Map.entry("E", "."),
            Map.entry("T", "-"),
            Map.entry("5", "....."),
            Map.entry("4", "....-"),
            Map.entry("&", ".-..."),
            Map.entry("-", "-....-"),
            Map.entry(")", "-.--.-"),
            Map.entry("H", "...."),
            Map.entry("V", "...-"),
            Map.entry("3", "...--")
    );

    public static void main(String[] args) {
        String sample = "hello world";
        StringBuilder morseBuilder = new StringBuilder();
        for(char c : sample.toUpperCase().toCharArray()){
            if (c == ' '){
                morseBuilder.append(" / ");
                continue;
            }

            String code = characterToCode.get(String.valueOf(c));
            morseBuilder.append(code).append(" ");
        }

        String text = morseBuilder.toString();
        String[] codes = text.split(" / ");
        StringBuilder builder = new StringBuilder();

        for (String s : codes) {
            String code = s.trim();
            String[] characters = code.split(" ");
            for (String character: characters){
                String word = codeToCharacter.get(character).trim();
                builder.append(word);
            }
            builder.append(" ");
        }

        System.out.println(builder);
    }
}
