package com.example.the_machine.models;

import java.util.Map;

/**
 * Base interface for all model provider properties.
 */
public interface ProviderProperties {

  String apiKey();

  Map<String, ModelProps> models();
}