package com.example.the_machine.mapper;

import com.example.the_machine.common.Routes;
import com.example.the_machine.db.ArtifactEntity;
import com.example.the_machine.dto.ArtifactDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ArtifactMapper {

  @Mapping(target = "url", expression = "java(buildUrl(entity))")
  ArtifactDto toDto(ArtifactEntity entity);

  default String buildUrl(ArtifactEntity entity) {
    if (entity.getStorageKey() != null && !entity.getStorageKey().trim().isEmpty()) {
      return Routes.ARTIFACTS + "/" + entity.getId();
    }
    return null;
  }

  @Mapping(target = "storageKey", ignore = true)
  ArtifactEntity toEntity(ArtifactDto dto);
}