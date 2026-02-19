import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class GifApiManager {
    private final String apiKey = "6lVYTEPOPTRBsEBsfYtBA2o3qta3LPGt";
    private final Map<String, String> defaultParams = Map.of(
            "trending", "https://api.giphy.com/v1/gifs/trending",
            "search", "https://api.giphy.com/v1/gifs/search",
            "rating", "g"
    );
}

public class GifViewer {

    public static void main(String[] args) {

            JFrame frame = new JFrame("GIF Viewer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        new ImageLoader(panel).execute();
            JScrollPane scroll = new JScrollPane(panel);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            frame.add(scroll);

            frame.setSize(600, 800);
            frame.setVisible(true);



    }

    static class ImageLoader extends SwingWorker<Void, JLabel> {
        private final JPanel panel;

        ImageLoader(JPanel panel) {
            this.panel = panel;
        }

        @Override
        protected Void doInBackground() throws Exception {
            String api = "https://api.giphy.com/v1/gifs/search?api_key=6lVYTEPOPTRBsEBsfYtBA2o3qta3LPGt&q=sex&limit=25&offset=0&rating=r&lang=en&bundle=messaging_non_clips";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(api)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            for (JsonNode item : root.get("data")) {
                String gifUrl = item.get("images").get("original").get("url").asText();

                System.out.println(gifUrl);
                // non-blocking (worker thread)
                ImageIcon icon = new ImageIcon(new URL(gifUrl));
                JLabel label = new JLabel(icon);

                publish(label); // send to EDT for display
            }

            return null;
        }

        @Override
        protected void process(List<JLabel> labels) {
            for (JLabel label : labels) {
                panel.add(label);
            }
            panel.revalidate();
            panel.repaint();
        }
    }
}
