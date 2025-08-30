package com.example.the_machine.service.mapping;

import com.example.the_machine.domain.ThreadEntity;
import com.example.the_machine.dto.ThreadDto;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface ThreadMapper {

  ThreadDto toDto(ThreadEntity entity);

  ThreadEntity toEntity(ThreadDto dto);
}