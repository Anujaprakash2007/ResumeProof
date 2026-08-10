package com.resumeproof.parser;

public class TextNormalizer {

    public static String normalize(String text) {
        if (text == null) return "";
        return text.trim().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
    }

    public static String cleanToken(String token) {
        if (token == null) return "";
        return token.replaceAll("[^a-zA-Z0-9+#]", "").toLowerCase();
    }

    public static boolean containsPhrase(String text, String phrase) {
        if (text == null || phrase == null) return false;
        String normalizedText = text.toLowerCase();
        String normalizedPhrase = phrase.toLowerCase();
        return normalizedText.contains(normalizedPhrase);
    }
}
