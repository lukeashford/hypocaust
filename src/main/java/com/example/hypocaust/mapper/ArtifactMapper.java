package com.example.hypocaust.mapper;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.dto.ArtifactDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ArtifactMapper {

  @Mapping(target = "url", expression = "java(entity.getStorageKey() != null ? \"/artifacts/\" + entity.getId() + \"/content\" : null)")
  @Mapping(target = "isPending", constant = "false")
  ArtifactDto toDto(ArtifactEntity entity);
}