package com.example.the_machine.mapper;

import com.example.the_machine.db.MessageEntity;
import com.example.the_machine.dto.MessageOutgoingDto;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface MessageMapper {

  MessageOutgoingDto toDto(MessageEntity entity);
}