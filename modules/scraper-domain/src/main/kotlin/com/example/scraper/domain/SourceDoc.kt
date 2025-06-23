package com.example.scraper.domain

import java.time.ZonedDateTime
import java.util.*

/**
 * Represents a source document that has been scraped.
 */
data class SourceDoc(
  val id: UUID,
  val companyId: UUID,
  val crawlJobId: UUID?,
  val sourceType: String,
  val externalId: String?,
  val url: String?,
  val title: String?,
  val rawContent: String?,
  val langCode: String?,
  val fetchedAt: ZonedDateTime,
  val checksum: String?
) {

  companion object {

    /**
     * Creates a new SourceDoc with a generated ID and current timestamp.
     */
    fun create(
      companyId: UUID,
      crawlJobId: UUID? = null,
      sourceType: String,
      externalId: String? = null,
      url: String? = null,
      title: String? = null,
      rawContent: String? = null,
      langCode: String? = null,
      checksum: String? = null
    ): SourceDoc = SourceDoc(
      id = UUID.randomUUID(),
      companyId = companyId,
      crawlJobId = crawlJobId,
      sourceType = sourceType,
      externalId = externalId,
      url = url,
      title = title,
      rawContent = rawContent,
      langCode = langCode,
      fetchedAt = ZonedDateTime.now(),
      checksum = checksum
    )
  }
}