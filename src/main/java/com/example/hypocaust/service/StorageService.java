package com.example.hypocaust.service;

import com.example.hypocaust.exception.StorageException;
import java.io.InputStream;

/**
 * Abstraction for file storage operations. Implementations can use MinIO, S3, local filesystem,
 * etc.
 */
public interface StorageService {

  /**
   * Store a file and return its storage key.
   *
   * @param data the file data as byte array
   * @param contentType the MIME type (e.g., "image/png")
   * @return storage key that can be used to retrieve the file later
   */
  String store(byte[] data, String contentType);

  /**
   * Store a file from an InputStream.
   *
   * @param inputStream the input stream containing file data
   * @param contentLength the size of the data in bytes
   * @param contentType the MIME type
   * @return storage key that can be used to retrieve the file later
   */
  String store(InputStream inputStream, long contentLength, String contentType);

  /**
   * Fetch a file's contents from storage.
   *
   * @param storageKey the storage key
   * @return the file contents as a byte array
   * @throws StorageException if the fetch fails
   */
  byte[] fetch(String storageKey);

  /**
   * Delete a file from storage.
   *
   * @param storageKey the storage key
   * @throws StorageException if deletion fails
   */
  void delete(String storageKey);

  /**
   * Generate a pre-signed URL for direct access to a file. Useful for serving files directly to
   * clients without proxying.
   *
   * @param storageKey the storage key
   * @param expirySeconds how long the URL should be valid
   * @return pre-signed URL
   */
  String generatePresignedUrl(String storageKey, int expirySeconds);

}
