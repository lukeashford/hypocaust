package com.example.the_machine.service.mapping;

import com.example.the_machine.domain.ArtifactEntity;
import com.example.the_machine.dto.ArtifactDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ArtifactMapper {

  @Mapping(target = "url", ignore = true)              // filled in controller
  @Mapping(source = "metaJson", target = "meta")
  ArtifactDTO toDTO(ArtifactEntity entity);

  @Mapping(source = "meta", target = "metaJson")
  @Mapping(target = "storageKey", ignore = true)
  ArtifactEntity toEntity(ArtifactDTO dto);
}