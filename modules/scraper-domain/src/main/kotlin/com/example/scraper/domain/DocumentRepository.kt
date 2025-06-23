package com.example.scraper.domain

import java.util.*

/**
 * Interface for accessing and storing documents.
 */
interface DocumentRepository {

  /**
   * Saves a source document.
   *
   * @param doc The document to save.
   * @return Result indicating success or failure with an error.
   */
  fun saveDocument(doc: SourceDoc): Result<SourceDoc>

  /**
   * Finds a document by its ID.
   *
   * @param id The ID of the document to find.
   * @return Result containing the document if found, or a failure with an error.
   */
  fun findDocumentById(id: UUID): Result<SourceDoc>

  /**
   * Finds documents for a specific company.
   *
   * @param companyId The ID of the company.
   * @param limit Optional limit on the number of documents to return.
   * @return Result containing a list of documents if found, or a failure with an error.
   */
  fun findDocumentsByCompany(companyId: UUID, limit: Int? = null): Result<List<SourceDoc>>
}
