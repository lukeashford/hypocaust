package com.example.the_machine.mapper;

import com.example.the_machine.db.RunEntity;
import com.example.the_machine.dto.RunDto;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface RunMapper {

  RunDto toDto(RunEntity entity);

  RunEntity toEntity(RunDto dto);
}