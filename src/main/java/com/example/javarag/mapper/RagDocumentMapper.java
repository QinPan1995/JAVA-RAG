package com.example.javarag.mapper;

import com.example.javarag.mapper.model.DocumentRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface RagDocumentMapper {

    @Insert("""
            INSERT INTO rag_documents(document_id, content, metadata, updated_at)
            VALUES(#{documentId}, #{content}, #{metadataJson}, #{updatedAt})
            ON DUPLICATE KEY UPDATE
                content = VALUES(content),
                metadata = VALUES(metadata),
                updated_at = VALUES(updated_at)
            """)
    int upsertDocument(@Param("documentId") String documentId,
                       @Param("content") String content,
                       @Param("metadataJson") String metadataJson,
                       @Param("updatedAt") OffsetDateTime updatedAt);

    @Delete("DELETE FROM rag_chunks WHERE document_id = #{documentId}")
    int deleteChunksByDocumentId(@Param("documentId") String documentId);

    @Insert("""
            INSERT INTO rag_chunks(chunk_id, document_id, chunk_index, content, metadata, updated_at)
            VALUES(#{chunkId}, #{documentId}, #{chunkIndex}, #{content}, #{metadataJson}, #{updatedAt})
            """)
    int insertChunk(@Param("chunkId") String chunkId,
                    @Param("documentId") String documentId,
                    @Param("chunkIndex") int chunkIndex,
                    @Param("content") String content,
                    @Param("metadataJson") String metadataJson,
                    @Param("updatedAt") OffsetDateTime updatedAt);

    @Select("""
            SELECT document_id, content, metadata
            FROM rag_documents
            ORDER BY updated_at ASC
            """)
    List<DocumentRow> selectAllDocuments();
}
