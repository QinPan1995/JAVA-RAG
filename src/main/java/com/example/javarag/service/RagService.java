package com.example.javarag.service;

import com.example.javarag.config.RagProperties;
import com.example.javarag.dto.AskResponse;
import com.example.javarag.dto.RetrievedChunk;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RagService {

    private final RagProperties ragProperties;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final PromptService promptService;
    private final LlmService llmService;
    private final MySqlPersistenceService mySqlPersistenceService;

    public RagService(RagProperties ragProperties,
                      EmbeddingService embeddingService,
                      VectorStoreService vectorStoreService,
                      PromptService promptService,
                      LlmService llmService,
                      MySqlPersistenceService mySqlPersistenceService) {
        this.ragProperties = ragProperties;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.promptService = promptService;
        this.llmService = llmService;
        this.mySqlPersistenceService = mySqlPersistenceService;
    }

    public void indexDocument(String id, String content) {
        indexDocument(id, content, Map.of(), true);
    }

    @Transactional
    public void indexDocument(String id, String content, Map<String, Object> metadata, boolean persist) {
        List<String> chunks = split(content, ragProperties.getChunkSize(), ragProperties.getChunkOverlap());
        List<MySqlPersistenceService.PersistedChunk> persistedChunks = new ArrayList<>();
        List<VectorStoreService.ChunkVector> chunkVectors = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = buildChunkId(id, i);
            String chunkContent = chunks.get(i);
            Map<String, Object> chunkMetadata = new HashMap<>(metadata == null ? Map.of() : metadata);
            chunkMetadata.put("chunkIndex", i);
            chunkMetadata.put("chunkId", chunkId);

            persistedChunks.add(new MySqlPersistenceService.PersistedChunk(chunkId, i, chunkContent, chunkMetadata));
            chunkVectors.add(new VectorStoreService.ChunkVector(
                    chunkId,
                    chunkContent,
                    chunkMetadata,
                    embeddingService.embed(chunkContent)
            ));
        }

        vectorStoreService.replaceDocumentChunks(id, chunkVectors);
        if (persist) {
            mySqlPersistenceService.upsertDocumentAndChunks(id, content, metadata, persistedChunks);
        }
    }

    public AskResponse ask(String question, Integer topK) {
        int k = topK == null || topK <= 0 ? ragProperties.getTopK() : topK;
        var queryEmbedding = embeddingService.embed(question);
        List<RetrievedChunk> retrievedChunks = vectorStoreService.search(
                queryEmbedding,
                k,
                ragProperties.getMinScore(),
                ragProperties.getMaxScoreGap()
        );
        if (!embeddingService.usingRealOpenAiEmbedding()) {
            retrievedChunks = filterByLexicalRelevance(retrievedChunks, question, ragProperties.getLexicalMinScore());
        }
        if (retrievedChunks.size() > ragProperties.getMaxContextChunks()) {
            retrievedChunks = retrievedChunks.subList(0, ragProperties.getMaxContextChunks());
        }
        String prompt = promptService.buildRagPrompt(question, retrievedChunks);
        String answer = llmService.generateAnswer(prompt, question, retrievedChunks);
        return new AskResponse(answer, retrievedChunks, llmService.usingRealOpenAiChat());
    }

    public int totalChunks() {
        return vectorStoreService.size();
    }

    private List<String> split(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int start = 0;
        int length = text.length();
        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            chunks.add(text.substring(start, end));
            if (end == length) {
                break;
            }
            start = Math.max(end - chunkOverlap, start + 1);
        }
        return chunks;
    }

    private List<RetrievedChunk> filterByLexicalRelevance(List<RetrievedChunk> chunks, String question, double minLexicalScore) {
        Set<String> queryFeatures = buildLexicalFeatures(question);
        if (queryFeatures.isEmpty()) {
            return chunks;
        }
        List<RetrievedChunk> filtered = new ArrayList<>();
        for (RetrievedChunk chunk : chunks) {
            Set<String> chunkFeatures = buildLexicalFeatures(chunk.content());
            if (chunkFeatures.isEmpty()) {
                continue;
            }
            Set<String> intersection = new HashSet<>(queryFeatures);
            intersection.retainAll(chunkFeatures);
            double score = (double) intersection.size() / (double) queryFeatures.size();
            if (score >= minLexicalScore) {
                filtered.add(chunk);
            }
        }
        return filtered;
    }

    private Set<String> buildLexicalFeatures(String text) {
        Set<String> features = new HashSet<>();
        if (text == null || text.isBlank()) {
            return features;
        }
        String normalized = text.toLowerCase().replaceAll("\\s+", "");

        String[] words = normalized.split("[^\\p{L}\\p{N}]+");
        for (String word : words) {
            if (word.length() >= 2) {
                features.add("w:" + word);
            }
        }

        for (int i = 0; i < normalized.length() - 1; i++) {
            char c1 = normalized.charAt(i);
            char c2 = normalized.charAt(i + 1);
            if (Character.isLetterOrDigit(c1) && Character.isLetterOrDigit(c2)) {
                features.add("c2:" + c1 + c2);
            }
        }
        return features;
    }

    private String buildChunkId(String documentId, int chunkIndex) {
        return documentId + "::chunk::" + chunkIndex;
    }
}
