package com.example.javarag.service;

import com.example.javarag.config.RagProperties;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingModel openAiEmbeddingModel;

    public EmbeddingService(RagProperties ragProperties) {
        if (StringUtils.hasText(ragProperties.getOpenaiApiKey())) {
            this.openAiEmbeddingModel = OpenAiEmbeddingModel.builder()
                    .baseUrl(ragProperties.getOpenaiBaseUrl())
                    .apiKey(ragProperties.getOpenaiApiKey())
                    .modelName(ragProperties.getEmbeddingModel())
                    .build();
        } else {
            this.openAiEmbeddingModel = null;
        }
    }

    public Embedding embed(String text) {
        if (openAiEmbeddingModel != null) {
            Response<Embedding> response = openAiEmbeddingModel.embed(text);
            return response.content();
        }
        return Embedding.from(hashEmbedding(text));
    }

    private float[] hashEmbedding(String text) {
        int dim = 384;
        float[] vector = new float[dim];
        String normalized = text == null ? "" : text.toLowerCase();
        List<String> tokens = buildFeatureTokens(normalized);

        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            int h1 = Math.abs((token + "#1").hashCode()) % dim;
            int h2 = Math.abs((token + "#2").hashCode()) % dim;
            vector[h1] += 1.0f;
            vector[h2] += 0.5f;
        }

        double sum = 0.0;
        for (float v : vector) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm > 0.0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
        return vector;
    }

    private List<String> buildFeatureTokens(String text) {
        List<String> tokens = new ArrayList<>();

        String[] wordTokens = text.split("[^\\p{L}\\p{N}]+");
        for (String word : wordTokens) {
            if (!word.isBlank()) {
                tokens.add("w:" + word);
            }
        }

        String compact = text.replaceAll("\\s+", "");
        for (int i = 0; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                tokens.add("c1:" + c);
            }
            if (i + 1 < compact.length()) {
                char next = compact.charAt(i + 1);
                if (Character.isLetterOrDigit(c) && Character.isLetterOrDigit(next)) {
                    tokens.add("c2:" + c + next);
                }
            }
        }
        return tokens;
    }

    public boolean usingRealOpenAiEmbedding() {
        return openAiEmbeddingModel != null;
    }
}
