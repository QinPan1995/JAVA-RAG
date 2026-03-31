package com.example.javarag.service;

import com.example.javarag.config.RagProperties;
import com.example.javarag.dto.RetrievedChunk;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class LlmService {

    private final ChatModel openAiChatModel;

    public LlmService(RagProperties ragProperties) {
        if (StringUtils.hasText(ragProperties.getOpenaiApiKey())) {
            this.openAiChatModel = OpenAiChatModel.builder()
                    .baseUrl(ragProperties.getOpenaiBaseUrl())
                    .apiKey(ragProperties.getOpenaiApiKey())
                    .modelName(ragProperties.getChatModel())
                    .temperature(0.1)
                    .build();
        } else {
            this.openAiChatModel = null;
        }
    }

    public String generateAnswer(String prompt, String question, List<RetrievedChunk> chunks) {
        if (openAiChatModel != null) {
            return openAiChatModel.chat(prompt);
        }

        if (chunks.isEmpty()) {
            return "根据当前知识库内容无法确定。你可以先调用 /api/rag/index 录入更多文档。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[Mock 模式回答] 未检测到 OPENAI_API_KEY，以下是基于检索片段的答案草稿。\n");
        sb.append("问题：").append(question).append("\n");
        sb.append("参考要点：\n");
        for (int i = 0; i < chunks.size(); i++) {
            sb.append(i + 1).append(". ").append(truncate(chunks.get(i).content(), 120)).append("\n");
        }
        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    public boolean usingRealOpenAiChat() {
        return openAiChatModel != null;
    }
}
