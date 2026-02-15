package com.example.hypocaust.config;

import com.example.hypocaust.models.enums.OpenAiChatModelSpec;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class OpenAiChatModelSpecConverter implements Converter<String, OpenAiChatModelSpec> {

  @Override
  public OpenAiChatModelSpec convert(@NonNull String source) {
    try {
      return OpenAiChatModelSpec.fromString(source);
    } catch (Throwable e) {
      throw new IllegalArgumentException("Invalid OpenAI chat model: " + source, e);
    }
  }
}
