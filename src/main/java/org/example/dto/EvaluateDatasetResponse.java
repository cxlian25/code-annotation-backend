package org.example.dto;

public class EvaluateDatasetResponse {
    // Number of evaluated samples.
    private int sampleCount;
    // Main BLEU score (corpus-level BLEU over the whole evaluated set).
    private double bleu;
    // Average METEOR score.
    private double meteor;
    // Main ROUGE-L score (corpus-level ROUGE-L over the whole evaluated set).
    private double rougeL;

    public EvaluateDatasetResponse() {
    }

    public EvaluateDatasetResponse(int sampleCount, double bleu, double meteor, double rougeL) {
        this.sampleCount = sampleCount;
        this.bleu = bleu;
        this.meteor = meteor;
        this.rougeL = rougeL;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public double getBleu() {
        return bleu;
    }

    public void setBleu(double bleu) {
        this.bleu = bleu;
    }

    public double getMeteor() {
        return meteor;
    }

    public void setMeteor(double meteor) {
        this.meteor = meteor;
    }

    public double getRougeL() {
        return rougeL;
    }

    public void setRougeL(double rougeL) {
        this.rougeL = rougeL;
    }
}
