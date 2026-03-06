package com.example.hypocaust.tool.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as eligible for semantic discovery via {@link SemanticSearchToolRegistry}.
 *
 * <p>Must be used alongside {@link org.springframework.ai.tool.annotation.Tool} — that
 * annotation owns the tool name, description, and schema. This one is purely a registry membership
 * marker.
 *
 * <pre>{@code
 * @DiscoverableTool
 * @Tool(name = "my_tool", description = "Does something useful")
 * public String run(@ToolParam(description = "input") String input) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DiscoverableTool {

}