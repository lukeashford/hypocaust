package com.example.hypocaust.mapper;

import com.example.hypocaust.db.TodoEntity;
import com.example.hypocaust.domain.Todo;
import java.util.UUID;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface TodoMapper {

  /**
   * Recursively maps an entity tree to a domain tree.
   */
  Todo toDomain(TodoEntity entity);

  /**
   * Recursively maps a domain tree to an entity tree. The taskExecutionId is passed down via
   * context.
   */
  @Mapping(target = "parent", ignore = true)
  @Mapping(target = "taskExecutionId", ignore = true)
  TodoEntity toEntity(Todo todo, @Context UUID taskExecutionId);

  /**
   * Links children to their parent and sets the taskExecutionId after mapping, ensuring the JPA
   * relationship is correctly established for the adjacency list.
   */
  @AfterMapping
  default void afterMapping(@MappingTarget TodoEntity entity, @Context UUID taskExecutionId) {
    entity.setTaskExecutionId(taskExecutionId);
    if (entity.getChildren() != null) {
      entity.getChildren().forEach(child -> child.setParent(entity));
    }
  }
}
