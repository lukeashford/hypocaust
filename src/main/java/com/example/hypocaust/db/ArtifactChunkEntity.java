package com.example.hypocaust.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "artifact_chunk")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ArtifactChunkEntity extends BaseEntity {

  private static final int DIM = 1536;
  private static final String VECTOR_DEF = "vector(1536)";

  @Column(nullable = false)
  private UUID artifactId;

  @Column(nullable = false)
  private UUID projectId;

  @Column(nullable = false)
  private String fieldPath;

  @Column(nullable = false)
  private int chunkIndex;

  @Column(nullable = false)
  private int charOffset;

  @Column(nullable = false, columnDefinition = "text")
  private String text;

  @Column(nullable = false, columnDefinition = VECTOR_DEF)
  @JdbcTypeCode(SqlTypes.VECTOR)
  @Array(length = DIM)
  private float[] embedding;
}
