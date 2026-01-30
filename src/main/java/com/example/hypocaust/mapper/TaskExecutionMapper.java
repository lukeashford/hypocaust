package com.example.hypocaust.mapper;

import com.example.hypocaust.db.RunEntity;
import com.example.hypocaust.dto.RunDto;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface TaskExecutionMapper {

  RunDto toDto(RunEntity entity);

  RunEntity toEntity(RunDto dto);
}