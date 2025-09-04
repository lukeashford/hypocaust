package com.example.the_machine.models;

import java.util.Map;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.stereotype.Component;

@Component
public class DefaultAnthropicModelBuilder extends AnthropicModelBuilder {

  public DefaultAnthropicModelBuilder(AnthropicApi api) {
    super(api);
  }

  @Override
  public String getName() {
    return "default";
  }

  @Override
  protected AnthropicChatOptions buildOptions(Map<String, String> props) {
    return createBaseOptions(props).build();
  }
}