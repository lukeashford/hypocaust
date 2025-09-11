package com.example.the_machine.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * JPA entity representing operator embeddings stored in the database for semantic search. Each
 * embedding corresponds to an operator's description and metadata converted to a vector
 * representation for similarity-based retrieval.
 */
@Entity
@Table(name = "operator_embeddings")
data class OperatorEmbedding(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "operator_name", unique = true, nullable = false)
  val operatorName: String = "",

  @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
  @JdbcTypeCode(SqlTypes.ARRAY)
  val embedding: FloatArray = floatArrayOf(),

  @Column(nullable = false, length = 64)
  val hash: String = "",

  @Column(insertable = false, updatable = false)
  val createdAt: LocalDateTime? = null
) {

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
    var result = id?.hashCode() ?: 0
    result = 31 * result + operatorName.hashCode()
    result = 31 * result + embedding.contentHashCode()
    result = 31 * result + hash.hashCode()
    result = 31 * result + (createdAt?.hashCode() ?: 0)
    return result
  }

}