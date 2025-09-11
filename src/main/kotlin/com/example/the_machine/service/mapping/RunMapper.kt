package com.example.the_machine.service.mapping

import com.example.the_machine.domain.RunEntity
import com.example.the_machine.dto.RunDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = GlobalMapperConfig::class)
interface RunMapper {

  @Mapping(source = "usageJson", target = "usage")
  fun toDto(entity: RunEntity): RunDto
}