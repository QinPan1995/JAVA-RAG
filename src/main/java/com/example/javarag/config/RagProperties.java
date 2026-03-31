package com.example.javarag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private String openaiApiKey = "";
    private String openaiBaseUrl = "https://api.openai.com/v1";
    private String chatModel = "gpt-5.4";
    private String embeddingModel = "text-embedding-3-small";
    private int topK = 2;
    private double minScore = 0.55;
    private double maxScoreGap = 0.08;
    private double lexicalMinScore = 0.06;
    private int maxContextChunks = 2;
    private int maxChunkCharsInPrompt = 220;
    private int chunkSize = 350;
    private int chunkOverlap = 50;

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    public String getOpenaiBaseUrl() {
        return openaiBaseUrl;
    }

    public void setOpenaiBaseUrl(String openaiBaseUrl) {
        this.openaiBaseUrl = openaiBaseUrl;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public double getMaxScoreGap() {
        return maxScoreGap;
    }

    public void setMaxScoreGap(double maxScoreGap) {
        this.maxScoreGap = maxScoreGap;
    }

    public double getLexicalMinScore() {
        return lexicalMinScore;
    }

    public void setLexicalMinScore(double lexicalMinScore) {
        this.lexicalMinScore = lexicalMinScore;
    }

    public int getMaxContextChunks() {
        return maxContextChunks;
    }

    public void setMaxContextChunks(int maxContextChunks) {
        this.maxContextChunks = maxContextChunks;
    }

    public int getMaxChunkCharsInPrompt() {
        return maxChunkCharsInPrompt;
    }

    public void setMaxChunkCharsInPrompt(int maxChunkCharsInPrompt) {
        this.maxChunkCharsInPrompt = maxChunkCharsInPrompt;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

}
