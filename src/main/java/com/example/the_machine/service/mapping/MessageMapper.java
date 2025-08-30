package com.example.the_machine.service.mapping;

import com.example.the_machine.domain.MessageEntity;
import com.example.the_machine.dto.MessageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class, uses = JsonConverters.class)
public interface MessageMapper {

  @Mapping(source = "contentJson", target = "content", qualifiedByName = "blocksFromJson")
  @Mapping(source = "attachmentsJson", target = "attachments", qualifiedByName = "uuidsFromJson")
  MessageDto toDto(MessageEntity entity);

  @Mapping(source = "content", target = "contentJson", qualifiedByName = "blocksToJson")
  @Mapping(source = "attachments", target = "attachmentsJson", qualifiedByName = "uuidsToJson")
  MessageEntity toEntity(MessageDto dto);
}