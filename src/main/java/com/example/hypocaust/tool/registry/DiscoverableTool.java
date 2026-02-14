package com.example.hypocaust.tool.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * Marks a Spring bean as a discoverable tool. The tool will be automatically
 * indexed by the {@link SemanticSearchToolRegistry} and made available for
 * semantic search and invocation via the execute_tool bridge.
 *
 * <p>The annotated class must also contain at least one method annotated with
 * {@link org.springframework.ai.tool.annotation.Tool}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface DiscoverableTool {

  String name();

  String description();
}
