package com.example.hypocaust.mapper;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.dto.TaskExecutionDto;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface TaskExecutionMapper {

  TaskExecutionDto toDto(TaskExecutionEntity entity);

  TaskExecutionEntity toEntity(TaskExecutionDto dto);
}