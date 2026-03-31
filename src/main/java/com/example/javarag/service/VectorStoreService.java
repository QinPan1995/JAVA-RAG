package com.example.javarag.service;

import com.example.javarag.dto.RetrievedChunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VectorStoreService {

    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    private final Map<String, StoredChunk> chunkByEmbeddingId = new ConcurrentHashMap<>();
    private final Map<String, List<String>> embeddingIdsByDocumentId = new ConcurrentHashMap<>();

    public List<RetrievedChunk> search(Embedding queryEmbedding, int topK, double minScore, double maxScoreGap) {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> rawMatches = new ArrayList<>(result.matches());
        rawMatches.sort(Comparator.comparing(EmbeddingMatch<TextSegment>::score).reversed());

        List<RetrievedChunk> chunks = new ArrayList<>();
        if (rawMatches.isEmpty()) {
            return chunks;
        }

        double bestScore = rawMatches.get(0).score();
        for (EmbeddingMatch<TextSegment> match : rawMatches) {
            if (match.score() < minScore) {
                continue;
            }
            if (bestScore - match.score() > maxScoreGap) {
                continue;
            }
            String matchId = match.embeddingId();
            StoredChunk storedChunk = chunkByEmbeddingId.get(matchId);
            if (storedChunk == null) {
                continue;
            }
            chunks.add(new RetrievedChunk(
                    storedChunk.chunkId(),
                    storedChunk.documentId(),
                    match.score(),
                    storedChunk.content(),
                    storedChunk.metadata()
            ));
        }
        return chunks;
    }

    public synchronized void replaceDocumentChunks(String documentId, List<ChunkVector> newChunkVectors) {
        List<String> newEmbeddingIds = new ArrayList<>();
        try {
            for (ChunkVector chunkVector : newChunkVectors) {
                TextSegment segment = TextSegment.from(chunkVector.content());
                String embeddingId = embeddingStore.add(chunkVector.embedding(), segment);
                newEmbeddingIds.add(embeddingId);
                chunkByEmbeddingId.put(embeddingId, new StoredChunk(
                        chunkVector.chunkId(),
                        documentId,
                        chunkVector.content(),
                        chunkVector.metadata()
                ));
            }
        } catch (Exception e) {
            for (String embeddingId : newEmbeddingIds) {
                embeddingStore.remove(embeddingId);
                chunkByEmbeddingId.remove(embeddingId);
            }
            throw e;
        }

        List<String> oldEmbeddingIds = embeddingIdsByDocumentId.getOrDefault(documentId, List.of());
        for (String oldEmbeddingId : oldEmbeddingIds) {
            embeddingStore.remove(oldEmbeddingId);
            chunkByEmbeddingId.remove(oldEmbeddingId);
        }
        embeddingIdsByDocumentId.put(documentId, newEmbeddingIds);
    }

    public int size() {
        return chunkByEmbeddingId.size();
    }

    public record ChunkVector(
            String chunkId,
            String content,
            Map<String, Object> metadata,
            Embedding embedding
    ) {
    }

    private record StoredChunk(
            String chunkId,
            String documentId,
            String content,
            Map<String, Object> metadata
    ) {
    }
}
