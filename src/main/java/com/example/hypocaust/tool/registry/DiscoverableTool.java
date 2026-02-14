package com.example.hypocaust.tool.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * Marks a Spring bean or method as a discoverable tool.
 *
 * <p>When used on a class (TYPE), the class becomes a Spring bean and the registry
 * will attempt to find a primary tool method if none are explicitly marked.
 *
 * <p>When used on a method (METHOD), it acts as a replacement for @Tool while
 * marking the method for semantic discovery.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Component
@Tool
public @interface DiscoverableTool {

  @AliasFor(annotation = Tool.class, attribute = "name")
  String name() default "";

  @AliasFor(annotation = Tool.class, attribute = "description")
  String description() default "";
}
