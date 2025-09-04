package com.example.the_machine.models;

import java.util.Map;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class OpenAiO3ModelBuilder extends OpenAiModelBuilder {

  public OpenAiO3ModelBuilder(OpenAiApi api) {
    super(api);
  }

  @Override
  public String getName() {
    return "o3";
  }

  @Override
  protected OpenAiChatOptions buildOptions(Map<String, String> props) {
    return createBaseOptions(props)
        .maxCompletionTokens(Integer.valueOf(props.get("maxCompletionToken")))
        .build();
  }
}
