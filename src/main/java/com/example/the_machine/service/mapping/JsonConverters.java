package com.example.the_machine.service.mapping;

import com.example.the_machine.dto.ContentBlock;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class JsonConverters {

  private static final TypeReference<List<ContentBlock>> BLOCK_LIST = new TypeReference<>() {
  };
  private final ObjectMapper om;

  public JsonConverters(ObjectMapper om) {
    this.om = om;
  }

  @Named("blocksFromJson")
  public List<ContentBlock> blocksFromJson(JsonNode node) {
    if (node == null || node.isNull()) {
      return List.of(); // DB NULL or JSON null => empty list in DTO
    }
    if (!node.isArray()) {
      throw new IllegalArgumentException("content_json must be a JSON array");
    }
    return om.convertValue(node, BLOCK_LIST); // honors @JsonTypeInfo/@JsonSubTypes
  }

  @Named("blocksToJson")
  public JsonNode blocksToJson(List<ContentBlock> blocks) {
    if (blocks == null) {
      return null;
    }
    return om.valueToTree(blocks);
  }

  @Named("uuidsFromJson")
  public List<UUID> uuidsFromJson(JsonNode node) {
    if (node == null || node.isNull()) {
      return List.of();
    }
    if (!node.isArray()) {
      throw new IllegalArgumentException("attachments_json must be a JSON array");
    }
    List<UUID> out = new ArrayList<>(node.size());
    node.forEach(n -> out.add(UUID.fromString(n.asText())));
    return out;
  }

  @Named("uuidsToJson")
  public JsonNode uuidsToJson(List<UUID> ids) {
    if (ids == null) {
      return null;
    }
    return om.valueToTree(ids);
  }
}