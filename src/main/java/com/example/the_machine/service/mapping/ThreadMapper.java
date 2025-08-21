package com.example.the_machine.service.mapping;

import com.example.the_machine.domain.ThreadEntity;
import com.example.the_machine.dto.ThreadDTO;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface ThreadMapper {

  ThreadDTO toDTO(ThreadEntity entity);

  ThreadEntity toEntity(ThreadDTO dto);
}