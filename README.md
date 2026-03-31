# Java RAG Demo (Spring Boot + LangChain4j + MySQL + MyBatis)

## 1. 技术选型
- 框架: Spring Boot
- RAG 框架: LangChain4j
- 向量库: InMemoryEmbeddingStore
- 持久化: MySQL + MyBatis
- Embedding: OpenAI `text-embedding-3-small`（无 key 回退本地 embedding）
- LLM: OpenAI `gpt-5.4`（无 key 回退 mock）

## 2. 架构流程
Controller -> Service(RAG 编排) -> Embedding -> VectorStore(检索) -> Prompt 构建 -> LLM 生成 -> 返回

## 3. MySQL 初始化（必须先执行）
```sql
CREATE DATABASE IF NOT EXISTS rag_demo
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE rag_demo;

CREATE TABLE IF NOT EXISTS rag_documents (
  document_id VARCHAR(128) NOT NULL,
  content LONGTEXT NOT NULL,
  metadata JSON NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rag_chunks (
  chunk_id VARCHAR(160) NOT NULL,
  document_id VARCHAR(128) NOT NULL,
  chunk_index INT NOT NULL,
  content LONGTEXT NOT NULL,
  metadata JSON NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (chunk_id),
  KEY idx_rag_chunks_doc (document_id),
  CONSTRAINT fk_rag_chunks_doc FOREIGN KEY (document_id)
    REFERENCES rag_documents(document_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 4. 启动前环境变量
```bash
export DB_URL="jdbc:mysql://127.0.0.1:3306/rag_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
export DB_DRIVER="com.mysql.cj.jdbc.Driver"
export DB_USER="root"
export DB_PASSWORD="your_password"

# 可选: OpenAI
export OPENAI_API_KEY="your_key"
export OPENAI_BASE_URL="https://api.openai.com/v1"
export OPENAI_CHAT_MODEL="gpt-5.4"
export OPENAI_EMBED_MODEL="text-embedding-3-small"
```

## 5. 启动
```bash
cd /Users/youniverse/Documents/JAVA-RAG
./mvnw spring-boot:run
```

## 6. API
### 健康检查
```bash
curl -s http://127.0.0.1:8080/api/rag/health
```

### 写入文档（含 metadata）
```bash
curl -s -X POST http://127.0.0.1:8080/api/rag/index \
  -H 'Content-Type: application/json' \
  -d '{"id":"doc-1","content":"采购流程：超过5000元需总监审批。","metadata":{"biz":"purchase","owner":"ops"}}'
```

### 提问
```bash
curl -s -X POST http://127.0.0.1:8080/api/rag/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"采购流程里超过5000元需要谁审批？"}'
```

返回 `references` 包含:
- `chunkId`（例如 `doc-1::chunk::0`）
- `documentId`
- `metadata`

## 7. 可调参数
- `rag.top-k=2`
- `rag.min-score=0.55`
- `rag.max-score-gap=0.08`
- `rag.lexical-min-score=0.06`
- `rag.max-context-chunks=2`
- `rag.max-chunk-chars-in-prompt=220`
- `rag.chunk-size=350`
- `rag.chunk-overlap=50`

## 8. 失败保护
- DB 写入使用事务（文档 + chunks 同时成功或同时失败）。
- 向量索引替换采用“先加新后删旧 + 失败回滚新索引”，避免中间态脏数据。
