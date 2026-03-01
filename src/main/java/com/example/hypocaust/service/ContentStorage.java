package com.example.hypocaust.service;

import org.springframework.http.MediaType;

/**
 * Simple abstraction to persist binary content and return a fetchable URL.
 */
public interface ContentStorage {

  /**
   * Store the given data and return an access URL.
   *
   * @param filename desired filename (used to derive extension only; storage may ignore the name)
   * @param data content bytes
   * @param type media type for content-type
   * @return URL string to access the stored content
   */
  String put(String filename, byte[] data, MediaType type);
}
