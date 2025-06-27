package com.example.scraper.adapter.pg

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Exposed table definition for the source_doc table.
 */
object SourceDocTable : Table("source_doc") {

  val id = uuid("id").autoGenerate()
  val companyId = uuid("company_id")
  val crawlJobId = uuid("crawl_job_id").nullable()
  val sourceType = varchar("source_type", 20)
  val externalId = varchar("external_id", 500).nullable()
  val url = varchar("url", 2000).nullable()
  val title = text("title").nullable()
  val rawContent = text("raw_content").nullable()
  val langCode = varchar("lang_code", 5).nullable()
  val fetchedAt =
    timestampWithTimeZone("fetched_at").clientDefault { OffsetDateTime.now(ZoneOffset.UTC) }
  val checksum = varchar("checksum", 64).nullable()

  override val primaryKey = PrimaryKey(id)
}
