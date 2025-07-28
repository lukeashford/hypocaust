package com.example.the_machine.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for shared HttpClient used throughout the application.
 */
@Configuration
public class HttpClientConfiguration {

  /**
   * Shared HttpClient bean for all HTTP operations in the application.
   */
  @Bean
  public HttpClient httpClient() {
    return HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(5))
        .executor(Executors.newFixedThreadPool(20))
        .build();
  }
}