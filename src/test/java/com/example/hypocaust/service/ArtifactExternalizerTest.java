package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.dto.ArtifactDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtifactExternalizerTest {

  @Mock
  private StorageService storageService;

  @InjectMocks
  private ArtifactExternalizer externalizer;

  @Test
  void shouldExternalizeImageWithPresignedUrl() {
    Artifact artifact = Artifact.builder()
        .name("hero")
        .kind(ArtifactKind.IMAGE)
        .storageKey("images/hero.png")
        .title("Hero")
        .description("Hero image")
        .status(ArtifactStatus.MANIFESTED)
        .build();

    when(storageService.generatePresignedUrl(eq("images/hero.png"), anyInt()))
        .thenReturn("https://cdn.example.com/hero.png?token=123");

    ArtifactDto dto = externalizer.externalize(artifact);

    assertThat(dto.name()).isEqualTo("hero");
    assertThat(dto.url()).isEqualTo("https://cdn.example.com/hero.png?token=123");
    verify(storageService).generatePresignedUrl(eq("images/hero.png"), eq(600));
  }

  @Test
  void shouldExternalizeTextWithoutUrl() {
    Artifact artifact = Artifact.builder()
        .name("story")
        .kind(ArtifactKind.TEXT)
        .title("Story")
        .description("Once upon a time")
        .status(ArtifactStatus.MANIFESTED)
        .build();

    ArtifactDto dto = externalizer.externalize(artifact);

    assertThat(dto.name()).isEqualTo("story");
    assertThat(dto.url()).isNull();
    assertThat(dto.description()).isEqualTo("Once upon a time");
  }

  @Test
  void shouldHandleNullStorageKey() {
    Artifact artifact = Artifact.builder()
        .name("gestating")
        .kind(ArtifactKind.IMAGE)
        .storageKey(null)
        .title("Title")
        .description("Desc")
        .status(ArtifactStatus.GESTATING)
        .build();

    ArtifactDto dto = externalizer.externalize(artifact);

    assertThat(dto.url()).isNull();
    assertThat(dto.status()).isEqualTo(ArtifactStatus.GESTATING);
  }
}
