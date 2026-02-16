package com.example.hypocaust.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

  private String hostUrl;
  private Cors cors = new Cors();

  @Data
  public static class Cors {

    private List<String> allowedOrigins = List.of("http://localhost:3000");
  }
}
