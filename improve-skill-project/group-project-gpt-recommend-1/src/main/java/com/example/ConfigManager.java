package com.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Reads and writes config.properties.
 * All settings live here — UI and engine both use this class.
 *
 * FIX: Uses UTF-8 InputStreamReader/OutputStreamWriter instead of the default
 * ISO-8859-1 that java.util.Properties uses, which was silently corrupting
 * Vietnamese folder paths and any non-ASCII characters in config values.
 */
public class ConfigManager {

    public static final Path CONFIG_DIR  = Paths.get(System.getProperty("user.home"), ".filesearch");
    public static final Path CONFIG_PATH = CONFIG_DIR.resolve("config.properties");

    // ---- Keys ----
    public static final String KEY_INDEX_DIR    = "index.dir";
    public static final String KEY_ROOT_FOLDERS = "root.folders";
    public static final String KEY_SKIP_FOLDERS = "skip.folders";
    public static final String KEY_MAX_RESULTS  = "max.results";

    // ---- Defaults ----
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put(KEY_INDEX_DIR,    Paths.get(System.getProperty("user.home"), ".filesearch", "index").toString());
        DEFAULTS.put(KEY_ROOT_FOLDERS, "C:\\");
        DEFAULTS.put(KEY_SKIP_FOLDERS,
                "Windows,node_modules,.git,$Recycle.Bin,System Volume Information," +
                        "ProgramData,AppData,hiberfil.sys,pagefile.sys,__pycache__," +
                        ".gradle,.m2,.idea,.vscode,target,build,dist,out");
        DEFAULTS.put(KEY_MAX_RESULTS,  "20");
    }

    private final Properties props = new Properties();

    public ConfigManager() {
        load();
    }

    private void load() {
        DEFAULTS.forEach(props::setProperty);

        if (Files.exists(CONFIG_PATH)) {
            // FIX: Explicit UTF-8 reader — default Properties.load(InputStream) uses ISO-8859-1
            // which corrupts Vietnamese characters (ọ, ắ, ề, etc.) in folder paths.
            try (InputStream in = Files.newInputStream(CONFIG_PATH);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                System.err.println("Could not load config, using defaults: " + e.getMessage());
            }
        } else {
            save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException ignored) {}

        // FIX: Explicit UTF-8 writer — default Properties.store(OutputStream) uses ISO-8859-1
        try (OutputStream out = Files.newOutputStream(CONFIG_PATH);
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            props.store(writer,
                    "Search Engine Configuration\n" +
                            "# root.folders  = comma-separated drive/folder paths\n" +
                            "# skip.folders  = comma-separated folder names to skip\n" +
                            "# index.dir     = where Lucene stores its index\n" +
                            "# max.results   = how many results to display"
            );
        } catch (IOException e) {
            System.err.println("Could not save config: " + e.getMessage());
        }
    }

    public String get(String key) {
        return props.getProperty(key, DEFAULTS.getOrDefault(key, ""));
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
    }

    /** Split a comma-separated config value into a trimmed list, ignoring blanks. */
    public List<String> getList(String key) {
        String val = get(key);
        if (val.isBlank()) return Collections.emptyList();
        return Arrays.stream(val.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    public int getInt(String key, int fallback) {
        try { return Integer.parseInt(get(key).trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}