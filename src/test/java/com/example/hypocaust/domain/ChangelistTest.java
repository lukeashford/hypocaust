package com.example.hypocaust.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ChangelistTest {

  private Changelist changelist;

  @BeforeEach
  void setUp() {
    changelist = new Changelist();
  }

  private static Artifact artifact(String name) {
    return Artifact.builder()
        .name(name)
        .kind(ArtifactKind.IMAGE)
        .title("Title")
        .description("Desc")
        .status(ArtifactStatus.CREATED)
        .build();
  }

  private static Artifact artifact(String name, String title) {
    return Artifact.builder()
        .name(name)
        .kind(ArtifactKind.IMAGE)
        .title(title)
        .description("Desc")
        .status(ArtifactStatus.CREATED)
        .build();
  }

  @Nested
  class Add {

    @Test
    void happyPath() {
      changelist.addArtifact(artifact("img-001"));

      assertThat(changelist.getAddedNames()).containsExactly("img-001");
      assertThat(changelist.contains("img-001")).isTrue();
      assertThat(changelist.hasChanges()).isTrue();
    }

    @Test
    void duplicateName_throws() {
      changelist.addArtifact(artifact("img-001"));

      assertThatThrownBy(() -> changelist.addArtifact(artifact("img-001")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("img-001");
    }

    @Test
    void afterDelete_movesToEdited() {
      // Simulate: artifact exists in base, was deleted, then re-added
      changelist.deleteArtifact("img-001");
      changelist.addArtifact(artifact("img-001"));

      assertThat(changelist.getAddedNames()).isEmpty();
      assertThat(changelist.getEditedNames()).containsExactly("img-001");
      assertThat(changelist.getDeletedNames()).isEmpty();
    }
  }

  @Nested
  class EditTest {

    @Test
    void happyPath() {
      changelist.editArtifact(artifact("img-001"));

      assertThat(changelist.getEditedNames()).containsExactly("img-001");
      assertThat(changelist.contains("img-001")).isTrue();
    }

    @Test
    void onDeletedArtifact_removesFromDeletedAddsToEdited() {
      changelist.deleteArtifact("img-001");
      changelist.editArtifact(artifact("img-001"));

      assertThat(changelist.getDeletedNames()).isEmpty();
      assertThat(changelist.getEditedNames()).containsExactly("img-001");
    }
  }

  @Nested
  class Delete {

    @Test
    void onAddedArtifact_removesCompletely() {
      changelist.addArtifact(artifact("img-001"));
      changelist.deleteArtifact("img-001");

      assertThat(changelist.getAddedNames()).isEmpty();
      assertThat(changelist.getDeletedNames()).isEmpty();
      assertThat(changelist.contains("img-001")).isFalse();
      assertThat(changelist.hasChanges()).isFalse();
    }

    @Test
    void onNonAddedArtifact_goesToDeletedSet() {
      changelist.deleteArtifact("img-001");

      assertThat(changelist.getDeletedNames()).containsExactly("img-001");
      assertThat(changelist.contains("img-001")).isTrue();
    }
  }

  @Nested
  class Update {

    @Test
    void onAddedArtifact_updatesInPlace() {
      changelist.addArtifact(artifact("img-001", "Original"));
      changelist.updateArtifact(artifact("img-001", "Updated"));

      assertThat(changelist.getAdded()).hasSize(1);
      assertThat(changelist.getAdded().getFirst().title()).isEqualTo("Updated");
    }

    @Test
    void onEditedArtifact_updatesInPlace() {
      changelist.editArtifact(artifact("img-001", "V1"));
      changelist.updateArtifact(artifact("img-001", "V2"));

      assertThat(changelist.getEdited()).hasSize(1);
      assertThat(changelist.getEdited().getFirst().title()).isEqualTo("V2");
    }

    @Test
    void onNonExistent_throws() {
      assertThatThrownBy(() -> changelist.updateArtifact(artifact("img-001")))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("img-001");
    }
  }

  @Nested
  class Rollback {

    @Test
    void removesFromAllMaps() {
      changelist.addArtifact(artifact("a"));
      changelist.editArtifact(artifact("b"));
      changelist.deleteArtifact("c");

      changelist.rollbackArtifact("a");
      changelist.rollbackArtifact("b");
      changelist.rollbackArtifact("c");

      assertThat(changelist.hasChanges()).isFalse();
      assertThat(changelist.contains("a")).isFalse();
      assertThat(changelist.contains("b")).isFalse();
      assertThat(changelist.contains("c")).isFalse();
    }
  }

  @Nested
  class ApplyTo {

    @Test
    void addsNewReplacesEditedRemovesDeleted() {
      var base = new ArrayList<>(List.of(
          artifact("existing-1"),
          artifact("existing-2")
      ));

      changelist.addArtifact(artifact("new-1"));
      changelist.editArtifact(artifact("existing-1", "Edited"));
      changelist.deleteArtifact("existing-2");

      var result = changelist.applyTo(base);

      assertThat(result).hasSize(2);
      assertThat(result.stream().map(Artifact::name).toList())
          .containsExactlyInAnyOrder("existing-1", "new-1");
      assertThat(result.stream().filter(a -> a.name().equals("existing-1")).findFirst()
          .orElseThrow().title()).isEqualTo("Edited");
    }

    @Test
    void duplicateNameInAddAndBase_throws() {
      var base = List.of(artifact("img-001"));
      changelist.addArtifact(artifact("img-001"));

      assertThatThrownBy(() -> changelist.applyTo(base))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Duplicate");
    }

    @Test
    void editNonExistentInBase_throws() {
      var base = List.<Artifact>of();
      changelist.editArtifact(artifact("ghost"));

      assertThatThrownBy(() -> changelist.applyTo(base))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  void accessors_afterMixedOperations() {
    changelist.addArtifact(artifact("a"));
    changelist.addArtifact(artifact("b"));
    changelist.editArtifact(artifact("c"));
    changelist.deleteArtifact("d");

    assertThat(changelist.getAddedNames()).containsExactly("a", "b");
    assertThat(changelist.getEditedNames()).containsExactly("c");
    assertThat(changelist.getDeletedNames()).containsExactly("d");
    assertThat(changelist.hasChanges()).isTrue();
    assertThat(changelist.contains("a")).isTrue();
    assertThat(changelist.contains("d")).isTrue();
    assertThat(changelist.contains("z")).isFalse();
  }
}
