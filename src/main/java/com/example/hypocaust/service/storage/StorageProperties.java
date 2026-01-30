package com.example.hypocaust.service.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

  /**
   * Storage provider identifier. Currently supported: "minio".
   */
  private String provider = "minio";

  /**
   * Default bucket fileName to store artifacts.
   */
  private String bucketName = "artifacts";

  private Minio minio;

  @Data
  public static class Minio {

    private String endpoint;
    private String accessKey;
    private String secretKey;
  }
}
