package com.example.scraper.adapter.kafka

import com.example.shared.kafka.Topics
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

/**
 * Configuration for Kafka producer and topics.
 */
@Configuration
class KafkaConfig(
  @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
  private val bootstrapServers: String
) {

  /**
   * Creates a producer factory for Kafka.
   */
  @Bean
  fun producerFactory(): ProducerFactory<String, Any> {
    val props = HashMap<String, Any>()
    props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
    props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
    props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
    return DefaultKafkaProducerFactory(props)
  }

  /**
   * Creates a KafkaTemplate for sending messages.
   */
  @Bean
  fun kafkaTemplate(): KafkaTemplate<String, Any> {
    return KafkaTemplate(producerFactory())
  }

  /**
   * Creates the scrape.tasks topic.
   */
  @Bean
  fun scrapeTasksTopic(): NewTopic {
    return NewTopic(Topics.SCRAPE_TASKS, 1, 1.toShort())
  }

  /**
   * Creates the scrape.done topic.
   */
  @Bean
  fun scrapeDoneTopic(): NewTopic {
    return NewTopic(Topics.SCRAPE_DONE, 1, 1.toShort())
  }

  /**
   * Creates the scrape.error topic.
   */
  @Bean
  fun scrapeErrorTopic(): NewTopic {
    return NewTopic(Topics.SCRAPE_ERROR, 1, 1.toShort())
  }
}
