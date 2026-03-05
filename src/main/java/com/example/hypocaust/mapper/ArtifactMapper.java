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

  protected StorageService storageService;

  @Autowired
  public void setStorageService(StorageService storageService) {
    this.storageService = storageService;
  }

  @Mapping(target = "url", source = "storageKey", qualifiedByName = "presignedUrl")
  public abstract Artifact toDomain(ArtifactEntity entity);

  public abstract ArtifactEntity toEntity(Artifact artifact, UUID projectId, UUID taskExecutionId);

  /**
   * Generate a presigned URL for a storage key. Returns null if the key is null/blank.
   */
  @Named("presignedUrl")
  public String toPresignedUrl(String storageKey) {
    if (storageKey == null || storageKey.isBlank() || storageService == null) {
      return null;
    }
    return storageService.generatePresignedUrl(storageKey, 600);
  }
}
