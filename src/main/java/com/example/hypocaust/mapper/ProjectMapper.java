package com.example.hypocaust.mapper;

import com.example.hypocaust.db.ProjectEntity;
import com.example.hypocaust.dto.ProjectResponseDto;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface ProjectMapper {

  ProjectResponseDto toDto(ProjectEntity entity);
}
