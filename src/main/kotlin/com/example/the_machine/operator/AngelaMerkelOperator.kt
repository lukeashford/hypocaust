package com.example.the_machine.operator

import com.example.the_machine.operator.result.OperatorResult
import com.example.the_machine.service.RunContext
/**
 * Operator that responds to text in the tone and style of Angela Merkel. Provides responses in her
 * characteristic diplomatic, measured, and thoughtful communication style. Currently uses mock
 * responses but designed to be enhanced with actual AI model integration.
 */
class AngelaMerkelOperator : BaseOperator(emptyList()) {

  private val toolSpec: ToolSpec = ToolSpec(
    name = "text.angela_merkel_response",
    version = "1.0.0",
    description = "Responds to input text in the tone and style of Angela Merkel, the former German Chancellor",
    inputs = listOf(
      ParamSpec.string("text", true, "Input text to respond to in Angela Merkel's style")
    ),
    outputs = listOf(
      ParamSpec.string("response", false, "Angela Merkel's response to the input text")
    ),
    metadata = mapOf(
      "tags" to listOf("text", "ai", "personality", "angela_merkel"),
      "sideEffecting" to false,
      "category" to "text_generation"
    )
  )

  override fun spec(): ToolSpec = toolSpec

  override fun getVersion(): String = "1.0.0"

  @Throws(Exception::class)
  override fun doExecute(ctx: RunContext, inputs: Map<String, Any>): OperatorResult {
    val inputText = inputs["text"] as String

    // Simulate processing time
    Thread.sleep(100)

    // Generate mock Angela Merkel-style response based on input
    val responseText = generateAngelaMerkelResponse(inputText)

    // Create outputs map
    val outputs = mapOf("response" to responseText)

    // Return successful result with the response
    return OperatorResult.success(
      "text.angela_merkel_response",
      "1.0.0",
      "Generated Angela Merkel response successfully",
      inputs,
      outputs
    )
  }

  /**
   * Generates a mock response in Angela Merkel's characteristic style.
   */
  private fun generateAngelaMerkelResponse(inputText: String): String {
    // Characteristic Angela Merkel phrases and approach
    val merkelPhrases = listOf(
      "Wir schaffen das",
      "We must work together",
      "This requires careful consideration",
      "We need a European solution",
      "It is important that we build consensus",
      "We must be pragmatic but principled"
    )

    val lowercaseInput = inputText.lowercase()

    // Different response patterns based on input content
    return when {
      "europe" in lowercaseInput || "eu" in lowercaseInput -> {
        "European unity has always been at the heart of my political conviction. " +
            "${merkelPhrases[3]}. We must strengthen our common values and work together " +
            "to address the challenges facing our continent."
      }

      "crisis" in lowercaseInput || "problem" in lowercaseInput -> {
        "${merkelPhrases[2]} and a methodical approach. ${merkelPhrases[0]} " +
            "- we can overcome this challenge through cooperation and determination."
      }

      "decision" in lowercaseInput || "choice" in lowercaseInput -> {
        "${merkelPhrases[4]} on this matter. ${merkelPhrases[5]}, " +
            "weighing all options carefully before moving forward."
      }

      else -> {
        "Thank you for raising this important point. ${merkelPhrases[1]} " +
            "to find sustainable solutions that serve the common good. " +
            "As I have always said, ${merkelPhrases[0]}."
      }
    }
  }
}