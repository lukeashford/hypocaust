package com.example.hypocaust.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SecurityIntegrationTest {

  @Container
  static MinIOContainer minio = new MinIOContainer("minio/minio:latest");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("app.storage.minio.endpoint", minio::getS3URL);
    registry.add("app.storage.minio.access-key", minio::getUserName);
    registry.add("app.storage.minio.secret-key", minio::getPassword);
    // Set a dummy issuer URI to satisfy Spring Security validation during startup
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> "http://localhost:9999");
  }

  @Autowired
  private MockMvc mockMvc;

  @Test
  void protectedEndpointsShouldReturn401() throws Exception {
    mockMvc.perform(get("/projects"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void publicEndpointsShouldBeAccessible() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/diagnostics/models"))
        .andExpect(status().isOk());
  }
}
