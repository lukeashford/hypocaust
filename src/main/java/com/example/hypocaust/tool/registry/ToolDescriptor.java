package com.example.hypocaust.tool.registry;

/**
 * Lightweight description of a discoverable tool. Returned by semantic search
 * so the decomposer knows what tools are available and what parameters they accept.
 */
public record ToolDescriptor(
    String name,
    String description,
    String parametersJson
) {

}
