package com.example.the_machine.operator;

import com.example.the_machine.models.ModelRegistry;
import com.example.the_machine.models.enums.OpenAiChatModelSpec;
import com.example.the_machine.operator.result.OperatorResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NumberTriviaOperator extends BaseOperator {

  private final ModelRegistry modelRegistry;

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    Integer number = (Integer) normalizedInputs.get("number");

    ChatClient chatClient = ChatClient.builder(
            modelRegistry.get(OpenAiChatModelSpec.GPT_4O))
        .build();

    String prompt = String.format(
        "Tell me an interesting fun fact about the number %d. " +
            "Keep it concise and engaging, around 1-2 sentences.",
        number);

    String trivia = chatClient.prompt()
        .user(prompt)
        .call()
        .content();

    Map<String, Object> outputs = Map.of("trivia", trivia);

    return OperatorResult.success(
        "Successfully retrieved number trivia",
        normalizedInputs,
        outputs
    );
  }

  @Override
  public OperatorSpec spec() {
    return new OperatorSpec(
        "NumberTrivia",
        "1.0.0",
        "Provides fun facts about a given number using AI",
        List.of(
            ParamSpec.integer("number", "Integer to get trivia about", true)
        ),
        List.of(
            ParamSpec.string("trivia", "Fun fact about the number", true)
        )
    );
  }
}