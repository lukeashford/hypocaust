package com.example.the_machine.models;

import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;

public interface ModelBuilder {

  String getName();

  ChatModel from(Map<String, String> props);
}
