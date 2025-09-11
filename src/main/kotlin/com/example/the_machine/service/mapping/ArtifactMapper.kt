package com.example.the_machine.service.mapping

import com.example.the_machine.common.Routes
import com.example.the_machine.domain.ArtifactEntity
import com.example.the_machine.dto.ArtifactDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = GlobalMapperConfig::class)
interface ArtifactMapper {

  @Mapping(target = "url", expression = "java(buildUrl(entity))")
  fun toDto(entity: ArtifactEntity): ArtifactDto

  fun buildUrl(entity: ArtifactEntity): String? {
    return if (!entity.storageKey.isNullOrBlank()) {
      "${Routes.ARTIFACTS}/${entity.id}"
    } else {
      null
    }
  }

  @Mapping(target = "storageKey", ignore = true)
  fun toEntity(dto: ArtifactDto): ArtifactEntity
}