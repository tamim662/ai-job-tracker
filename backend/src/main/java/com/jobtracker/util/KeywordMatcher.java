package com.jobtracker.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class KeywordMatcher {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "this", "that", "from", "have", "will",
            "you", "are", "not", "they", "your", "our", "but", "can", "all",
            "work", "role", "team", "also", "able", "good", "well", "new",
            "what", "who", "how", "when", "where", "more", "about", "into",
            "experience", "skills", "requirements", "responsibilities", "working",
            "strong", "using", "must", "would", "including", "such", "other",
            "across", "within", "through", "their", "which", "being", "been",
            "these", "those", "then", "than", "some", "each", "both", "while"
    );

    private KeywordMatcher() {}

    public static int score(String jobText, String resumeText) {
        if (jobText == null || jobText.isBlank() || resumeText == null || resumeText.isBlank()) return 0;
        Set<String> jobKeywords = extract(jobText);
        if (jobKeywords.isEmpty()) return 0;
        Set<String> resumeKeywords = extract(resumeText);
        long matched = jobKeywords.stream().filter(resumeKeywords::contains).count();
        return (int) Math.round((double) matched / jobKeywords.size() * 100);
    }

    private static Set<String> extract(String text) {
        return Arrays.stream(text.toLowerCase().split("[^a-zA-Z0-9]+"))
                .filter(w -> w.length() > 3)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }
}
