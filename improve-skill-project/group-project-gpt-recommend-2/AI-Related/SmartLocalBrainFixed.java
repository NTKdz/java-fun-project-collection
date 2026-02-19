import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.qa.QAInput;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * FIXED VERSION - Better answer quality through:
 * - Increased search results (top 10 instead of 3)
 * - Better text cleaning and chunking
 * - Answer quality scoring
 * - Filtering out table/template content
 * - Better context extraction
 */
public class SmartLocalBrainFixed {
    private static final String INDEX_PATH = "D:\\dataset\\lucene_index";
    private static final String TXT_PATH = "D:\\dataset\\txt";
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int BATCH_SIZE = 1000;
    private static final int MAX_CHUNK_SIZE = 2000; // Smaller chunks = more focused
    private static final int TOP_DOCS = 10; // Check MORE documents for better answers

    private static final Map<String, CachedAnswer> answerCache = new ConcurrentHashMap<>();
    private static final int CACHE_SIZE = 100;

    static class CachedAnswer {
        String answer;
        String source;
        long timestamp;

        CachedAnswer(String answer, String source) {
            this.answer = answer;
            this.source = source;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        Path indexPath = Paths.get(INDEX_PATH);

        System.out.println("\nLoading AI model...");

        Criteria<QAInput, String> criteria = Criteria.builder()
                .setTypes(QAInput.class, String.class)
                .optApplication(Application.NLP.QUESTION_ANSWER)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/deepset/roberta-large-squad2")
                .optEngine("PyTorch")
                .optDevice(ai.djl.Device.gpu())
                .build();

        try (ZooModel<QAInput, String> model = criteria.loadModel();
             Predictor<QAInput, String> predictor = model.newPredictor();
             DirectoryReader reader = DirectoryReader.open(MMapDirectory.open(indexPath))) {

            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());

            StandardAnalyzer analyzer = new StandardAnalyzer();

            System.out.println("✓ Ready!\n");
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("Smart Local Brain - Improved Answer Quality");
            System.out.println("Type 'exit' to quit, 'stats' for statistics");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            int questionsAsked = 0;
            int cacheHits = 0;

            while (true) {
                System.out.print("❯ ");
                String question = scanner.nextLine().trim();

                if (question.isEmpty()) continue;
                if (question.equalsIgnoreCase("exit")) break;

                if (question.equalsIgnoreCase("stats")) {
                    showStatistics(reader, questionsAsked, cacheHits);
                    continue;
                }

                questionsAsked++;
                long queryStart = System.currentTimeMillis();

                // Check cache
                String cacheKey = question.toLowerCase().trim();
                if (answerCache.containsKey(cacheKey)) {
                    CachedAnswer cached = answerCache.get(cacheKey);
                    System.out.println("\n💨 [Cached] " + cached.answer);
                    System.out.println("└─ Source: " + cached.source + " (instant)");
                    cacheHits++;
                    continue;
                }

                // IMPROVED: Search more broadly
                MultiFieldQueryParser parser = new MultiFieldQueryParser(
                        new String[]{"content", "filename"},
                        analyzer,
                        Map.of("content", 1.0f, "filename", 3.0f) // Higher boost for title matches
                );

                Query query = parser.parse(QueryParser.escape(question));
                TopDocs docs = searcher.search(query, TOP_DOCS);

                if (docs.totalHits.value == 0) {
                    System.out.println("❌ No matching documents found.");
                    continue;
                }

                System.out.println("🔍 Analyzing top " + docs.scoreDocs.length + " documents...");

                List<ScoredAnswer> candidateAnswers = new ArrayList<>();

                // Process each document
                for (ScoreDoc scoreDoc : docs.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    String content = doc.get("content");
                    String fileName = doc.get("filename");

                    // CRITICAL FIX: Clean the content first!
                    String cleanedContent = cleanWikiContent(content);

                    // Skip if too little usable content
                    if (cleanedContent.length() < 200) continue;

                    // Extract best chunk
                    String relevantChunk = extractBestChunk(cleanedContent, question, MAX_CHUNK_SIZE);

                    try {
                        QAInput input = new QAInput(question, relevantChunk);
                        String answer = predictor.predict(input);

                        if (answer != null && !answer.trim().isEmpty()) {
                            // Score the answer quality
                            double qualityScore = scoreAnswerQuality(answer, question, scoreDoc.score);

                            candidateAnswers.add(new ScoredAnswer(
                                    answer,
                                    fileName,
                                    qualityScore,
                                    relevantChunk
                            ));
                        }
                    } catch (Exception e) {
                        // Continue to next document
                    }
                }

                long queryEnd = System.currentTimeMillis();

                // Find best answer
                if (!candidateAnswers.isEmpty()) {
                    // Sort by quality score (highest first)
                    candidateAnswers.sort((a, b) -> Double.compare(b.score, a.score));

                    ScoredAnswer best = candidateAnswers.get(0);

                    // Show top answer
                    System.out.println("\n✓ " + best.answer);
                    System.out.println("└─ Source: " + best.source + " (" + (queryEnd - queryStart) + "ms)");

                    // Show alternative answers if they're good
                    if (candidateAnswers.size() > 1 && candidateAnswers.get(1).score > 20) {
                        System.out.println("\n📚 Alternative answer:");
                        System.out.println("   " + candidateAnswers.get(1).answer);
                        System.out.println("   └─ From: " + candidateAnswers.get(1).source);
                    }

                    // Cache
                    if (answerCache.size() > CACHE_SIZE) {
                        answerCache.clear();
                    }
                    answerCache.put(cacheKey, new CachedAnswer(best.answer, best.source));
                } else {
                    System.out.println("❌ Could not find a good answer in the matching documents.");
                    System.out.println("💡 Try rephrasing your question or being more specific.");
                }
            }

            System.out.println("\n👋 Goodbye!");
        }
    }

