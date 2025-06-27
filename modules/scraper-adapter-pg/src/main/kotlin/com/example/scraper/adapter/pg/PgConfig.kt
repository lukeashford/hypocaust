package com.example.scraper.adapter.pg

import com.example.scraper.domain.DocumentRepository
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
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
  @ConfigurationProperties(prefix = "spring.datasource")
  fun dataSource(dsProps: DataSourceProperties): HikariDataSource =
    dsProps.initializeDataSourceBuilder()
      .type(HikariDataSource::class.java)
      .build()

  /**
   * Creates a DataSource using the Hikari connection pool.
   */
  @Bean
  @Primary
  fun hikariDataSource(hikariConfig: HikariConfig): DataSource {
    val ds = HikariDataSource(hikariConfig)
    Database.connect(ds)
    return ds
  }

  /**
   * Creates a DocumentRepository bean using the Exposed implementation.
   */
  @Bean
  fun documentRepository(): DocumentRepository {
    return ExposedDocumentRepository()
  }
}
