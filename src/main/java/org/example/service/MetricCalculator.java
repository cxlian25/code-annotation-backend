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
        double brevityPenalty = candLen > refLen ? 1.0 : Math.exp(1.0 - ((double) refLen / candLen));

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
        String[] parts = text.toLowerCase().trim().split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                tokens.add(part);
            }
        }
        return tokens;
    }
}
