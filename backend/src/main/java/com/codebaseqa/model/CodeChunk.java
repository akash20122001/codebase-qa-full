package com.codebaseqa.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "code_chunks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CodeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private Repo repo;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "start_line", nullable = false)
    private Integer startLine;

    @Column(name = "end_line", nullable = false)
    private Integer endLine;

    @Column(name = "chunk_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ChunkType chunkType;

    @Column(name = "chunk_name")
    private String chunkName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String language;

    // Stored as pgvector type - Hibernate will pass the string directly to PostgreSQL
    // PostgreSQL will cast "[1,2,3,...]" format to vector type
    @Column(nullable = false, columnDefinition = "vector(3072)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String embedding;

    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum ChunkType {
        FUNCTION, CLASS, METHOD, MODULE, BLOCK, INTERFACE, ENUM
    }
}
