package com.example.hypocaust.mapper;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.service.StorageService;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(config = GlobalMapperConfig.class)
public abstract class ArtifactMapper {

  @Autowired
  protected StorageService storageService;

  @Mapping(source = "storageKey", target = "url", qualifiedByName = "externalizeUrl")
  public abstract Artifact toDomain(ArtifactEntity entity);

  @Mapping(source = "artifact.url", target = "storageKey")
  public abstract ArtifactEntity toEntity(Artifact artifact, UUID projectId, UUID taskExecutionId);

  @Named("externalizeUrl")
  protected String externalizeUrl(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }
    return url.startsWith("blobs/")
        ? storageService.generatePresignedUrl(url, 600)
        : url;
  }
}