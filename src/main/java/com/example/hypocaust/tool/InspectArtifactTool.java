package com.example.hypocaust.tool;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.service.ArtifactChunker;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class InspectArtifactTool {

  private static final int PREVIEW_LENGTH = 200;

  @Tool(name = "inspect_artifact", description = """
      Return the full metadata for a named artifact: kind, status, generation model, generation \
      prompt, provider-specific inputs (e.g. voice_id, spoken text for audio), dimensions, \
      mime type, and error message if failed.
      For large text fields (script content, long spoken text), returns a 200-character preview \
      and the total size. Use search_project to find specific content within those large fields.""")
  public String inspect(@ToolParam(description = "Artifact name") String name) {
    var artifact = TaskExecutionContextHolder.getContext().getArtifacts().get(name)
        .orElse(null);
    if (artifact == null) {
      return "Artifact not found: " + name;
    }
    return format(artifact);
  }

  private String format(Artifact artifact) {
    var sb = new StringBuilder();
    sb.append("name: ").append(artifact.name()).append('\n');
    sb.append("kind: ").append(artifact.kind()).append('\n');
    sb.append("status: ").append(artifact.status()).append('\n');
    sb.append("title: ").append(artifact.title()).append('\n');
    sb.append("description: ").append(artifact.description()).append('\n');

    if (artifact.mimeType() != null) {
      sb.append("mimeType: ").append(artifact.mimeType()).append('\n');
    }
    if (artifact.errorMessage() != null) {
      sb.append("errorMessage: ").append(artifact.errorMessage()).append('\n');
    }

    if (artifact.inlineContent() != null) {
      String text = artifact.inlineContent().isTextual()
          ? artifact.inlineContent().asText()
          : artifact.inlineContent().toString();
      appendField(sb, "inlineContent", text);
    }

    if (artifact.metadata() != null) {
      formatJson(sb, "metadata", artifact.metadata());
    }

    return sb.toString();
  }

  private void formatJson(StringBuilder sb, String path, JsonNode node) {
    if (node.isTextual()) {
      appendField(sb, path, node.asText());
    } else if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        formatJson(sb, path + "." + field.getKey(), field.getValue());
      }
    } else if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        formatJson(sb, path + "[" + i + "]", node.get(i));
      }
    } else {
      sb.append(path).append(": ").append(node.asText()).append('\n');
    }
  }

  private void appendField(StringBuilder sb, String path, String value) {
    if (value.length() > ArtifactChunker.CHUNK_THRESHOLD) {
      String preview = value.substring(0, PREVIEW_LENGTH);
      sb.append(path)
          .append(" (").append(value.length()).append(" chars): \"")
          .append(preview).append("…\"")
          .append(" [use search_project to access full content]\n");
    } else {
      sb.append(path).append(": ").append(value).append('\n');
    }
  }
}
