package com.example.the_machine.mapper;

import com.example.the_machine.db.ThreadEntity;
import com.example.the_machine.dto.ThreadDto;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface ThreadMapper {

  ThreadDto toDto(ThreadEntity entity);

  ThreadEntity toEntity(ThreadDto dto);
}