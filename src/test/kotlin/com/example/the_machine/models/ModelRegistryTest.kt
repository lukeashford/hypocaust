package com.example.the_machine.models

import com.example.the_machine.models.enums.AnthropicChatModelSpec
import com.example.the_machine.models.enums.OpenAiChatModelSpec
import com.example.the_machine.models.enums.OpenAiEmbeddingModelSpec
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.context.ApplicationContext

@ExtendWith(MockKExtension::class)
class ModelRegistryTest {

  @MockK(relaxed = true)
  private lateinit var applicationContext: ApplicationContext

  @MockK(relaxed = true)
  @Suppress("unused")
  private lateinit var modelProperties: ModelProperties

  @MockK(relaxed = true)
  @Suppress("unused")
  private lateinit var modelFactory: ModelFactory

  @InjectMockKs
  private lateinit var modelRegistry: ModelRegistry

  @Test
  fun testTypeSafeEnumBasedAccess() {
    // Mock the application context to return specific model instances
    val mockOpenAiChatModel = mockk<OpenAiChatModel>()
    val mockOpenAiEmbeddingModel = mockk<OpenAiEmbeddingModel>()
    val mockAnthropicChatModel = mockk<AnthropicChatModel>()

    every {
      applicationContext.getBean(
        "gpt-4o",
        OpenAiChatModel::class.java
      )
    } returns mockOpenAiChatModel
    every {
      applicationContext.getBean(
        "text-embedding-3-small",
        OpenAiEmbeddingModel::class.java
      )
    } returns mockOpenAiEmbeddingModel
    every {
      applicationContext.getBean(
        "claude-sonnet-4-latest",
        AnthropicChatModel::class.java
      )
    } returns mockAnthropicChatModel

    // Test OpenAI chat model access
    val retrievedOpenAiChat = modelRegistry.get(OpenAiChatModelSpec.GPT_4O)
    assertThat(retrievedOpenAiChat).isEqualTo(mockOpenAiChatModel)

    // Test OpenAI embedding model access
    val retrievedOpenAiEmbedding =
      modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL)
    assertThat(retrievedOpenAiEmbedding).isEqualTo(mockOpenAiEmbeddingModel)

    // Test Anthropic chat model access
    val retrievedAnthropicChat = modelRegistry.get(AnthropicChatModelSpec.CLAUDE_SONNET_4_LATEST)
    assertThat(retrievedAnthropicChat).isEqualTo(mockAnthropicChatModel)
  }

  @Test
  fun testStringBasedAccess() {
    // Mock the application context to return a generic ChatModel
    val mockChatModel = mockk<ChatModel>()
    every { applicationContext.getBean("gpt-4o", ChatModel::class.java) } returns mockChatModel

    // Test string-based access
    val retrievedModel = modelRegistry.get("gpt-4o")
    assertThat(retrievedModel).isEqualTo(mockChatModel)
  }
}