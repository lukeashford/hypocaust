package com.example.scraper.adapter.pg

import com.example.scraper.domain.DocumentRepository
import com.example.scraper.domain.Result
import com.example.scraper.domain.ScraperError
import com.example.scraper.domain.SourceDoc
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/**
 * Implementation of DocumentRepository using Exposed ORM.
 */
class ExposedDocumentRepository : DocumentRepository {

  override fun saveDocument(doc: SourceDoc): Result<SourceDoc> {
    return try {
      transaction {
        SourceDocTable.insert {
          it[id] = doc.id
          it[companyId] = doc.companyId
          it[crawlJobId] = doc.crawlJobId
          it[sourceType] = doc.sourceType
          it[externalId] = doc.externalId
          it[url] = doc.url
          it[title] = doc.title
          it[rawContent] = doc.rawContent
          it[langCode] = doc.langCode
          it[fetchedAt] = doc.fetchedAt.toOffsetDateTime()
          it[checksum] = doc.checksum
        }

        Result.Success(doc)
      }
    } catch (e: Exception) {
      Result.Failure(ScraperError.DatabaseError("Failed to save document: ${e.message}", e))
    }
  }

  override fun findDocumentById(id: UUID): Result<SourceDoc> {
    return try {
      transaction {
        val result = SourceDocTable.selectAll().where { SourceDocTable.id eq id }.singleOrNull()
          ?.let { mapRowToSourceDoc(it) }

        if (result != null) {
          Result.Success(result)
        } else {
          Result.Failure(ScraperError.DocumentNotFound(id.toString()))
        }
      }
    } catch (e: Exception) {
      Result.Failure(ScraperError.DatabaseError("Failed to find document by ID: ${e.message}", e))
    }
  }

  override fun findDocumentsByCompany(companyId: UUID, limit: Int?): Result<List<SourceDoc>> {
    return try {
      transaction {
        val query = SourceDocTable.selectAll().where { SourceDocTable.companyId eq companyId }

        val results = if (limit != null) {
          query.limit(limit).map { mapRowToSourceDoc(it) }
        } else {
          query.map { mapRowToSourceDoc(it) }
        }

        Result.Success(results)
      }
    } catch (e: Exception) {
      Result.Failure(
        ScraperError.DatabaseError(
          "Failed to find documents by company: ${e.message}",
          e
        )
      )
    }
  }

  private fun mapRowToSourceDoc(row: ResultRow): SourceDoc {
    return SourceDoc(
      id = row[SourceDocTable.id],
      companyId = row[SourceDocTable.companyId],
      crawlJobId = row[SourceDocTable.crawlJobId],
      sourceType = row[SourceDocTable.sourceType],
      externalId = row[SourceDocTable.externalId],
      url = row[SourceDocTable.url],
      title = row[SourceDocTable.title],
      rawContent = row[SourceDocTable.rawContent],
      langCode = row[SourceDocTable.langCode],
      fetchedAt = ZonedDateTime.ofInstant(
        row[SourceDocTable.fetchedAt].toInstant(),
        ZoneId.systemDefault()
      ),
      checksum = row[SourceDocTable.checksum]
    )
  }
}
