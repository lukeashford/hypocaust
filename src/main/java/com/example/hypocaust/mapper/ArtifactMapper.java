package com.example.hypocaust.mapper;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.dto.ArtifactDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ArtifactMapper {

  ArtifactDto toDto(ArtifactEntity entity);

  @Mapping(target = "storageKey", ignore = true)
  ArtifactEntity toEntity(ArtifactDto dto);
}