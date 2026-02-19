package com.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.icu.ICUNormalizer2Filter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.*;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Core Lucene + Tika indexing and search engine.
 *
 * ── Partial / fuzzy filename matching ──────────────────────────────────────
 *
 * Problem: searching "rep" did not find "report.docx" because the standard
 * analyzer tokenizes to exact whole words ["report","docx"] — you had to type
 * the complete token "report" to get a match.
 *
 * Solution: dual-field filename indexing:
 *
 *   F_FILENAME        (existing) — standard tokenizer + lowercase.
 *                      "My Report Final.docx" -> ["my","report","final","docx"]
 *                      Good for whole-word matches and relevance scoring.
 *
 *   F_FILENAME_NGRAM  (new)      — edge n-gram tokenizer, min=1 max=20.
 *                      Each token from the standard tokenizer is additionally
 *                      expanded into all its leading prefixes:
 *                        "report" -> ["r","re","rep","repo","repor","report"]
 *                        "docx"   -> ["d","do","doc","docx"]
 *                      So typing "rep", "repo", "repor" all match "report.docx".
 *
 *   F_FILENAME_NGRAM also uses NGramTokenFilter (not just Edge) so substrings
 *   in the middle are also found: "por" matches "report", "epo" matches "report".
 *   This is controlled by NGRAM_MIN/NGRAM_MAX constants below.
 *
 * At search time, the query is run against BOTH fields. F_FILENAME gets a
 * higher boost (exact word match scores higher than a partial match), but
 * F_FILENAME_NGRAM ensures partial typing works.
 *
 * The n-gram field is NOT used for content — it would explode the index size.
 * Content search remains whole-word only (use wildcards like "rep*" if needed).
 *
 * ── Other fixes carried forward ────────────────────────────────────────────
 * - Files with unreadable content are still indexed by filename (FIX 1)
 * - F_PATH_INDEXED StringField for correct updateDocument/delete (FIX 2+3)
 * - ICUNormalizer2Filter for Vietnamese NFC/NFD (FIX 4)
 * - Query NFC normalization (FIX 5)
 * - QueryParser escape fallback (FIX 6)
 */
public class SearchEngine implements Closeable {

    // ---- Lucene field names ----
    public static final String F_PATH         = "path";
    public static final String F_PATH_INDEXED = "path_idx";
    public static final String F_CONTENT      = "content";
    public static final String F_FILENAME     = "filename";
    public static final String F_FILENAME_NGRAM = "filename_ngram"; // partial-match field
    public static final String F_SIZE         = "size";
    public static final String F_MODIFIED     = "modified";
    public static final String F_TYPE         = "filetype";

    // N-gram range: min=1 means single-char prefixes work ("t" finds "test.txt").
    // max=20 covers filenames up to 20 chars as a single n-gram token.
    // Larger max = larger index but more flexible matching.
    private static final int NGRAM_MIN = 1;
    private static final int NGRAM_MAX = 20;

    // ---- Result record ----
    public record SearchResult(
            String path,
            String filename,
            String fileType,
            float  score,
            float  maxScore,
            long   size) {}

    /** Controls which fields are searched. */
    public enum SearchMode {
        ALL(
                "Filename + Content",
                new String[]{"filename", "filename_ngram", "content", "filetype"},
                Map.of("filename", 4.0f, "filename_ngram", 2.0f, "content", 1.0f, "filetype", 0.5f)
        ),
        FILENAME_ONLY(
                "Filename Only",
                new String[]{"filename", "filename_ngram", "filetype"},
                Map.of("filename", 4.0f, "filename_ngram", 2.0f, "filetype", 0.5f)
        ),
        CONTENT_ONLY(
                "Content Only",
                new String[]{"content"},
                Map.of("content", 1.0f)
        );

        public final String             label;
        final        String[]           fields;
        final        Map<String, Float> boosts;

        SearchMode(String label, String[] fields, Map<String, Float> boosts) {
            this.label  = label;
            this.fields = fields;
            this.boosts = boosts;
        }
    }

    // ---- MIME types that contain no searchable text ----
    private static final Set<String> SKIP_MIME_PREFIXES = Set.of("image/", "video/", "audio/");
    private static final Set<String> SKIP_MIME_EXACT = Set.of(
            "application/x-font-ttf", "application/x-font-otf",
            "application/x-font-woff", "application/x-font-woff2",
            "application/x-executable", "application/x-sharedlib",
            "application/x-dex", "application/octet-stream"
    );

