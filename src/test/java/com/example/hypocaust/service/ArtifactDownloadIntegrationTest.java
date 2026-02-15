package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.mapper.ArtifactMapper;
import com.example.hypocaust.repo.ArtifactRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArtifactDownloadIntegrationTest {

  private HttpServer server;
  private int port;
  private ArtifactService artifactService;
  private StorageService storageService;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/test", exchange -> {
      byte[] response = "test content".getBytes();
      exchange.getResponseHeaders().set("Content-Type", "image/webp");
      exchange.sendResponseHeaders(200, response.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response);
      }
    });
    server.start();
    port = server.getAddress().getPort();

    ArtifactRepository artifactRepository = mock(ArtifactRepository.class);
    storageService = mock(StorageService.class);
    ArtifactMapper artifactMapper = mock(ArtifactMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();
    artifactService = new ArtifactService(artifactRepository, storageService, artifactMapper,
        objectMapper);
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void shouldDownloadWithMimeTypeAndContentLength() throws Exception {
    Artifact pendingArtifact = Artifact.builder()
        .name("test-artifact")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.CREATED)
        .url("http://localhost:" + port + "/test")
        .title("Title")
        .description("Desc")
        .build();

    when(storageService.store(any(byte[].class), any(String.class))).thenReturn("blobs/hash.webp");

    java.lang.reflect.Method method = ArtifactService.class.getDeclaredMethod("downloadArtifact",
        Artifact.class);
    method.setAccessible(true);
    Artifact result = (Artifact) method.invoke(artifactService, pendingArtifact);

    assertThat(result.status()).isEqualTo(ArtifactStatus.MANIFESTED);
    assertThat(result.url()).isEqualTo("blobs/hash.webp");
    assertThat(result.mimeType()).isEqualTo("image/webp");
    assertThat(result.metadata().get("contentLength").asLong()).isEqualTo("test content".length());
  }
}
