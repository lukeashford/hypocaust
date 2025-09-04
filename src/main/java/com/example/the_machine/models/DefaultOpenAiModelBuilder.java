package com.example.the_machine.models;

import java.util.Map;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class DefaultOpenAiModelBuilder extends OpenAiModelBuilder {

  public DefaultOpenAiModelBuilder(OpenAiApi api) {
    super(api);
  }

  @Override
  public String getName() {
    return "default";
  }

  @Override
  protected OpenAiChatOptions buildOptions(Map<String, String> props) {
    return createBaseOptions(props).build();
  }
}