    private static final int MAX_CHARS_PER_FILE = 500_000;

    private final ConfigManager    config;
    private final Analyzer         indexAnalyzer;  // used at index time (has n-gram)
    private final Analyzer         searchAnalyzer; // used at search time (NO n-gram — we want exact query tokens)
    private final AutoDetectParser tikaParser;
    private       Directory        directory;
    private       DirectoryReader  reader;
    private       IndexSearcher    searcher;

    public SearchEngine(ConfigManager config) throws Exception {
        this.config         = config;
        this.indexAnalyzer  = buildIndexAnalyzer();
        this.searchAnalyzer = buildSearchAnalyzer();
        this.tikaParser     = new AutoDetectParser(TikaConfig.getDefaultConfig());
        openIndex();
    }

    /**
     * Index-time analyzer.
     *
     * Uses PerFieldAnalyzerWrapper so:
     *   - F_FILENAME_NGRAM gets the n-gram analyzer (for partial filename matching)
     *   - F_CONTENT and F_FILENAME get the dual-token analyzer (diacritic + ASCII fold)
     *
     * The dual-token trick for Vietnamese:
     *   "hà nội" is indexed as BOTH ["hà","nội"] AND ["ha","noi"] in the same field.
     *   This means searching either form finds the document:
     *     - User types "hà nội" (with diacritics)  → matches ["hà","nội"] tokens ✓
     *     - User types "ha noi" (without diacritics) → matches ["ha","noi"] tokens ✓
     *   Without this, "ha noi" would never find a document containing "hà nội".
     *
     * How dual-token works technically:
     *   After LowerCaseFilter we have e.g. "hà". We then pass through ASCIIFoldingFilter
     *   with preserveOriginal=true. This emits TWO tokens at the same position:
     *     position 1: "hà"   (original, diacritics preserved)
     *     position 1: "ha"   (ASCII-folded copy, diacritics stripped)
     *   Both get stored in the index. At search time a single-token query "ha" matches
     *   the "ha" token; a query "hà" matches the "hà" token. Either works.
     */
    private Analyzer buildIndexAnalyzer() {
        Analyzer dualTokenBase = buildDualTokenAnalyzer();

        // N-gram analyzer for the filename_ngram field only.
        // Also uses dual-token so partial ASCII typing finds Vietnamese filenames.
        Analyzer ngramAnalyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                StandardTokenizer tokenizer = new StandardTokenizer();
                TokenStream stream = new ICUNormalizer2Filter(tokenizer);
                stream = new LowerCaseFilter(stream);
                // preserveOriginal=true: emit both "hà" and "ha" before n-gram expansion
                stream = new ASCIIFoldingFilter(stream, true);
                stream = new NGramTokenFilter(stream, NGRAM_MIN, NGRAM_MAX, true);
                return new TokenStreamComponents(tokenizer, stream);
            }
        };

        Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
        fieldAnalyzers.put(F_FILENAME_NGRAM, ngramAnalyzer);

        return new PerFieldAnalyzerWrapper(dualTokenBase, fieldAnalyzers);
    }

    /**
     * Search-time analyzer: standard pipeline, NO n-gram, NO dual-token.
     *
     * At search time we want "ha noi" → ["ha", "noi"] (two exact tokens).
     * These then match either:
     *   - The ASCII-folded tokens in the index ("ha","noi") if the document had diacritics
     *   - Or the exact tokens if the document had no diacritics
     *
     * We also strip diacritics at search time via ASCIIFoldingFilter (preserveOriginal=true)
     * so typing "hà nội" at search also emits ["hà","nội","ha","noi"] and matches both
     * document forms.
     */
    private Analyzer buildSearchAnalyzer() {
        return buildDualTokenAnalyzer();
    }

    /**
     * Core pipeline used at both index and search time for all text fields.
     *
     * Pipeline:
     *   1. StandardTokenizer      — Unicode word-boundary splitting.
     *                               "hà nội Q1-2024" → ["hà","nội","Q1","2024"]
     *   2. ICUNormalizer2Filter   — NFC normalize (macOS NFD → NFC, Vietnamese diacritics fix)
     *   3. LowerCaseFilter        — Unicode-aware lowercase. "Hà"→"hà", "TEST"→"test"
     *   4. ASCIIFoldingFilter     — preserveOriginal=TRUE (this is the key setting).
     *                               For each token with diacritics, emits TWO tokens
     *                               at the same position: original + ASCII-stripped copy.
     *                               "hà" → "hà" (pos 1) + "ha" (pos 1)
     *                               "nội" → "nội" (pos 2) + "noi" (pos 2)
     *                               "report" → "report" only (no diacritics, no extra token)
     *   5. StopFilter             — English stop words only.
     *
     * Result: searching "ha noi" OR "hà nội" both find files containing either form.
     */
    private static Analyzer buildDualTokenAnalyzer() {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                StandardTokenizer tokenizer = new StandardTokenizer();
                TokenStream stream = new ICUNormalizer2Filter(tokenizer);
                stream = new LowerCaseFilter(stream);
                // preserveOriginal=true: emit original diacritic token AND ASCII-folded token
                // at the same position. This doubles the tokens for Vietnamese text but
                // index size increase is modest because content tokens are deduplicated.
                stream = new ASCIIFoldingFilter(stream, true);
                stream = new StopFilter(stream, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
                return new TokenStreamComponents(tokenizer, stream);
            }
        };
    }

    private void openIndex() throws IOException {
        Path indexPath = Paths.get(config.get(ConfigManager.KEY_INDEX_DIR));
        Files.createDirectories(indexPath);
        directory = FSDirectory.open(indexPath);
        if (DirectoryReader.indexExists(directory)) {
            reader   = DirectoryReader.open(directory);
            searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());
        }
    }

    // ========== INDEXING ==========

    public void indexAll(BiConsumer<Long, Long> onProgress,
                         Consumer<String>       onStatus,
                         AtomicBoolean          cancelFlag) throws Exception {

        List<String> rootFolders = config.getList(ConfigManager.KEY_ROOT_FOLDERS);
        List<String> skipFolders = config.getList(ConfigManager.KEY_SKIP_FOLDERS);

        onStatus.accept("Counting files in all folders...");
        AtomicLong total = countAll(rootFolders, skipFolders, cancelFlag);
        if (cancelFlag.get()) { onStatus.accept("Cancelled."); return; }

        onStatus.accept("Full re-index - " + total.get() + " files found.");
        onProgress.accept(0L, total.get());

        Path indexPath = Paths.get(config.get(ConfigManager.KEY_INDEX_DIR));
        Files.createDirectories(indexPath);
        closeReaderQuietly();

        // IMPORTANT: use indexAnalyzer (has n-gram) for writing
        IndexWriterConfig cfg = new IndexWriterConfig(indexAnalyzer);
        cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        cfg.setSimilarity(new BM25Similarity());

        try (IndexWriter writer = new IndexWriter(FSDirectory.open(indexPath), cfg)) {
            runWorkerPool(rootFolders, skipFolders, writer, false,
                    total.get(), onProgress, onStatus, cancelFlag);
            if (!cancelFlag.get()) {
                onStatus.accept("Optimising index...");
                writer.forceMerge(1);
            }
        }

        openReaderOnNewIndex(indexPath);
    }

    public void indexFolders(List<String>          folders,
                             BiConsumer<Long, Long> onProgress,
                             Consumer<String>       onStatus,
                             AtomicBoolean          cancelFlag) throws Exception {

        List<String> skipFolders = config.getList(ConfigManager.KEY_SKIP_FOLDERS);

        onStatus.accept("Counting files in selected folder(s)...");
        AtomicLong total = countAll(folders, skipFolders, cancelFlag);
        if (cancelFlag.get()) { onStatus.accept("Cancelled."); return; }

        onStatus.accept("Update - " + total.get() + " files to add/refresh.");
        onProgress.accept(0L, total.get());

        Path indexPath = Paths.get(config.get(ConfigManager.KEY_INDEX_DIR));
        Files.createDirectories(indexPath);
        closeReaderQuietly();

        IndexWriterConfig cfg = new IndexWriterConfig(indexAnalyzer);
        cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        cfg.setSimilarity(new BM25Similarity());

        try (IndexWriter writer = new IndexWriter(FSDirectory.open(indexPath), cfg)) {
            runWorkerPool(folders, skipFolders, writer, true,
                    total.get(), onProgress, onStatus, cancelFlag);
        }

        openReaderOnNewIndex(indexPath);
    }

    public int removeFolder(String folderPath, Consumer<String> onStatus) throws Exception {
        Path indexPath = Paths.get(config.get(ConfigManager.KEY_INDEX_DIR));
        if (!DirectoryReader.indexExists(FSDirectory.open(indexPath))) return 0;

        closeReaderQuietly();
        String prefix = Paths.get(folderPath).toString();

        IndexWriterConfig cfg = new IndexWriterConfig(indexAnalyzer);
        cfg.setOpenMode(IndexWriterConfig.OpenMode.APPEND);

        int deletedCount = 0;
        try (IndexWriter writer = new IndexWriter(FSDirectory.open(indexPath), cfg)) {
            List<String> toDelete = new ArrayList<>();
            try (DirectoryReader tempReader = DirectoryReader.open(FSDirectory.open(indexPath))) {
                for (int i = 0; i < tempReader.maxDoc(); i++) {
                    Document doc = tempReader.document(i);
                    String path = doc.get(F_PATH);
                    if (path != null && path.startsWith(prefix)) toDelete.add(path);
                }
            }
            for (String path : toDelete) {
                writer.deleteDocuments(new Term(F_PATH_INDEXED, path));
                deletedCount++;
            }
            onStatus.accept("Removed " + deletedCount + " documents from: " + folderPath);
        }

        openReaderOnNewIndex(indexPath);
        return deletedCount;
    }

    // ── Worker pool ─────────────────────────────────────────────────────────

    private void runWorkerPool(List<String>          folders,
                               List<String>          skipFolders,
                               IndexWriter           writer,
                               boolean               useUpdate,
                               long                  total,
                               BiConsumer<Long, Long> onProgress,
                               Consumer<String>       onStatus,
                               AtomicBoolean          cancelFlag) throws Exception {

        AtomicLong processed      = new AtomicLong(0);
        AtomicLong contentSkipped = new AtomicLong(0);
        AtomicLong errored        = new AtomicLong(0);

        int threads  = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        int queueCap = threads * 64;
        BlockingQueue<Path> queue = new ArrayBlockingQueue<>(queueCap);
        Path POISON  = Paths.get("__POISON__");

        onStatus.accept("Using " + threads + " thread(s) on "
                + Runtime.getRuntime().availableProcessors() + " CPU cores.");

        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                while (true) {
                    Path file;
                    try { file = queue.take(); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                    if (file == POISON) break;

                    try {
                        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                        TikaResult result = extractWithTika(file);

                        // Always index even if content extraction failed —
                        // the file must remain findable by filename.
                        if (result == null) {
                            result = filenameOnlyResult(file);
                            contentSkipped.incrementAndGet();
                        }

                        Document doc = buildDocument(file, attrs, result);
                        if (useUpdate) {
                            writer.updateDocument(new Term(F_PATH_INDEXED, file.toString()), doc);
                        } else {
                            writer.addDocument(doc);
                        }
                    } catch (Exception e) {
                        errored.incrementAndGet();
                    }

                    long done = processed.incrementAndGet();
                    onProgress.accept(done, total);
                    if (done % 200 == 0)
                        onStatus.accept("Processed " + done + " / " + total
                                + " files  [" + threads + " threads]...");
                }
            });
        }

        for (String root : folders) {
            if (cancelFlag.get()) break;
            Path rootPath = Paths.get(root);
            if (!Files.exists(rootPath)) { onStatus.accept("Not found, skipping: " + root); continue; }
            onStatus.accept("Walking: " + root);

            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes a) {
                    if (cancelFlag.get()) return FileVisitResult.TERMINATE;
                    return skipFolders.contains(dirName(dir))
                            ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes a) {
                    if (cancelFlag.get()) return FileVisitResult.TERMINATE;
                    try { queue.put(file); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); return FileVisitResult.TERMINATE; }
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFileFailed(Path file, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        for (int i = 0; i < threads; i++)
            try { queue.put(POISON); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        pool.shutdown();
        try {
            if (!pool.awaitTermination(24, TimeUnit.HOURS)) pool.shutdownNow();
        } catch (InterruptedException e) { pool.shutdownNow(); Thread.currentThread().interrupt(); }

        onStatus.accept((cancelFlag.get() ? "Cancelled - partial index saved.  " : "Complete!  ")
                + processed.get()      + " indexed, "
                + contentSkipped.get() + " filename-only, "
                + errored.get()        + " errors.");
    }

    /**
     * Builds a Lucene Document.
     *
     * The filename is indexed in TWO fields:
     *   F_FILENAME       — standard tokenizer, whole words only.
     *                      "My Report Final.docx" -> ["my","report","final","docx"]
     *   F_FILENAME_NGRAM — n-gram expansion, all substrings of each token.
     *                      "report" -> ["r","re","rep","repo","repor","report",
     *                                   "e","ep","epo","epor","eport", ...]
     *
     * At search time, the query runs against both. Typing "rep" matches the
     * indexed n-gram token "rep" in F_FILENAME_NGRAM and finds "report.docx".
     * Typing the full word "report" also matches F_FILENAME directly (higher score).
     */
    private Document buildDocument(Path file, BasicFileAttributes attrs, TikaResult result) {
        Document doc = new Document();
        String pathStr  = file.toString();
        String filename = file.getFileName().toString();

        doc.add(new StoredField(F_PATH,     pathStr));
        doc.add(new StoredField(F_SIZE,     attrs.size()));
        doc.add(new StoredField(F_MODIFIED, attrs.lastModifiedTime().toMillis()));
        doc.add(new StoredField(F_TYPE,     result.mimeShort()));

        // Indexed path for exact-term deduplication (updateDocument/delete)
        doc.add(new StringField(F_PATH_INDEXED, pathStr, Field.Store.NO));

        // Whole-word filename (stored for display)
        doc.add(new TextField(F_FILENAME, filename, Field.Store.YES));

        // N-gram filename (NOT stored — only used for matching, not display)
        // The PerFieldAnalyzerWrapper will apply the n-gram analyzer to this field.
        doc.add(new TextField(F_FILENAME_NGRAM, filename, Field.Store.NO));

        // File type as searchable text
        doc.add(new TextField(F_TYPE, result.mimeShort(), Field.Store.YES));

        // Content body (not stored, large)
        if (result.text() != null && !result.text().isBlank())
            doc.add(new TextField(F_CONTENT, result.text(), Field.Store.NO));

        if (result.metaText() != null && !result.metaText().isBlank())
            doc.add(new TextField(F_CONTENT, result.metaText(), Field.Store.NO));

        return doc;
    }

    /** Fallback when Tika cannot extract content — file still indexed for name search. */
    private TikaResult filenameOnlyResult(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = (dot >= 0 && dot < name.length() - 1)
                ? name.substring(dot + 1).toUpperCase()
                : "FILE";
        return new TikaResult(null, null, ext);
    }

    private AtomicLong countAll(List<String> roots, List<String> skipFolders,
                                AtomicBoolean cancelFlag) {
        AtomicLong counter = new AtomicLong(0);
        for (String root : roots) {
            Path p = Paths.get(root);
            if (Files.exists(p)) countFiles(p, skipFolders, counter, cancelFlag);
        }
        return counter;
    }

    private void countFiles(Path root, List<String> skipFolders,
                            AtomicLong counter, AtomicBoolean cancel) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes a) {
                    if (cancel.get()) return FileVisitResult.TERMINATE;
                    return skipFolders.contains(dirName(dir))
                            ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes a) {
                    counter.incrementAndGet(); return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFileFailed(Path file, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {}
    }

    // ========== TIKA EXTRACTION ==========

    private record TikaResult(String text, String metaText, String mimeShort) {}

    /**
     * Attempt Tika content extraction.
     * Returns null when the file is binary/media/unreadable.
     * Callers MUST handle null via filenameOnlyResult() -- never skip the file.
     *
     * Charset handling for Vietnamese text files:
     *   Vietnamese .txt/.log/.csv files in Vietnam are frequently saved in
     *   Windows-1258 (the Vietnamese Windows code page) rather than UTF-8.
     *   Without a charset hint, Tika sometimes mis-detects these as ISO-8859-1
     *   and produces mojibake ("hA  na\u1ed9i" instead of "ha\u0300 no\u1ed9i").
     *   We detect mojibake after extraction and retry with Windows-1258 if needed.
     */
    private TikaResult extractWithTika(Path file) {
        Metadata     metadata = new Metadata();
        ParseContext context  = new ParseContext();

        String filename = file.getFileName().toString();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
        // UTF-8 hint -- Tika charset detector will override if it detects otherwise
        metadata.set(Metadata.CONTENT_ENCODING, "UTF-8");

        BodyContentHandler handler = new BodyContentHandler(MAX_CHARS_PER_FILE);

        try (InputStream stream = new BufferedInputStream(Files.newInputStream(file))) {
            tikaParser.parse(stream, handler, metadata, context);

            String mime = metadata.get(Metadata.CONTENT_TYPE);
            if (mime == null) mime = "application/octet-stream";
            final String baseMime = mime.split(";")[0].trim();

            if (SKIP_MIME_PREFIXES.stream().anyMatch(baseMime::startsWith)) return null;
            if (SKIP_MIME_EXACT.contains(baseMime))                         return null;

            String text = handler.toString();

            // If extracted text looks like mojibake (high ratio of U+FFFD replacement
            // chars), charset detection failed -- retry with Windows-1258 then Latin-1
            if (isMojibake(text) && isPlainTextExtension(filename)) {
                String retry = retryWithCharset(file, "windows-1258");
                if (retry != null && !isMojibake(retry)) text = retry;
                else {
                    retry = retryWithCharset(file, "ISO-8859-1");
                    if (retry != null && !isMojibake(retry)) text = retry;
                }
            }

            return new TikaResult(text, buildMetaText(metadata), mimeToShort(baseMime));

        } catch (IOException | TikaException e) { return null; }
        catch (Exception e)                      { return null; }
    }

    /** Returns true if >2% of chars are Unicode replacement char -- sign of failed charset detection. */
    private boolean isMojibake(String text) {
        if (text == null || text.length() < 50) return false;
        long bad = text.chars().filter(c -> c == '\uFFFD').count();
        return (double) bad / text.length() > 0.02;
    }

    private boolean isPlainTextExtension(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".log") || lower.endsWith(".csv");
    }

    /** Re-parse a plain text file forcing a specific charset. */
    private String retryWithCharset(Path file, String charset) {
        try {
            Metadata m = new Metadata();
            m.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getFileName().toString());
            m.set(Metadata.CONTENT_ENCODING, charset);
            BodyContentHandler h = new BodyContentHandler(MAX_CHARS_PER_FILE);
            try (InputStream s = new BufferedInputStream(Files.newInputStream(file))) {
                tikaParser.parse(s, h, m, new ParseContext());
                return h.toString();
            }
        } catch (Exception e) { return null; }
    }

    private String buildMetaText(Metadata meta) {
        StringBuilder sb = new StringBuilder();
        for (String key : new String[]{
                TikaCoreProperties.TITLE.getName(), TikaCoreProperties.CREATOR.getName(),
                TikaCoreProperties.SUBJECT.getName(), TikaCoreProperties.DESCRIPTION.getName(),
                "keywords", "Keywords", "dc:subject", "Author", "producer", "Company"}) {
            String val = meta.get(key);
            if (val != null && !val.isBlank()) sb.append(val).append(' ');
        }
        return sb.toString().trim();
    }

    private String mimeToShort(String mime) {
        return switch (mime) {
            case "application/pdf"                                                          -> "PDF";
            case "application/msword"                                                       -> "DOC";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document"  -> "DOCX";
            case "application/vnd.ms-excel"                                                 -> "XLS";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"        -> "XLSX";
            case "application/vnd.ms-powerpoint"                                            -> "PPT";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation"-> "PPTX";
            case "application/vnd.oasis.opendocument.text"                                  -> "ODT";
            case "application/vnd.oasis.opendocument.spreadsheet"                           -> "ODS";
            case "application/vnd.oasis.opendocument.presentation"                          -> "ODP";
            case "application/rtf", "text/rtf"                                              -> "RTF";
            case "application/epub+zip"                                                     -> "EPUB";
            case "message/rfc822"                                                           -> "EML";
            case "application/vnd.ms-outlook"                                               -> "MSG";
            case "text/html", "application/xhtml+xml"                                       -> "HTML";
            case "text/xml", "application/xml"                                              -> "XML";
            case "application/json"                                                         -> "JSON";
            case "text/csv"                                                                 -> "CSV";
            case "text/plain"                                                               -> "TXT";
            case "application/x-ipynb+json"                                                 -> "IPYNB";
            case "application/vnd.ms-excel.sheet.binary.macroEnabled.12"                    -> "XLSB";
            case "application/vnd.ms-word.document.macroEnabled.12"                         -> "DOCM";
            case "application/vnd.wordperfect"                                              -> "WPD";
            case "text/x-java-source"                                                       -> "JAVA";
            case "text/x-python"                                                            -> "PY";
            case "application/x-sh"                                                         -> "SH";
            case "text/x-csrc", "text/x-chdr"                                              -> "C";
            case "text/x-c++src", "text/x-c++hdr"                                          -> "CPP";
            case "text/x-csharp"                                                            -> "CS";
            case "text/x-go"                                                                -> "GO";
            case "text/x-rust"                                                              -> "RS";
            default -> mime.contains("/")
                    ? mime.substring(mime.lastIndexOf('/') + 1).toUpperCase().replace("X-", "")
                    : "FILE";
        };
    }

    // ========== SEARCHING ==========

    public List<SearchResult> search(String queryText, SearchMode mode) throws Exception {
        if (searcher == null) return Collections.emptyList();

        int maxResults = config.getInt(ConfigManager.KEY_MAX_RESULTS, 20);

        // NFC-normalize query for Vietnamese compatibility
        String normalizedQuery = Normalizer.normalize(queryText.trim(), Normalizer.Form.NFC);

        // IMPORTANT: use searchAnalyzer (no n-gram) so "rep" stays as the single
        // token ["rep"] and matches the pre-indexed n-gram token "rep" in the index.
        // If we used indexAnalyzer here, "rep" would expand to ["r","re","rep"]
        // which is far too broad.
        MultiFieldQueryParser parser = new MultiFieldQueryParser(
                mode.fields, searchAnalyzer, mode.boosts);
        // AND operator: "hà nội" requires BOTH tokens present in document.
        // OR would match any doc containing just "hà" or just "nội" — too broad.
        // Users can still use OR explicitly: "hà OR saigon"
        parser.setDefaultOperator(QueryParser.Operator.AND);
        parser.setAllowLeadingWildcard(true);

        Query query;
        try {
            query = parser.parse(normalizedQuery);
        } catch (Exception e) {
            try {
                query = parser.parse(QueryParser.escape(normalizedQuery));
            } catch (Exception e2) {
                return Collections.emptyList();
            }
        }

        TopDocs topDocs = searcher.search(query, maxResults);
        if (topDocs.scoreDocs.length == 0) return Collections.emptyList();

        float maxScore = topDocs.scoreDocs[0].score;
        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc sd : topDocs.scoreDocs) {
            Document doc = searcher.doc(sd.doc);
            results.add(new SearchResult(
                    doc.get(F_PATH),
                    doc.get(F_FILENAME),
                    doc.get(F_TYPE),
                    sd.score, maxScore,
                    doc.getField(F_SIZE).numericValue().longValue()));
        }
        return results;
    }

    public int getIndexedDocCount() {
        return reader != null ? reader.numDocs() : 0;
    }

    // ========== HELPERS ==========

    private void openReaderOnNewIndex(Path indexPath) {
        try {
            closeReaderQuietly();
            Directory newDir = FSDirectory.open(indexPath);
            if (DirectoryReader.indexExists(newDir)) {
                if (directory != null) try { directory.close(); } catch (IOException ignored) {}
                directory = newDir;
                reader    = DirectoryReader.open(directory);
                searcher  = new IndexSearcher(reader);
                searcher.setSimilarity(new BM25Similarity());
            }
        } catch (IOException e) {
            System.err.println("Could not reopen reader: " + e.getMessage());
        }
    }

    private void closeReaderQuietly() {
        if (reader != null) {
            try { reader.close(); } catch (IOException ignored) {}
            reader   = null;
            searcher = null;
        }
    }

    private static String dirName(Path dir) {
        return dir.getFileName() != null ? dir.getFileName().toString() : "";
    }

    @Override
    public void close() {
        closeReaderQuietly();
        if (directory != null) try { directory.close(); } catch (IOException ignored) {}
        indexAnalyzer.close();
        searchAnalyzer.close();
    }
}