package com.example.javarag.service;

import com.example.javarag.config.RagProperties;
import com.example.javarag.dto.RetrievedChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

@Service
public class PromptService {

    private final RagProperties ragProperties;

    public PromptService(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public String buildRagPrompt(String question, List<RetrievedChunk> retrievedChunks) {
        StringJoiner contextJoiner = new StringJoiner("\n\n");
        for (int i = 0; i < retrievedChunks.size(); i++) {
            RetrievedChunk chunk = retrievedChunks.get(i);
            contextJoiner.add("[chunk-" + (i + 1) + "] score=" + String.format("%.4f", chunk.score()) + "\n"
                    + truncate(chunk.content(), ragProperties.getMaxChunkCharsInPrompt()));
        }

        return """
                你是一个企业知识库问答助手。
                你只能基于给定上下文回答，不允许使用外部常识补充。
                如果上下文不足以回答，请明确说“根据当前知识库内容无法确定”。
                不要输出与问题无关的信息，不要整段复读上下文原文。
                回答控制在 3-5 句，优先给结论。

                上下文：
                %s

                用户问题：
                %s
                """.formatted(contextJoiner.toString(), question);
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
