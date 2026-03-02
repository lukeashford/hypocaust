package com.example.hypocaust.tool.creative;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.mapper.ArtifactMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolves artifact placeholder references in provider input JSON. Supports:
 * <ul>
 *   <li>{@code @artifact_name} — presigned URL (media) or description (TEXT)</li>
 *   <li>{@code @artifact_name.url} — presigned URL explicitly</li>
 *   <li>{@code @artifact_name.metadata.fieldName} — metadata field value</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArtifactResolver {

  // Matches @word_chars(.word_chars)* — longest match first due to greedy quantifier
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("@(\\w+(?:\\.\\w+)*)");

  private final ArtifactMapper artifactMapper;
  private final ObjectMapper objectMapper;

  /**
   * Resolve all artifact placeholders in the given JSON tree. Returns a new tree with placeholders
   * replaced by their resolved values.
   */
  public JsonNode resolve(JsonNode input, List<Artifact> artifacts) {
    if (input == null || artifacts == null || artifacts.isEmpty()) {
      return input;
    }

    Map<String, Artifact> byName = artifacts.stream()
        .filter(a -> a.name() != null)
        .collect(Collectors.toMap(Artifact::name, Function.identity(), (a, b) -> a));

    return walkAndResolve(input, byName);
  }

  private JsonNode walkAndResolve(JsonNode node, Map<String, Artifact> byName) {
    if (node.isTextual()) {
      String resolved = resolveText(node.asText(), byName);
      return new TextNode(resolved);
    }

    if (node.isObject()) {
      ObjectNode result = objectMapper.createObjectNode();
      var fields = node.fields();
      while (fields.hasNext()) {
        var entry = fields.next();
        result.set(entry.getKey(), walkAndResolve(entry.getValue(), byName));
      }
      return result;
    }

    if (node.isArray()) {
      ArrayNode result = objectMapper.createArrayNode();
      for (JsonNode element : node) {
        result.add(walkAndResolve(element, byName));
      }
      return result;
    }

    // Numbers, booleans, nulls — pass through unchanged
    return node;
  }

  private String resolveText(String text, Map<String, Artifact> byName) {
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
    if (!matcher.find()) {
      return text;
    }

    StringBuilder result = new StringBuilder();
    matcher.reset();
    while (matcher.find()) {
      String fullMatch = matcher.group(1); // e.g., "artifact_name.metadata.voiceId"
      String[] segments = fullMatch.split("\\.");

      String artifactName = segments[0];
      Artifact artifact = byName.get(artifactName);
      if (artifact == null) {
        // Not a known artifact — leave the placeholder as-is
        continue;
      }

      String replacement = resolveReference(artifact, segments);
      if (replacement != null) {
        matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
      }
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private String resolveReference(Artifact artifact, String[] segments) {
    if (segments.length == 1) {
      // @artifact_name → default resolution
      return resolveDefault(artifact);
    }

    String property = segments[1];

    if ("url".equals(property)) {
      // @artifact_name.url → presigned URL
      return resolveUrl(artifact);
    }

    if ("metadata".equals(property) && segments.length >= 3) {
      // @artifact_name.metadata.fieldName → metadata field value
      return resolveMetadataField(artifact, segments[2]);
    }

    log.warn("Unknown artifact path property '{}' for artifact '{}'", property, artifact.name());
    return null;
  }

  private String resolveDefault(Artifact artifact) {
    if (artifact.kind() == ArtifactKind.TEXT) {
      return artifact.description();
    }
    return resolveUrl(artifact);
  }

  private String resolveUrl(Artifact artifact) {
    if (artifact.storageKey() == null || artifact.storageKey().isBlank()) {
      log.warn("Artifact '{}' has no storage key for URL resolution", artifact.name());
      return null;
    }
    return artifactMapper.toPresignedUrl(artifact.storageKey());
  }

  private String resolveMetadataField(Artifact artifact, String fieldName) {
    JsonNode metadata = artifact.metadata();
    if (metadata == null || metadata.isNull() || !metadata.has(fieldName)) {
      log.warn("Artifact '{}' has no metadata field '{}'", artifact.name(), fieldName);
      return null;
    }
    return metadata.get(fieldName).asText();
  }
}
