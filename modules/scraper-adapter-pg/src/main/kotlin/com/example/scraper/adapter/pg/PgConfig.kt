package com.example.scraper.adapter.pg

import com.example.scraper.domain.DocumentRepository
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * PostgreSQL configuration for the scraper adapter.
 */
@Configuration
class PgConfig {

  /**
   * Creates a Hikari DataSource from Spring properties.
   */
  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.hikari")
  fun hikariConfig(): HikariConfig {
    return HikariConfig()
  }

  /**
   * Creates a DataSource using the Hikari connection pool.
   */
  @Bean
  fun dataSource(hikariConfig: HikariConfig): DataSource {
    val dataSource = HikariDataSource(hikariConfig)

    // Connect Exposed to the DataSource
    Database.connect(dataSource)

    return dataSource
  }

  /**
   * Creates a DocumentRepository bean using the Exposed implementation.
   */
  @Bean
  fun documentRepository(): DocumentRepository {
    return ExposedDocumentRepository()
  }
}
