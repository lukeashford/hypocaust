package com.example.scraper.service

import com.example.shared.contract.ScrapeCompanyCommand
import com.example.shared.contract.SourceType
import com.example.shared.kafka.Topics
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Integration test for the Scraper service that:
 * - Spins up Postgres 16 and Kafka containers
 * - Overrides Spring properties for the containers
 * - Publishes a ScrapeCompanyCommand JSON to topic Topics.SCRAPE_TASKS
 * - Waits until source_doc row count ≥ 1 and one message received on Topics.SCRAPE_DONE
 */
@SpringBootTest(classes = [ScraperServiceApplication::class])
@Testcontainers
@ContextConfiguration(initializers = [ScraperE2E.Initializer::class])
class ScraperE2E @Autowired constructor(
  private val jdbcTemplate: JdbcTemplate,
  private val kafkaTemplate: KafkaTemplate<String, Any>
) {

  companion object {

    @Container
    val postgresContainer = PostgreSQLContainer<Nothing>("postgres:16")
      .apply {
        withDatabaseName("testdb")
        withUsername("test")
        withPassword("test")
      }

    @Container
    val kafkaContainer = KafkaContainer(
      DockerImageName.parse("confluentinc/cp-kafka:${System.getProperty("kafkaPlatform", "7.6.1")}")
    )
      .withEmbeddedZookeeper()
  }

  class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
      TestPropertyValues.of(
        "spring.datasource.url=${postgresContainer.jdbcUrl}",
        "spring.datasource.username=${postgresContainer.username}",
        "spring.datasource.password=${postgresContainer.password}",
        "spring.kafka.bootstrap-servers=${kafkaContainer.bootstrapServers}"
      ).applyTo(applicationContext.environment)
    }
  }

  @Test
  fun testScrapeCompanyFlow() {
    // Verify that the database is accessible and the source_doc table exists
    val tables = jdbcTemplate.queryForList(
      "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
      String::class.java
    )
    assert(tables.contains("source_doc")) { "source_doc table not found in database" }

    // Create a ScrapeCompanyCommand
    val companyId = UUID.randomUUID()
    val command = ScrapeCompanyCommand(
      companyId = companyId,
      companyName = "Luke Ashford",
      seedUrls = listOf("https://lukeashford.com"),
      sourceTypes = setOf(SourceType.Web),
      priority = 5,
      maxDocuments = 10,
      forceRefresh = false
    )

    // Create a Kafka consumer for the SCRAPE_DONE topic
    val consumerProps = Properties().apply {
      put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
      put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer")
      put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
      put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java.name)
      put(JsonDeserializer.TRUSTED_PACKAGES, "*")
    }

    KafkaConsumer<String, Any>(consumerProps).use { consumer ->
      // Subscribe to the SCRAPE_DONE topic
      consumer.subscribe(Collections.singletonList(Topics.SCRAPE_DONE))

      // Publish the command to the SCRAPE_TASKS topic
      kafkaTemplate.send(Topics.SCRAPE_TASKS, companyId.toString(), command).get()
      println("Published command to ${Topics.SCRAPE_TASKS} topic: $command")

      // Wait for a row to be inserted into the source_doc table and a message to be received on the SCRAPE_DONE topic
      var messageReceived = false

      Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(Duration.ofSeconds(1))
        .untilAsserted {
          // Check if a row was inserted into the source_doc table
          val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM source_doc WHERE company_id = ?",
            Int::class.java,
            companyId
          ) ?: 0

          // Check if a message was received on the SCRAPE_DONE topic
          if (!messageReceived) {
            val records = consumer.poll(Duration.ofMillis(100))
            for (record in records) {
              println("Received message on ${Topics.SCRAPE_DONE} topic: ${record.value()}")
              messageReceived = true
              break
            }
          }

          // Assert that at least one row was inserted and a message was received on the SCRAPE_DONE topic
          assert(count >= 1) { "No rows found in source_doc table for company ID: $companyId" }
          assert(messageReceived) { "No message received on ${Topics.SCRAPE_DONE} topic" }
        }

      println("Test completed successfully!")
    }
  }
}
