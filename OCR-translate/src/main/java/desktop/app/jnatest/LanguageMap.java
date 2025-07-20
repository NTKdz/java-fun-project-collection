package desktop.app.jnatest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LanguageMap {
    private static final Map<String, String> tessLangMap = new LinkedHashMap<>();
    private static final Map<String, String> lingvaLangMap = new LinkedHashMap<>();

    static {
        // Friendly name → Tesseract code (based on tessdata .traineddata files)
        // Tesseract codes are typically ISO 639-3 or custom (e.g., chi_sim)
        tessLangMap.put("Afrikaans", "afr");
        tessLangMap.put("Albanian", "sqi");
        tessLangMap.put("Amharic", "amh");
        tessLangMap.put("Arabic", "ara");
        tessLangMap.put("Armenian", "hye");
        tessLangMap.put("Bengali", "ben");
        tessLangMap.put("Bulgarian", "bul");
        tessLangMap.put("Catalan", "cat");
        tessLangMap.put("Chinese (Simplified)", "chi_sim");
        tessLangMap.put("Chinese (Traditional)", "chi_tra");
        tessLangMap.put("Croatian", "hrv");
        tessLangMap.put("Czech", "ces");
        tessLangMap.put("Danish", "dan");
        tessLangMap.put("Dutch", "nld");
        tessLangMap.put("English", "eng");
        tessLangMap.put("Estonian", "est");
        tessLangMap.put("Finnish", "fin");
        tessLangMap.put("French", "fra");
        tessLangMap.put("Georgian", "kat");
        tessLangMap.put("German", "deu");
        tessLangMap.put("Greek", "ell");
        tessLangMap.put("Hebrew", "heb");
        tessLangMap.put("Hindi", "hin");
        tessLangMap.put("Hungarian", "hun");
        tessLangMap.put("Icelandic", "isl");
        tessLangMap.put("Indonesian", "ind");
        tessLangMap.put("Italian", "ita");
        tessLangMap.put("Japanese", "jpn");
        tessLangMap.put("Korean", "kor");
        tessLangMap.put("Latvian", "lav");
        tessLangMap.put("Lithuanian", "lit");
        tessLangMap.put("Malay", "msa");
        tessLangMap.put("Norwegian", "nor");
        tessLangMap.put("Persian", "fas");
        tessLangMap.put("Polish", "pol");
        tessLangMap.put("Portuguese", "por");
        tessLangMap.put("Romanian", "ron");
        tessLangMap.put("Russian", "rus");
        tessLangMap.put("Serbian", "srp");
        tessLangMap.put("Slovak", "slk");
        tessLangMap.put("Slovenian", "slv");
        tessLangMap.put("Spanish", "spa");
        tessLangMap.put("Swahili", "swa");
        tessLangMap.put("Swedish", "swe");
        tessLangMap.put("Tamil", "tam");
        tessLangMap.put("Telugu", "tel");
        tessLangMap.put("Thai", "tha");
        tessLangMap.put("Turkish", "tur");
        tessLangMap.put("Ukrainian", "ukr");
        tessLangMap.put("Vietnamese", "vie");

        // Friendly name → Lingva code (based on ISO 639-1 or Google Translate codes)
        lingvaLangMap.put("Afrikaans", "af");
        lingvaLangMap.put("Albanian", "sq");
        lingvaLangMap.put("Amharic", "am");
        lingvaLangMap.put("Arabic", "ar");
        lingvaLangMap.put("Armenian", "hy");
        lingvaLangMap.put("Bengali", "bn");
        lingvaLangMap.put("Bulgarian", "bg");
        lingvaLangMap.put("Catalan", "ca");
        lingvaLangMap.put("Chinese (Simplified)", "zh");
        lingvaLangMap.put("Chinese (Traditional)", "zh-TW");
        lingvaLangMap.put("Croatian", "hr");
        lingvaLangMap.put("Czech", "cs");
        lingvaLangMap.put("Danish", "da");
        lingvaLangMap.put("Dutch", "nl");
        lingvaLangMap.put("English", "en");
        lingvaLangMap.put("Estonian", "et");
        lingvaLangMap.put("Finnish", "fi");
        lingvaLangMap.put("French", "fr");
        lingvaLangMap.put("Georgian", "ka");
        lingvaLangMap.put("German", "de");
        lingvaLangMap.put("Greek", "el");
        lingvaLangMap.put("Hebrew", "he");
        lingvaLangMap.put("Hindi", "hi");
        lingvaLangMap.put("Hungarian", "hu");
        lingvaLangMap.put("Icelandic", "is");
        lingvaLangMap.put("Indonesian", "id");
        lingvaLangMap.put("Italian", "it");
        lingvaLangMap.put("Japanese", "ja");
        lingvaLangMap.put("Korean", "ko");
        lingvaLangMap.put("Latvian", "lv");
        lingvaLangMap.put("Lithuanian", "lt");
        lingvaLangMap.put("Malay", "ms");
        lingvaLangMap.put("Norwegian", "no");
        lingvaLangMap.put("Persian", "fa");
        lingvaLangMap.put("Polish", "pl");
        lingvaLangMap.put("Portuguese", "pt");
        lingvaLangMap.put("Romanian", "ro");
        lingvaLangMap.put("Russian", "ru");
        lingvaLangMap.put("Serbian", "sr");
        lingvaLangMap.put("Slovak", "sk");
        lingvaLangMap.put("Slovenian", "sl");
        lingvaLangMap.put("Spanish", "es");
        lingvaLangMap.put("Swahili", "sw");
        lingvaLangMap.put("Swedish", "sv");
        lingvaLangMap.put("Tamil", "ta");
        lingvaLangMap.put("Telugu", "te");
        lingvaLangMap.put("Thai", "th");
        lingvaLangMap.put("Turkish", "tr");
        lingvaLangMap.put("Ukrainian", "uk");
        lingvaLangMap.put("Vietnamese", "vi");
    }

    /**
     * Returns the Tesseract language code for a given friendly name.
     * @param friendlyName The user-friendly language name (e.g., "English").
     * @return The Tesseract language code (e.g., "eng"), or "eng" if not found.
     */
    public static String getTesseractCode(String friendlyName) {
        String code = tessLangMap.get(friendlyName);
        if (code == null) {
            System.err.println("Tesseract code not found for: " + friendlyName + ", defaulting to 'eng'");
            return "eng";
        }
        return code;
    }

    /**
     * Returns the Lingva language code for a given friendly name.
     * @param friendlyName The user-friendly language name (e.g., "English").
     * @return The Lingva language code (e.g., "en"), or "en" if not found.
     */
    public static String getLingvaCode(String friendlyName) {
        String code = lingvaLangMap.get(friendlyName);
        if (code == null) {
            System.err.println("Lingva code not found for: " + friendlyName + ", defaulting to 'en'");
            return "en";
        }
        return code;
    }

    /**
     * Returns an array of supported language names.
     * @return Array of friendly language names.
     */
    public static String[] getSupportedLanguages() {
        return tessLangMap.keySet().toArray(new String[0]);
    }

    /**
     * Checks if a language is supported by both Tesseract and Lingva.
     * @param friendlyName The user-friendly language name.
     * @return True if the language is supported by both, false otherwise.
     */
    public static boolean isLanguageSupported(String friendlyName) {
        boolean supported = tessLangMap.containsKey(friendlyName) && lingvaLangMap.containsKey(friendlyName);
        if (!supported) {
            System.err.println("Language not supported by both Tesseract and Lingva: " + friendlyName);
        }
        return supported;
    }
}