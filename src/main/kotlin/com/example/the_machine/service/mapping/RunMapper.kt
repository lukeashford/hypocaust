package com.example.the_machine.service.mapping

import com.example.the_machine.domain.RunEntity
import com.example.the_machine.dto.RunDto
import org.mapstruct.Mapper

@Mapper(config = GlobalMapperConfig::class)
interface RunMapper {

  fun toDto(entity: RunEntity): RunDto
}