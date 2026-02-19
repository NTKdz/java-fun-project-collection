import com.google.gson.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

public class NovelScraper {

    public static void main(String[] args) throws IOException {
        String bookId = "7215574730256944188";
        String chapterListLink = "https://sangtacviet.app/index.php?ngmar=chapterlist&h=fanqie&bookid="
                + bookId
                + "&sajax=getchapterlist";
        int endChapter = 1;

        Connection.Response response = Jsoup.connect(chapterListLink)
                .ignoreContentType(true)
                .method(Connection.Method.GET)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://sangtacviet.app/truyen/fanqie/1/" + bookId + "/")
                .header("Accept", "*/*")
                .header("X-Requested-With", "XMLHttpRequest")
                .execute();

        String body = response.body();

        // Parse JSON
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();

        if (root.get("code").getAsInt() != 1) {
            System.out.println("API returned error.");
            return;
        }

        String data = root.get("data").getAsString();

        // Split chapters
        String[] chapters = data.split("-//-");
        List<List<String>> chapterList = new ArrayList<>();
        for (String chapter : chapters) {

            String[] parts = chapter.split("-/-");

            if (parts.length >= 3) {
                String chapterId = parts[1].trim();
                String chapterName = parts[2].trim();

//                System.out.println("ID: " + chapterId);
//                System.out.println("Name: " + chapterName);
//                System.out.println("--------------------------------");
                chapterList.add(Arrays.asList(chapterId, chapterName));
            }
        }
        chapterList.forEach(chapter -> {
            String id = chapter.get(0);
            String name = chapter.get(1);

//            System.out.println(id);
//            System.out.println(name);
        });

        for (int i = 0; i < endChapter; i++) {
            List<String> chapterInfo = chapterList.get(i);
            String chapterContentLink = "https://sangtacviet.app/truyen/fanqie/1/" +
                    bookId
                    + "/" + chapterInfo.getFirst() + "/";
            String link =
                    "https://sangtacviet.app/index.php?bookid=7215574730256944188&h=fanqie&c=7215574803431096832&ngmar=readc&sajax=readchapter&sty=1&exts=";
            System.out.println(chapterContentLink);
            Document doc = Jsoup.connect(chapterContentLink).get();
            System.out.println(doc.getElementsByClass("contentbox"));
        }
    }
}
