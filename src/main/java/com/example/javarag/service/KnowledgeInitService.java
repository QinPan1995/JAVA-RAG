package com.example.javarag.service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeInitService {

    private final RagService ragService;
    private final MySqlPersistenceService mySqlPersistenceService;

    public KnowledgeInitService(RagService ragService, MySqlPersistenceService mySqlPersistenceService) {
        this.ragService = ragService;
        this.mySqlPersistenceService = mySqlPersistenceService;
    }

    @PostConstruct
    public void init() {
        List<MySqlPersistenceService.PersistedDocument> persistedDocuments = mySqlPersistenceService.loadAllDocuments();
        if (persistedDocuments.isEmpty()) {
            List<String> seedFiles = List.of(
                    "knowledge/company-policy.md",
                    "knowledge/product-faq.md"
            );

            for (String file : seedFiles) {
                try {
                    ClassPathResource resource = new ClassPathResource(file);
                    byte[] bytes = resource.getInputStream().readAllBytes();
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    ragService.indexDocument(file, content, Map.of("seedFile", file), true);
                } catch (IOException ignored) {
                    // Seed 文档是可选项，不影响应用启动。
                }
            }
            persistedDocuments = mySqlPersistenceService.loadAllDocuments();
        }

        for (MySqlPersistenceService.PersistedDocument document : persistedDocuments) {
            ragService.indexDocument(document.documentId(), document.content(), document.metadata(), false);
        }
    }
}
