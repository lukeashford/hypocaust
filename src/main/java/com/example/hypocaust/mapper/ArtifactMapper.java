package com.example.hypocaust.mapper;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.dto.ArtifactDto;
import com.example.hypocaust.dto.ArtifactMetadataDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ArtifactMapper {

  @Mapping(target = "url", expression = "java(buildUrl(entity))")
  ArtifactDto toDto(ArtifactEntity entity);

  ArtifactMetadataDto toMetadataDto(ArtifactEntity entity);

  default String buildUrl(ArtifactEntity entity) {
    if (entity.getStorageKey() != null && !entity.getStorageKey().trim().isEmpty()) {
      return Routes.ARTIFACTS + "/" + entity.getId();
    }
    return null;
  }

  @Mapping(target = "storageKey", ignore = true)
  ArtifactEntity toEntity(ArtifactDto dto);
}