package com.example.hypocaust.mapper;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.Artifact;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ArtifactMapper {

  @Mapping(target = "url", expression = "java(entity.getStorageKey() != null ? \"/artifacts/\" + entity.getId() + \"/content\" : null)")
  Artifact toDomain(ArtifactEntity entity);
}