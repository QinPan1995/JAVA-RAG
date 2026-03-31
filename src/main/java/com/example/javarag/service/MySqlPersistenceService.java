package com.example.javarag.service;

import com.example.javarag.mapper.RagDocumentMapper;
import com.example.javarag.mapper.model.DocumentRow;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MySqlPersistenceService {

    private final RagDocumentMapper ragDocumentMapper;
    private final ObjectMapper objectMapper;

    public MySqlPersistenceService(RagDocumentMapper ragDocumentMapper, ObjectMapper objectMapper) {
        this.ragDocumentMapper = ragDocumentMapper;
        this.objectMapper = objectMapper;
    }

    public void upsertDocumentAndChunks(String documentId,
                                        String content,
                                        Map<String, Object> documentMetadata,
                                        List<PersistedChunk> chunks) {
        OffsetDateTime now = OffsetDateTime.now();
        String metadataJson = toJson(documentMetadata);

        ragDocumentMapper.upsertDocument(
                documentId,
                content,
                metadataJson,
                now
        );

        ragDocumentMapper.deleteChunksByDocumentId(documentId);
        for (PersistedChunk chunk : chunks) {
            ragDocumentMapper.insertChunk(
                    chunk.chunkId(),
                    documentId,
                    chunk.chunkIndex(),
                    chunk.content(),
                    toJson(chunk.metadata()),
                    now
            );
        }
    }

    public List<PersistedDocument> loadAllDocuments() {
        return ragDocumentMapper.selectAllDocuments().stream()
                .map(this::toPersistedDocument)
                .toList();
    }

    private PersistedDocument toPersistedDocument(DocumentRow row) {
        return new PersistedDocument(
                row.documentId(),
                row.content(),
                parseJson(row.metadata())
        );
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (IOException e) {
            return "{}";
        }
    }

    private Map<String, Object> parseJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<HashMap<String, Object>>() {
            });
        } catch (IOException e) {
            return Map.of();
        }
    }

    public record PersistedDocument(
            String documentId,
            String content,
            Map<String, Object> metadata
    ) {
    }

    public record PersistedChunk(
            String chunkId,
            int chunkIndex,
            String content,
            Map<String, Object> metadata
    ) {
    }
}