    static class ScoredAnswer {
        String answer;
        String source;
        double score;
        String context;

        ScoredAnswer(String answer, String source, double score, String context) {
            this.answer = answer;
            this.source = source;
            this.score = score;
            this.context = context;
        }
    }

    /**
     * CRITICAL FIX: Clean Wikipedia formatting that confuses the AI
     */
    private static String cleanWikiContent(String content) {
        // Remove tables (major source of garbage answers!)
        content = content.replaceAll("(?s)\\{\\|.*?\\|\\}", "");

        // Remove template brackets
        content = content.replaceAll("\\{\\{[^}]*\\}\\}", "");

        // Remove image/file references
        content = content.replaceAll("\\[\\[File:.*?\\]\\]", "");
        content = content.replaceAll("\\[\\[Image:.*?\\]\\]", "");

        // Remove category links
        content = content.replaceAll("\\[\\[Category:.*?\\]\\]", "");

        // Clean up wiki links: [[Link|Text]] -> Text
        content = content.replaceAll("\\[\\[(?:[^|\\]]*\\|)?([^\\]]+)\\]\\]", "$1");

        // Remove reference tags
        content = content.replaceAll("<ref[^>]*>.*?</ref>", "");
        content = content.replaceAll("<ref[^>]*/>", "");

        // Remove HTML comments
        content = content.replaceAll("<!--.*?-->", "");

        // Remove remaining HTML tags
        content = content.replaceAll("<[^>]+>", "");

        // Clean up excessive whitespace
        content = content.replaceAll("\\s+", " ");
        content = content.replaceAll("\\n{3,}", "\n\n");

        return content.trim();
    }

