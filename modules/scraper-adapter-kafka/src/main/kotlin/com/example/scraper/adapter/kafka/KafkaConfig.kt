package com.example.scraper.adapter.kafka

import com.example.shared.contract.ScrapeCompanyCommand
import com.example.shared.kafka.Topics
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaConfig(
  @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
  private val bootstrapServers: String
) {

  /* ---------- Producer ---------- */

  @Bean
  fun producerFactory(): ProducerFactory<String, Any> =
    DefaultKafkaProducerFactory(
      mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
        JsonSerializer.ADD_TYPE_INFO_HEADERS to true
      )
    )

  @Bean
  fun kafkaTemplate(): KafkaTemplate<String, Any> = KafkaTemplate(producerFactory())

  /* ---------- Consumer ---------- */

  @Bean
  fun scrapeCompanyConsumerFactory(): ConsumerFactory<String, ScrapeCompanyCommand> {
    val valueDeser = JsonDeserializer(ScrapeCompanyCommand::class.java).apply {
      addTrustedPackages("com.example.shared.contract")
    }

    return DefaultKafkaConsumerFactory(
      mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG to "scraper-service"
      ),
      StringDeserializer(),
      valueDeser
    )
  }

  /**
   * Separate container factory keeps config explicit and avoids
   * collisions with other agents that may need different settings.
   * No additional message converter is necessary—the JsonDeserializer
   * has already produced a ScrapeCompanyCommand instance.
   */
  @Bean
  fun scrapeCompanyKafkaListenerContainerFactory():
      ConcurrentKafkaListenerContainerFactory<String, ScrapeCompanyCommand> =
    ConcurrentKafkaListenerContainerFactory<String, ScrapeCompanyCommand>().apply {
      consumerFactory = scrapeCompanyConsumerFactory()
    }

  /* ---------- Topics ---------- */

  @Bean
  fun scrapeTasksTopic(): NewTopic = NewTopic(Topics.SCRAPE_TASKS, 1, 1)

  @Bean
  fun scrapeDoneTopic(): NewTopic = NewTopic(Topics.SCRAPE_DONE, 1, 1)

  @Bean
  fun scrapeErrorTopic(): NewTopic = NewTopic(Topics.SCRAPE_ERROR, 1, 1)
}
