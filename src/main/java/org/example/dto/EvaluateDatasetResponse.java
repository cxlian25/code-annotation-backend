package org.example.dto;

public class EvaluateDatasetResponse {
    // 实际参与评测的样本数量
    private int sampleCount;
    // 平均 BLEU 分数（衡量生成注释与参考注释的 n-gram 重合度）
    private double bleu;
    // 平均 METEOR 分数（综合考虑精确率、召回率和片段连续性）
    private double meteor;
    // 平均 ROUGE-L 分数（基于最长公共子序列的相似度）
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
