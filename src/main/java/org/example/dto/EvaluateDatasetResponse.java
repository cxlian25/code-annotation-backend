package org.example.dto;

public class EvaluateDatasetResponse {
    private int sampleCount;
    private double bleu;
    private double meteor;
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
