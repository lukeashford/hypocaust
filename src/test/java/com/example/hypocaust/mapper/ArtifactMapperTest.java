package com.example.hypocaust.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtifactMapperTest {

  private ArtifactMapper artifactMapper;

  @BeforeEach
  void setUp() {
    // Use the generated implementation
    artifactMapper = new ArtifactMapperImpl();
  }

  @Test
  void toDomain_mapsStorageKey() {
    ArtifactEntity entity = ArtifactEntity.builder()
        .name("test-artifact")
        .kind(ArtifactKind.IMAGE)
        .storageKey("some/key.png")
        .title("Test Title")
        .description("Test Description")
        .status(ArtifactStatus.MANIFESTED)
        .build();

    Artifact domain = artifactMapper.toDomain(entity);

    assertThat(domain.name()).isEqualTo("test-artifact");
    assertThat(domain.storageKey()).isEqualTo("some/key.png");
    // Verify other fields are mapped correctly
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

    assertThat(domain.storageKey()).isNull();
  }
}
