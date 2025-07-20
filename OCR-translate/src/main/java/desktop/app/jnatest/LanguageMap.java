package desktop.app.jnatest;

import java.util.LinkedHashMap;
import java.util.Map;

public class LanguageMap {

    public static final Map<String, String> tessLangMap = new LinkedHashMap<>();
    public static final Map<String, String> lingvaLangMap = new LinkedHashMap<>();

    static {
        // Friendly name → Tesseract code
        tessLangMap.put("English", "eng");
        tessLangMap.put("Spanish", "spa");
        tessLangMap.put("French", "fra");
        tessLangMap.put("German", "deu");
        tessLangMap.put("Japanese", "jpn");
        tessLangMap.put("Chinese (Simplified)", "chi_sim");

        // Friendly name → Lingva code
        lingvaLangMap.put("English", "en");
        lingvaLangMap.put("Spanish", "es");
        lingvaLangMap.put("French", "fr");
        lingvaLangMap.put("German", "de");
        lingvaLangMap.put("Japanese", "ja");
        lingvaLangMap.put("Chinese (Simplified)", "zh");
    }

    public static String getTesseractCode(String friendlyName) {
        return tessLangMap.getOrDefault(friendlyName, "eng"); // default to English
    }

    public static String getLingvaCode(String friendlyName) {
        return lingvaLangMap.getOrDefault(friendlyName, "en"); // default to English
    }
}