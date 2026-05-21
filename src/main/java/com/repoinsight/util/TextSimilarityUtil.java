package com.repoinsight.util;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utilities for text similarity and token-based matching.
 */
@Component
public class TextSimilarityUtil {

    private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();
    private static final LevenshteinDistance LEVENSHTEIN = LevenshteinDistance.getDefaultInstance();
    private static final Pattern WORD_SPLIT = Pattern.compile("[\\s_\\-./\\\\:]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "it", "in", "on", "at", "of", "to", "for",
            "and", "or", "but", "not", "with", "by", "as", "be", "are", "was",
            "test", "tests", "spec", "feature", "scenario", "step", "when", "then",
            "given", "should", "can", "will", "that", "this", "from", "all"
    );

    /**
     * Normalize text for comparison: lowercase, split camelCase, remove stop words.
     */
    public String normalize(String text) {
        if (text == null) return "";
        // split camelCase
        String spaced = text.replaceAll("([a-z])([A-Z])", "$1 $2")
                            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        return WORD_SPLIT.splitAsStream(spaced.toLowerCase())
                .filter(t -> !t.isBlank() && !STOP_WORDS.contains(t))
                .collect(Collectors.joining(" "));
    }

    /**
     * Returns a set of significant tokens from the text.
     */
    public Set<String> tokenize(String text) {
        if (text == null) return Set.of();
        String normalized = normalize(text);
        return Arrays.stream(normalized.split("\\s+"))
                .filter(t -> t.length() > 2)
                .collect(Collectors.toSet());
    }

    /**
     * Jaccard coefficient of token overlap between two strings.
     */
    public double tokenOverlap(String a, String b) {
        Set<String> ta = tokenize(a);
        Set<String> tb = tokenize(b);
        if (ta.isEmpty() && tb.isEmpty()) return 1.0;
        if (ta.isEmpty() || tb.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(ta);
        intersection.retainAll(tb);
        Set<String> union = new HashSet<>(ta);
        union.addAll(tb);
        return (double) intersection.size() / union.size();
    }

    /**
     * Jaro-Winkler similarity between two strings (normalised before comparison).
     */
    public double fuzzyMatch(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.isBlank() && nb.isBlank()) return 1.0;
        if (na.isBlank() || nb.isBlank()) return 0.0;
        return JARO_WINKLER.apply(na, nb);
    }

    /**
     * Returns true when the two strings match exactly after normalisation.
     */
    public boolean exactMatch(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    /**
     * Combined similarity score using weighted components.
     */
    public double combinedSimilarity(String a, String b) {
        if (exactMatch(a, b)) return 1.0;
        double token   = tokenOverlap(a, b);
        double fuzzy   = fuzzyMatch(a, b);
        // Weighted average
        return token * 0.6 + fuzzy * 0.4;
    }

    /**
     * Returns true when tokens of {@code needle} are a subset of tokens of {@code haystack}.
     */
    public boolean containsTokens(String needle, String haystack) {
        Set<String> needleTokens = tokenize(needle);
        Set<String> haystackTokens = tokenize(haystack);
        if (needleTokens.isEmpty()) return false;
        return haystackTokens.containsAll(needleTokens);
    }
}
