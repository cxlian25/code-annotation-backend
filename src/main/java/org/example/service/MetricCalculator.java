package org.example.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MetricCalculator {

    public double bleu(String reference, String candidate) {
        List<String> refTokens = tokenize(reference);
        List<String> candTokens = tokenize(candidate);

        if (candTokens.isEmpty()) {
            return 0.0;
        }

        double logPrecisionSum = 0.0;
        int maxN = 4;

        for (int n = 1; n <= maxN; n++) {
            double precision = modifiedPrecision(refTokens, candTokens, n);
            logPrecisionSum += (1.0 / maxN) * Math.log(precision);
        }

        int refLen = refTokens.size();
        int candLen = candTokens.size();

        //BP：长度惩罚因子
        double brevityPenalty = candLen > refLen ? 1.0 : Math.exp(1.0 - ((double) refLen / candLen));

        return brevityPenalty * Math.exp(logPrecisionSum);
    }

    public double bleuCorpus(List<String> references, List<String> candidates) {
        if (references == null || candidates == null) {
            return 0.0;
        }

        int total = Math.min(references.size(), candidates.size());
        if (total == 0) {
            return 0.0;
        }

        List<List<String>> referenceTokens = new ArrayList<>(total);
        List<List<String>> candidateTokens = new ArrayList<>(total);
        int refLenSum = 0;
        int candLenSum = 0;

        for (int i = 0; i < total; i++) {
            List<String> ref = tokenize(references.get(i));
            List<String> cand = tokenize(candidates.get(i));
            referenceTokens.add(ref);
            candidateTokens.add(cand);
            refLenSum += ref.size();
            candLenSum += cand.size();
        }

        if (candLenSum == 0) {
            return 0.0;
        }

        double logPrecisionSum = 0.0;
        int maxN = 4;
        for (int n = 1; n <= maxN; n++) {
            double precision = modifiedPrecisionCorpus(referenceTokens, candidateTokens, n);
            logPrecisionSum += (1.0 / maxN) * Math.log(precision);
        }

        double brevityPenalty = candLenSum > refLenSum ? 1.0 : Math.exp(1.0 - ((double) refLenSum / candLenSum));
        return brevityPenalty * Math.exp(logPrecisionSum);
    }

    public double meteor(String reference, String candidate) {
        List<String> refTokens = tokenize(reference);
        List<String> candTokens = tokenize(candidate);

        if (refTokens.isEmpty() || candTokens.isEmpty()) {
            return 0.0;
        }

        Map<String, List<Integer>> refPositions = new HashMap<>();
        for (int i = 0; i < refTokens.size(); i++) {
            refPositions.computeIfAbsent(refTokens.get(i), key -> new ArrayList<>()).add(i);
        }

        List<Integer> matchedPositions = new ArrayList<>();
        for (String token : candTokens) {
            List<Integer> positions = refPositions.get(token);
            if (positions != null && !positions.isEmpty()) {
                matchedPositions.add(positions.remove(0));
            }
        }

        int matched = matchedPositions.size();
        if (matched == 0) {
            return 0.0;
        }

        double precision = (double) matched / candTokens.size();
        double recall = (double) matched / refTokens.size();
        double fMean = (10.0 * precision * recall) / (recall + 9.0 * precision);

        int chunks = 1;
        for (int i = 1; i < matchedPositions.size(); i++) {
            if (matchedPositions.get(i) != matchedPositions.get(i - 1) + 1) {
                chunks++;
            }
        }

        double penalty = 0.5 * Math.pow((double) chunks / matched, 3);
        return fMean * (1.0 - penalty);
    }

    public double rougeL(String reference, String candidate) {
        List<String> refTokens = tokenize(reference);
        List<String> candTokens = tokenize(candidate);

        if (refTokens.isEmpty() || candTokens.isEmpty()) {
            return 0.0;
        }

        int lcs = lcsLength(refTokens, candTokens);
        double precision = (double) lcs / candTokens.size();
        double recall = (double) lcs / refTokens.size();

        if (precision == 0.0 || recall == 0.0) {
            return 0.0;
        }

        return (2.0 * precision * recall) / (precision + recall);
    }

    private double modifiedPrecision(List<String> reference, List<String> candidate, int n) {
        Map<String, Integer> refCounts = countNgrams(reference, n);
        Map<String, Integer> candCounts = countNgrams(candidate, n);

        int clipped = 0;
        int total = 0;

        for (Map.Entry<String, Integer> entry : candCounts.entrySet()) {
            int refCount = refCounts.getOrDefault(entry.getKey(), 0);
            clipped += Math.min(refCount, entry.getValue());
            total += entry.getValue();
        }

        if (total == 0) {
            return 1.0;
        }

        //平滑处理
        return (clipped + 1.0) / (total + 1.0);
    }

    private double modifiedPrecisionCorpus(List<List<String>> references, List<List<String>> candidates, int n) {
        int clipped = 0;
        int total = 0;

        int sampleCount = Math.min(references.size(), candidates.size());
        for (int i = 0; i < sampleCount; i++) {
            Map<String, Integer> refCounts = countNgrams(references.get(i), n);
            Map<String, Integer> candCounts = countNgrams(candidates.get(i), n);
            for (Map.Entry<String, Integer> entry : candCounts.entrySet()) {
                int refCount = refCounts.getOrDefault(entry.getKey(), 0);
                clipped += Math.min(refCount, entry.getValue());
                total += entry.getValue();
            }
        }

        return (clipped + 1.0) / (total + 1.0);
    }

    private Map<String, Integer> countNgrams(List<String> tokens, int n) {
        Map<String, Integer> ngrams = new HashMap<>();
        for (int i = 0; i + n <= tokens.size(); i++) {
            String key = String.join(" ", tokens.subList(i, i + n));
            ngrams.put(key, ngrams.getOrDefault(key, 0) + 1);
        }
        return ngrams;
    }

    private int lcsLength(List<String> reference, List<String> candidate) {
        int m = reference.size();
        int n = candidate.size();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (reference.get(i - 1).equals(candidate.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp[m][n];
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();
        String normalized = text.toLowerCase().trim();

        for (int i = 0; i < normalized.length(); ) {
            int codePoint = normalized.codePointAt(i);
            i += Character.charCount(codePoint);

            if (isHanCharacter(codePoint)) {
                flushToken(tokens, currentWord);
                tokens.add(new String(Character.toChars(codePoint)));
                continue;
            }

            if (isWordCharacter(codePoint)) {
                currentWord.appendCodePoint(codePoint);
            } else {
                flushToken(tokens, currentWord);
            }
        }

        flushToken(tokens, currentWord);
        return tokens;
    }

    private void flushToken(List<String> tokens, StringBuilder currentWord) {
        if (currentWord.length() > 0) {
            tokens.add(currentWord.toString());
            currentWord.setLength(0);
        }
    }

    private boolean isHanCharacter(int codePoint) {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }

    private boolean isWordCharacter(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '_';
    }
}
