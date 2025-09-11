package com.example.the_machine.service.mapping

import com.example.the_machine.domain.ThreadEntity
import com.example.the_machine.dto.ThreadDto
import org.mapstruct.Mapper

@Mapper(config = GlobalMapperConfig::class)
interface ThreadMapper {

  fun toDto(entity: ThreadEntity): ThreadDto

  fun toEntity(dto: ThreadDto): ThreadEntity
}