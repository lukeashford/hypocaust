package com.example.hypocaust.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.event.ArtifactAddedEvent;
import com.example.hypocaust.exception.ArtifactNotFoundException;
import com.example.hypocaust.service.NamingService;
import com.example.hypocaust.service.VersionManagementService;
import com.example.hypocaust.service.events.EventService;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArtifactsContextRestoreTest {

  private static final UUID PROJECT_ID = UUID.randomUUID();
  private static final UUID TASK_EXECUTION_ID = UUID.randomUUID();
  private static final UUID PREDECESSOR_ID = UUID.randomUUID();

  private EventService eventService;
  private VersionManagementService versionService;
  private NamingService namingService;
  private ArtifactsContext context;

  @BeforeEach
  void setUp() {
    eventService = mock(EventService.class);
    versionService = mock(VersionManagementService.class);
    namingService = mock(NamingService.class);
    when(namingService.generateArtifactName(anyString(), anyCollection(), anyString()))
        .thenAnswer(invocation -> {
          Collection<String> existing = invocation.getArgument(1);
          String preferred = invocation.getArgument(2);
          if (preferred != null && !existing.contains(preferred)) {
            return preferred;
          }
          return preferred != null ? preferred + "_new" : "generated_name";
        });
    when(namingService.generateArtifactName(anyString(), anyCollection()))
        .thenAnswer(invocation -> "generated_name");

    when(versionService.computeArtifactSnapshotAt(PREDECESSOR_ID)).thenReturn(Map.of());

    context = new ArtifactsContext(
        PROJECT_ID, TASK_EXECUTION_ID, PREDECESSOR_ID,
        eventService, versionService, namingService
    );
  }

  @Test
  void restore_artifactNotFound_throwsArtifactNotFoundException() {
    when(versionService.getMaterializedArtifactAtExecution(
        "protagonist", "initial_character_designs", PROJECT_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> context.restore("protagonist", "initial_character_designs"))
        .isInstanceOf(ArtifactNotFoundException.class)
        .hasMessageContaining("protagonist")
        .hasMessageContaining("initial_character_designs");
  }

  @Test
  void restore_nameAvailable_reusesOriginalName() {
    Artifact source = Artifact.builder()
        .name("protagonist")
        .kind(ArtifactKind.IMAGE)
        .title("Protagonist Portrait")
        .description("A portrait of the protagonist")
        .status(ArtifactStatus.MANIFESTED)
        .url("https://storage.example.com/protagonist.webp")
        .build();

    when(versionService.getMaterializedArtifactAtExecution(
        "protagonist", "initial_character_designs", PROJECT_ID))
        .thenReturn(Optional.of(source));
    // No existing artifacts in snapshot

    String finalName = context.restore("protagonist", "initial_character_designs");

    assertThat(finalName).isEqualTo("protagonist");
    verify(eventService).publish(any(ArtifactAddedEvent.class));
  }

  @Test
  void restore_nameTaken_delegatesToNameGenerator() {
    Artifact source = Artifact.builder()
        .name("protagonist")
        .kind(ArtifactKind.IMAGE)
        .title("Protagonist Portrait")
        .description("A portrait of the protagonist")
        .status(ArtifactStatus.MANIFESTED)
        .url("https://storage.example.com/protagonist.webp")
        .build();

    when(versionService.getMaterializedArtifactAtExecution(
        "protagonist", "initial_character_designs", PROJECT_ID))
        .thenReturn(Optional.of(source));

    // Existing snapshot already contains "protagonist"
    when(versionService.computeArtifactSnapshotAt(PREDECESSOR_ID))
        .thenReturn(Map.of("protagonist", UUID.randomUUID()));
    when(namingService.generateArtifactName(
        eq("A portrait of the protagonist"), anyCollection(), eq("protagonist")))
        .thenReturn("protagonist_2");

    String finalName = context.restore("protagonist", "initial_character_designs");

    assertThat(finalName).isEqualTo("protagonist_2");
    verify(namingService).generateArtifactName(
        eq("A portrait of the protagonist"), anyCollection(), eq("protagonist"));
  }

  @Test
  void restore_imageArtifact_setsStatusToCreated() {
    Artifact source = Artifact.builder()
        .name("protagonist")
        .kind(ArtifactKind.IMAGE)
        .title("Protagonist Portrait")
        .description("Portrait")
        .status(ArtifactStatus.MANIFESTED)
        .url("https://storage.example.com/protagonist.webp")
        .build();

    when(versionService.getMaterializedArtifactAtExecution(any(), any(), any()))
        .thenReturn(Optional.of(source));

    context.restore("protagonist", "initial_character_designs");

    // Verify the artifact in the changelist has CREATED status (needed for materialization)
    Optional<Artifact> restored = context.getChangelist().getAdded().stream()
        .filter(a -> a.name().equals("protagonist"))
        .findFirst();

    assertThat(restored).isPresent();
    assertThat(restored.get().status()).isEqualTo(ArtifactStatus.CREATED);
    assertThat(restored.get().url()).isEqualTo("https://storage.example.com/protagonist.webp");
  }

  @Test
  void restore_textArtifactWithInlineContent_keepsOriginalStatus() {
    Artifact source = Artifact.builder()
        .name("story_outline")
        .kind(ArtifactKind.TEXT)
        .title("Story Outline")
        .description("Initial story outline")
        .status(ArtifactStatus.MANIFESTED)
        .url(null)
        .inlineContent(com.fasterxml.jackson.databind.node.TextNode.valueOf("Once upon a time..."))
        .build();

    when(versionService.getMaterializedArtifactAtExecution(any(), any(), any()))
        .thenReturn(Optional.of(source));

    String finalName = context.restore("story_outline", "initial_draft");

    assertThat(finalName).isEqualTo("story_outline");

    Optional<Artifact> restored = context.getChangelist().getAdded().stream()
        .filter(a -> a.name().equals("story_outline"))
        .findFirst();

    assertThat(restored).isPresent();
    assertThat(restored.get().status()).isEqualTo(ArtifactStatus.MANIFESTED);
    assertThat(restored.get().url()).isNull();
    assertThat(restored.get().inlineContent()).isNotNull();
  }

  @Test
  void restore_copiesAllArtifactFields() {
    Artifact source = Artifact.builder()
        .name("protagonist")
        .kind(ArtifactKind.IMAGE)
        .title("Protagonist Portrait")
        .description("A detailed portrait")
        .status(ArtifactStatus.MANIFESTED)
        .url("https://storage.example.com/img.webp")
        .mimeType("image/webp")
        .build();

    when(versionService.getMaterializedArtifactAtExecution(any(), any(), any()))
        .thenReturn(Optional.of(source));

    context.restore("protagonist", "initial_character_designs");

    Artifact restored = context.getChangelist().getAdded().getFirst();
    assertThat(restored.kind()).isEqualTo(ArtifactKind.IMAGE);
    assertThat(restored.title()).isEqualTo("Protagonist Portrait");
    assertThat(restored.description()).isEqualTo("A detailed portrait");
    assertThat(restored.mimeType()).isEqualTo("image/webp");
    assertThat(restored.url()).isEqualTo("https://storage.example.com/img.webp");
  }
}
