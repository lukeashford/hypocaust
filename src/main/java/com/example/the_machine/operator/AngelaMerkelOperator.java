package com.example.the_machine.operator;

import com.example.the_machine.operator.result.OperatorResult;
import com.example.the_machine.service.RunContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Operator that responds to text in the tone and style of Angela Merkel. Provides responses in her
 * characteristic diplomatic, measured, and thoughtful communication style. Currently uses mock
 * responses but designed to be enhanced with actual AI model integration.
 */
public class AngelaMerkelOperator extends BaseOperator {

  private final ToolSpec toolSpec;

  public AngelaMerkelOperator() {
    super(new ObjectMapper(), Collections.emptyList());

    this.toolSpec = ToolSpec.builder()
        .name("text.angela_merkel_response")
        .version("1.0.0")
        .description(
            "Responds to input text in the tone and style of Angela Merkel, the former German Chancellor")
        .inputs(List.of(
            ParamSpec.string("text", true, "Input text to respond to in Angela Merkel's style")
        ))
        .outputs(List.of(
            ParamSpec.string("response", false, "Angela Merkel's response to the input text")
        ))
        .metadata(Map.of(
            "tags", List.of("text", "ai", "personality", "angela_merkel"),
            "sideEffecting", false,
            "category", "text_generation"
        ))
        .build();
  }

  @Override
  public ToolSpec spec() {
    return toolSpec;
  }

  @Override
  protected String getVersion() {
    return "1.0.0";
  }

  @Override
  protected OperatorResult doExecute(RunContext ctx, Map<String, Object> inputs) throws Exception {
    final var inputText = (String) inputs.get("text");

    // Simulate processing time
    Thread.sleep(100);

    // Generate mock Angela Merkel-style response based on input
    String responseText = generateAngelaMerkelResponse(inputText);

    // Create outputs map
    final var outputs = Map.of("response", (Object) responseText);

    // Return successful result with the response
    return OperatorResult.success(
        "text.angela_merkel_response",
        "1.0.0",
        "Generated Angela Merkel response successfully",
        inputs,
        outputs
    );
  }

  /**
   * Generates a mock response in Angela Merkel's characteristic style.
   */
  private String generateAngelaMerkelResponse(String inputText) {
    // Characteristic Angela Merkel phrases and approach
    final var merkelPhrases = List.of(
        "Wir schaffen das",
        "We must work together",
        "This requires careful consideration",
        "We need a European solution",
        "It is important that we build consensus",
        "We must be pragmatic but principled"
    );

    // Different response patterns based on input content
    if (inputText.toLowerCase().contains("europe") || inputText.toLowerCase().contains("eu")) {
      return "European unity has always been at the heart of my political conviction. " +
          merkelPhrases.get(3) + ". We must strengthen our common values and work together " +
          "to address the challenges facing our continent.";
    } else if (inputText.toLowerCase().contains("crisis") || inputText.toLowerCase()
        .contains("problem")) {
      return merkelPhrases.get(2) + " and a methodical approach. " + merkelPhrases.get(0) +
          " - we can overcome this challenge through cooperation and determination.";
    } else if (inputText.toLowerCase().contains("decision") || inputText.toLowerCase()
        .contains("choice")) {
      return merkelPhrases.get(4) + " on this matter. " + merkelPhrases.get(5) +
          ", weighing all options carefully before moving forward.";
    } else {
      return "Thank you for raising this important point. " + merkelPhrases.get(1) +
          " to find sustainable solutions that serve the common good. " +
          "As I have always said, " + merkelPhrases.get(0) + ".";
    }
  }
}