    /**
     * Extract the chunk most likely to contain the answer
     */
    private static String extractBestChunk(String content, String question, int maxSize) {
        if (content.length() <= maxSize) {
            return content;
        }

        // Split into sentences
        String[] sentences = content.split("\\. ");

        // Extract question keywords
        String[] keywords = question.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .split("\\s+");

        // Find sentence with most keyword matches
        int bestSentenceIdx = 0;
        int bestScore = 0;

        for (int i = 0; i < sentences.length; i++) {
            String sentenceLower = sentences[i].toLowerCase();
            int score = 0;

            for (String keyword : keywords) {
                if (keyword.length() > 3 && sentenceLower.contains(keyword)) {
                    score += keyword.length();
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestSentenceIdx = i;
            }
        }

        // Build context around best sentence
        StringBuilder chunk = new StringBuilder();
        int currentSize = 0;

        // Add sentences around the best match
        int start = Math.max(0, bestSentenceIdx - 3);
        int end = Math.min(sentences.length, bestSentenceIdx + 4);

        for (int i = start; i < end && currentSize < maxSize; i++) {
            String sentence = sentences[i];
            if (currentSize + sentence.length() > maxSize) break;

            chunk.append(sentence);
            if (!sentence.endsWith(".")) chunk.append(".");
            chunk.append(" ");
            currentSize += sentence.length();
        }

        return chunk.toString().trim();
    }

    /**
     * Score answer quality to filter out garbage
     */
    private static double scoreAnswerQuality(String answer, String question, float searchScore) {
        double score = 0;

        // Base score from search ranking
        score += searchScore * 10;

        // Longer answers are generally better (but not too long)
        int length = answer.length();
        if (length >= 20 && length <= 300) {
            score += Math.min(length / 2.0, 50);
        } else if (length > 300) {
            score += 20; // Penalize overly long answers
        } else {
            score -= 20; // Penalize very short answers
        }

        // Check if answer looks like garbage (table content, etc.)
        if (answer.matches(".*[{|}\\[\\]].*")) {
            score -= 50; // Heavy penalty for wiki markup
        }

        // Penalize if answer contains special characters (likely table/template)
        long specialChars = answer.chars().filter(ch -> "!|={}[]".indexOf(ch) >= 0).count();
        score -= specialChars * 5;

        // Bonus if answer contains question keywords
        String answerLower = answer.toLowerCase();
        String[] keywords = question.toLowerCase().split("\\s+");
        for (String keyword : keywords) {
            if (keyword.length() > 3 && answerLower.contains(keyword)) {
                score += 5;
            }
        }

        // Bonus for proper sentence structure
        if (answer.matches("^[A-Z].*[.!?]$")) {
            score += 10;
        }

        // Penalize answers that are just numbers or single words
        if (answer.split("\\s+").length < 3) {
            score -= 15;
        }

        return score;
    }

    private static void showStatistics(DirectoryReader reader, int questions, int cacheHits) {
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("Statistics");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Documents indexed: " + String.format("%,d", reader.numDocs()));
        System.out.println("Questions asked: " + questions);
        System.out.println("Cache hits: " + cacheHits + " (" +
                (questions > 0 ? (cacheHits * 100 / questions) : 0) + "%)");
        System.out.println("Cache size: " + answerCache.size() + " / " + CACHE_SIZE);
        System.out.println("═══════════════════════════════════════════════════════════\n");
    }

    // Indexing methods (same as before, just cleaner content)
    private static void buildIndexParallel() throws IOException, InterruptedException, ExecutionException {
        FSDirectory dir = MMapDirectory.open(Paths.get(INDEX_PATH));
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());

        config.setRAMBufferSizeMB(512);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        config.setUseCompoundFile(false);

        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger totalFiles = new AtomicInteger(0);

        try (IndexWriter writer = new IndexWriter(dir, config)) {
            System.out.println("📁 Scanning files...");
            List<Path> allFiles = Files.walk(Paths.get(TXT_PATH))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .collect(Collectors.toList());

            totalFiles.set(allFiles.size());
            System.out.println("📊 Found " + String.format("%,d", totalFiles.get()) + " files\n");

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            List<Future<List<Document>>> futures = new ArrayList<>();

            int batchCount = 0;
            for (int i = 0; i < allFiles.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allFiles.size());
                List<Path> batch = allFiles.subList(i, end);

                final int batchNum = ++batchCount;
                futures.add(executor.submit(() -> processBatch(batch, processedCount, totalFiles, batchNum)));
            }

            for (Future<List<Document>> future : futures) {
                List<Document> docs = future.get();
                if (!docs.isEmpty()) {
                    writer.addDocuments(docs);
                }
            }

            System.out.println("\n💾 Committing index...");
            writer.commit();

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);

            System.out.println("✓ Indexing complete! Indexed: " + String.format("%,d", processedCount.get()) + " documents");
        }
    }

    private static List<Document> processBatch(List<Path> paths,
                                               AtomicInteger processedCount,
                                               AtomicInteger totalFiles,
                                               int batchNum) {
        List<Document> documents = new ArrayList<>();

        for (Path path : paths) {
            try {
                String content = Files.readString(path);

                if (content.length() < 100) continue;

                // IMPORTANT: Store cleaned content for better search
                String cleanedContent = cleanWikiContent(content);

                Document doc = new Document();
                doc.add(new TextField("content", cleanedContent, Field.Store.YES));

                String cleanName = path.getFileName().toString().replace(".txt", "").replace("_", " ");
                doc.add(new TextField("filename", cleanName, Field.Store.YES));

                doc.add(new StringField("path", path.toString(), Field.Store.YES));
                doc.add(new IntPoint("length", cleanedContent.length()));
                doc.add(new StoredField("length", cleanedContent.length()));

                documents.add(doc);

                int count = processedCount.incrementAndGet();
                if (count % 1000 == 0) {
                    int percent = (count * 100) / totalFiles.get();
                    System.out.println(String.format("⚡ Progress: %,d / %,d (%d%%) - Batch #%d",
                            count, totalFiles.get(), percent, batchNum));
                }

            } catch (IOException e) {
                System.err.println("⚠ Error reading " + path.getFileName());
            }
        }

        return documents;
    }
}