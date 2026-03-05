package com.example.hypocaust.mapper;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.Artifact;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface ArtifactMapper {

  Artifact toDomain(ArtifactEntity entity);

  ArtifactEntity toEntity(Artifact artifact, UUID projectId, UUID taskExecutionId);
}
