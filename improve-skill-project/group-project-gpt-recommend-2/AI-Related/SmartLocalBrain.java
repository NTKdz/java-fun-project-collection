import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.qa.QAInput;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class SmartLocalBrain {
    private static final String INDEX_PATH = "D:\\dataset\\lucene_index";
    private static final String TXT_PATH = "D:\\dataset\\txt";

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // STEP 1: INDEXING (Run once or if files change)
        Path path = Paths.get(INDEX_PATH);
//        if (!Files.exists(path)) {
//            System.out.println("Building search index... this might take a few minutes.");
//            buildIndex();
//        }

        // STEP 2: SETUP AI MODEL
        Criteria<QAInput, String> criteria = Criteria.builder()
                .setTypes(QAInput.class, String.class)
                .optApplication(Application.NLP.QUESTION_ANSWER)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/deepset/roberta-large-squad2")
                .optEngine("PyTorch")
                .build();

        try (ZooModel<QAInput, String> model = criteria.loadModel();
             Predictor<QAInput, String> predictor = model.newPredictor();
             DirectoryReader reader = DirectoryReader.open(FSDirectory.open(path))) {

            IndexSearcher searcher = new IndexSearcher(reader);
            StandardAnalyzer analyzer = new StandardAnalyzer();

            while (true) {
                System.out.print("\nAsk (or 'exit'): ");
                String question = scanner.nextLine();
                if (question.equalsIgnoreCase("exit")) break;

                // STEP 3: INSTANT KEYWORD SEARCH
                // We search for the top 5 most relevant documents
                Query query = new QueryParser("content", analyzer).parse(QueryParser.escape(question));
                TopDocs docs = searcher.search(query, 5);

                if (docs.totalHits.value == 0) {
                    System.out.println("No matching documents found in index.");
                    continue;
                }

                System.out.println("Found " + docs.totalHits.value + " potential matches. Analyzing with AI...");

                for (ScoreDoc scoreDoc : docs.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    String content = doc.get("content");
                    String fileName = doc.get("filename");

                    // STEP 4: AI EXTRACTS ANSWER FROM ONLY THE RELEVANT TEXT
                    QAInput input = new QAInput(question, content);
                    String answer = predictor.predict(input);

                    if (!answer.trim().isEmpty()) {
                        System.out.println("\n[Match: " + fileName + "]");
                        System.out.println("ANSWER: " + answer);
                        break; // Stop after the first good answer
                    }
                }
            }
        }
    }

    private static void buildIndex() throws IOException {
        FSDirectory dir = FSDirectory.open(Paths.get(INDEX_PATH));
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        try (IndexWriter writer = new IndexWriter(dir, config)) {
            Files.walk(Paths.get(TXT_PATH))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Document doc = new Document();
                            doc.add(new TextField("content", Files.readString(path), Field.Store.YES));
                            doc.add(new StringField("filename", path.getFileName().toString(), Field.Store.YES));
                            writer.addDocument(doc);
                        } catch (IOException e) { e.printStackTrace(); }
                    });
            writer.commit();
        }
    }
}