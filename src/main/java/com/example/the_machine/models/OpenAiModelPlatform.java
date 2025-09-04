package com.example.the_machine.models;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class OpenAiModelPlatform extends ModelPlatform {

  private final GenericModelBuilder<OpenAiChatModel, OpenAiChatOptions> modelBuilder;

  public OpenAiModelPlatform(OpenAiApi api)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    modelBuilder = new GenericModelBuilder<>(OpenAiChatModel.class, api, OpenAiChatOptions.class);
  }

  @Override
  public String getName() {
    return "openai";
  }

  @Override
  public List<ModelBuilder> getAllBuilders() {
    return List.of();
  }

  @Override
  public GenericModelBuilder<?, ?> getGenericBuilder() {
    return modelBuilder;
  }
}
