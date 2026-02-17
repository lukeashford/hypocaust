package com.example.hypocaust.service.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

  /**
   * Storage provider identifier. Currently supported: "minio", "r2".
   */
  private String provider = "minio";

  /**
   * Default bucket name to store artifacts.
   */
  private String bucketName = "artifacts";

  private Minio minio;
  private R2 r2;

  @Data
  public static class Minio {

    private String endpoint;
    private String accessKey;
    private String secretKey;
  }

  @Data
  public static class R2 {

    private String accountId;
    private String accessKey;
    private String secretKey;

    public String getEndpoint() {
      if (accountId == null || accountId.isBlank()) {
        return null;
      }
      return String.format("https://%s.r2.cloudflarestorage.com", accountId);
    }
  }
}
