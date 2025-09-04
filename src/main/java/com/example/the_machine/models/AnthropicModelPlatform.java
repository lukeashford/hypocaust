package com.example.the_machine.models;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.stereotype.Component;

@Component
public class AnthropicModelPlatform extends ModelPlatform {

//  private final List<AnthropicModelBuilder> modelBuilders;

  private final GenericModelBuilder<AnthropicChatModel, AnthropicChatOptions> modelBuilder;

  public AnthropicModelPlatform(AnthropicApi api)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    modelBuilder = new GenericModelBuilder<>(AnthropicChatModel.class, api,
        AnthropicChatOptions.class);
  }

  @Override
  public String getName() {
    return "anthropic";
  }

  @Override
  protected List<ModelBuilder> getAllBuilders() {
    return List.of();
  }

  @Override
  public GenericModelBuilder<?, ?> getGenericBuilder() {
    return modelBuilder;
  }
}