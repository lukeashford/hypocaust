package com.example.the_machine.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * JPA entity representing operator embeddings stored in the database for semantic search. Each
 * embedding corresponds to an operator's description and metadata converted to a vector
 * representation for similarity-based retrieval.
 */
@Entity
@Table(name = "operator_embeddings")
data class OperatorEmbedding(
  @Column(name = "operator_name", unique = true, nullable = false)
  val operatorName: String,

  @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
  @JdbcTypeCode(SqlTypes.ARRAY)
  val embedding: FloatArray,

  @Column(nullable = false, length = 64)
  val hash: String,

  ) : BaseEntity() {


  // Override equals and hashCode to handle FloatArray properly
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as OperatorEmbedding

    if (id != other.id) return false
    if (operatorName != other.operatorName) return false
    if (!embedding.contentEquals(other.embedding)) return false
    if (hash != other.hash) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + operatorName.hashCode()
    result = 31 * result + embedding.contentHashCode()
    result = 31 * result + hash.hashCode()
    result = 31 * result + createdAt.hashCode()
    return result
  }

}