package com.example.the_machine.common

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Instant
import java.util.*

/**
 * Custom serializer for UUID type.
 */
object UUIDSerializer : KSerializer<UUID> {

  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

/**
 * Custom serializer for Instant type.
 */
object InstantSerializer : KSerializer<Instant> {

  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

/**
 * Kotlin Serialization configuration with Spring Boot integration.
 * Provides global snake_case naming and consistent JSON handling.
 */
@OptIn(ExperimentalSerializationApi::class)
@Configuration
class KotlinSerializationConfig : WebMvcConfigurer {

  @Bean
  fun kotlinJson(): Json = staticJson

  override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
    converters.add(KotlinSerializationJsonHttpMessageConverter(kotlinJson()))
    super.configureMessageConverters(converters)
  }

  companion object {

    /**
     * Static Json instance for entities and places where DI isn't available.
     * Uses the same configuration as the Spring bean.
     */
    @JvmStatic
    val staticJson: Json = Json {
      namingStrategy = JsonNamingStrategy.SnakeCase
      ignoreUnknownKeys = true
      explicitNulls = false
      prettyPrint = false
      classDiscriminator = "#type"

      serializersModule = SerializersModule {
        contextual(UUIDSerializer)
        contextual(InstantSerializer)
      }
    }
  }
}