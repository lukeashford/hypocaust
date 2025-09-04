package com.example.the_machine.models;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

@Slf4j
public class GenericModelBuilder<M extends ChatModel, O extends ChatOptions> {

  private final Object modelBuilder;
  private final Method setOptions;
  private final Method modelBuild;
  private final Method optionsBuilderGetter;
  private final Method optionsBuild;

  public GenericModelBuilder(Class<M> modelClass, Object api, Class<O> optionsClass)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    modelBuilder = modelClass.getMethod("builder").invoke(null);
    setOptions = modelBuilder.getClass().getMethod("defaultOptions", optionsClass);
    modelBuild = modelBuilder.getClass().getMethod("build");
    setApi(api);
    optionsBuilderGetter = optionsClass.getMethod("builder");
    optionsBuild = optionsBuilderGetter.invoke(null).getClass().getMethod("build");
  }

  @SuppressWarnings("unchecked")
  public M from(Map<String, String> props)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    setOptions.invoke(modelBuilder, buildOptions(props));
    return (M) modelBuild.invoke(modelBuilder);
  }

  private void setApi(Object api)
      throws InvocationTargetException, IllegalAccessException {
    Arrays.stream(modelBuilder.getClass().getMethods())
        .filter(method -> method.getName().contains("Api")).findFirst().orElseThrow()
        .invoke(modelBuilder, api);
  }

  @SuppressWarnings("unchecked")
  private O buildOptions(Map<String, String> props)
      throws InvocationTargetException, IllegalAccessException {
    val optionsBuilder = optionsBuilderGetter.invoke(null);

    props.forEach((paramName, paramValue) -> {
      try {
        val setter = findSetter(optionsBuilder.getClass(), paramName);
        val convertedValue = convertValue(paramValue, setter.getParameterTypes()[0]);
        setter.invoke(optionsBuilder, convertedValue);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new IllegalStateException("Failed to set parameter: " + paramName, e);
      }
    });

    return (O) optionsBuild.invoke(optionsBuilder);
  }

  private Method findSetter(Class<?> builderClass, String paramName) {
    return Arrays.stream(builderClass.getMethods())
        .filter(method -> method.getName().contains(paramName))
        .filter(method -> method.getParameterCount() == 1)
        // check that the parameter type is not an enum
        .filter(method -> !method.getParameterTypes()[0].isEnum())
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("No setter found for parameter: " + paramName));
  }

  private Object convertValue(String value, Class<?> paramType) {
    log.debug("Converting value '{}' to type: {}", value, paramType);
    if (value == null || value.isBlank()) {
      return null;
    }
    if (paramType.isAssignableFrom(value.getClass())) {
      return value;
    }

    if (paramType.equals(String.class)) {
      return value;
    } else if (paramType.equals(Integer.class)) {
      return Integer.valueOf(value);
    } else if (paramType.equals(Double.class)) {
      return Double.valueOf(value);
    } else if (paramType.equals(Boolean.class)) {
      if (value.equalsIgnoreCase("true")) {
        return true;
      } else if (value.equalsIgnoreCase("false")) {
        return false;
      }
    } else if (paramType.equals(Float.class)) {
      return Float.valueOf(value);
    } else {
      throw new IllegalStateException("Unsupported parameter type: " + paramType);
    }

    return value;
  }
}
