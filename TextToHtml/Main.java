import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class Main {
    public static void main(String[] args) throws IOException {
        String text = "<* Hello World >\nHello World\nfdfsdf";
        System.out.println(textToHtml(text, "p"));
    }

    public static String textToHtml(String text, String defaultSymbol) throws IOException {
        StringBuilder html = new StringBuilder();
        BufferedReader buffer = new BufferedReader(new StringReader(text));
        String string;
        while ((string = buffer.readLine()) != null) {
            String symbol = getSymbols(string);
            if (symbol == null) symbol = defaultSymbol;
            else {
                string = string.substring(symbol.length() + 3);
                symbol = symbol.trim();
            }
            html.append("<").append(symbol).append(">").append(string).append("</").append(symbol).append(">").append("\n");
        }

        return html.toString();
    }

    public static String getSymbols(String text) {
        StringBuilder symbols = new StringBuilder();
        int i = 2;
        if (text.startsWith("<*")) {
            while (i < text.length() && text.charAt(i) != '>') {
                symbols.append(text.charAt(i));
                i++;
            }
        } else return null;
        System.out.println(symbols);
        return symbols.toString();
    }
}
