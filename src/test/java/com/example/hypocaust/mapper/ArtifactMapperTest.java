package com.example.hypocaust.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArtifactMapperTest {

  private ArtifactMapper artifactMapper;

  @Mock
  private StorageService storageService;

  @BeforeEach
  void setUp() {
    // Use the generated implementation
    artifactMapper = new ArtifactMapperImpl();
    ReflectionTestUtils.setField(artifactMapper, "storageService", storageService);
  }

  @Test
  void toDomain_mapsStorageKeyToPresignedUrl() {
    ArtifactEntity entity = ArtifactEntity.builder()
        .name("test-artifact")
        .kind(ArtifactKind.IMAGE)
        .storageKey("some/key.png")
        .title("Test Title")
        .description("Test Description")
        .status(ArtifactStatus.MANIFESTED)
        .build();

    when(storageService.generatePresignedUrl(eq("some/key.png"), anyInt()))
        .thenReturn("https://presigned.url/key.png");

    Artifact domain = artifactMapper.toDomain(entity);

    assertThat(domain.name()).isEqualTo("test-artifact");
    assertThat(domain.url()).isEqualTo("https://presigned.url/key.png");
    assertThat(domain.storageKey()).isEqualTo("some/key.png");
    // Verify other fields are mapped correctly and NOT passed through toPresignedUrl
    assertThat(domain.title()).isEqualTo("Test Title");
    assertThat(domain.description()).isEqualTo("Test Description");
  }

  @Test
  void toDomain_handlesNullStorageKey() {
    ArtifactEntity entity = ArtifactEntity.builder()
        .name("test-artifact")
        .kind(ArtifactKind.TEXT)
        .storageKey(null)
        .title("Test Title")
        .description("Test Description")
        .status(ArtifactStatus.MANIFESTED)
        .build();

    Artifact domain = artifactMapper.toDomain(entity);

    assertThat(domain.url()).isNull();
  }
}
