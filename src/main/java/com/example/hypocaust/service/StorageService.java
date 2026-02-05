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
   * Retrieve a file as an InputStream.
   *
   * @param storageKey the key returned from store()
   * @return input stream of the file data
   * @throws StorageException if file not found or retrieval fails
   */
  InputStream retrieve(String storageKey);

  /**
   * Get metadata about a stored file.
   *
   * @param storageKey the storage key
   * @return file metadata
   * @throws StorageException if file not found
   */
  FileMetadata getMetadata(String storageKey);

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

  /**
   * Manifest a URL - if already local (a storage key), return as-is. If external, download, hash,
   * store (dedup by hash), return local storage key.
   *
   * @param url the URL to manifest (can be external URL or local storage key)
   * @param contentType the MIME type for storage
   * @return local storage key
   */
  String manifestUrl(String url, String contentType);

  /**
   * Metadata about a stored file.
   */
  record FileMetadata(
      String storageKey,
      String contentType,
      long size,
      String etag
  ) {

  }
}
