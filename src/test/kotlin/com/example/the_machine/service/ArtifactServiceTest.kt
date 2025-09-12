package com.example.the_machine.service

import com.example.the_machine.common.UuidV7
import com.example.the_machine.config.TestDataConfiguration.TEST_ARTIFACT_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_RUN_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID_2
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID_3
import com.example.the_machine.domain.ArtifactEntity
import com.example.the_machine.repo.ArtifactRepository
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ArtifactServiceTest {

  private val artifactRepository: ArtifactRepository = mockk()
  private val artifactService = ArtifactService(artifactRepository)
  private val artifactSlot = slot<ArtifactEntity>()

  @Test
  fun shouldCreateStructuredJsonArtifact() {
    mockkObject(UuidV7)
    try {
      // Given
      val threadId = TEST_THREAD_ID
      val runId = TEST_RUN_ID

      every { UuidV7.newId() } returns TEST_ARTIFACT_ID
      every { artifactRepository.save(capture(artifactSlot)) } answers { artifactSlot.captured }

      // When
      val result = artifactService.createArtifact(
        threadId, runId,
        ArtifactEntity.Kind.STRUCTURED_JSON,
        ArtifactEntity.Stage.SCRIPT,
        "Apple Marketing Pitch",
        null
      )

      // Then
      verify { artifactRepository.save(any()) }
      val savedArtifact = artifactSlot.captured

      assertThat(savedArtifact.id).isEqualTo(TEST_ARTIFACT_ID)
      assertThat(savedArtifact.threadId).isEqualTo(threadId)
      assertThat(savedArtifact.runId).isEqualTo(runId)
      assertThat(savedArtifact.kind).isEqualTo(ArtifactEntity.Kind.STRUCTURED_JSON)
      assertThat(savedArtifact.stage).isEqualTo(ArtifactEntity.Stage.SCRIPT)
      assertThat(savedArtifact.status).isEqualTo(ArtifactEntity.Status.PENDING)
      assertThat(savedArtifact.title).isEqualTo("Apple Marketing Pitch")
      assertThat(savedArtifact.content).isNull() // No content initially
      assertThat(result).isEqualTo(savedArtifact)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  @Test
  fun shouldCreateImageArtifact() {
    mockkObject(UuidV7)
    try {
      // Given
      val threadId = TEST_THREAD_ID_2
      val runId = TEST_RUN_ID

      every { UuidV7.newId() } returns TEST_ARTIFACT_ID
      every { artifactRepository.save(capture(artifactSlot)) } answers { artifactSlot.captured }

      // When
      val result = artifactService.createArtifact(
        threadId, runId,
        ArtifactEntity.Kind.IMAGE,
        ArtifactEntity.Stage.IMAGES,
        "Marketing Visual 1",
        "image/png"
      )

      // Then
      verify { artifactRepository.save(any()) }
      val savedArtifact = artifactSlot.captured

      assertThat(savedArtifact.id).isEqualTo(TEST_ARTIFACT_ID)
      assertThat(savedArtifact.kind).isEqualTo(ArtifactEntity.Kind.IMAGE)
      assertThat(savedArtifact.stage).isEqualTo(ArtifactEntity.Stage.IMAGES)
      assertThat(savedArtifact.title).isEqualTo("Marketing Visual 1")
      assertThat(savedArtifact.mime).isEqualTo("image/png")
      assertThat(savedArtifact.storageKey).isNull() // No storage key initially
      assertThat(savedArtifact.content).isNull() // Images don't have inline content
      assertThat(result).isEqualTo(savedArtifact)
    } finally {
      unmockkObject(UuidV7)
    }
  }

  @Test
  fun shouldCreatePdfArtifact() {
    mockkObject(UuidV7)
    try {
      // Given
      val threadId = TEST_THREAD_ID_3
      val runId = TEST_RUN_ID
      val artifactId = TEST_ARTIFACT_ID

      every { UuidV7.newId() } returns artifactId
      every { artifactRepository.save(capture(artifactSlot)) } answers { artifactSlot.captured }

      // When
      val result = artifactService.createArtifact(
        threadId, runId,
        ArtifactEntity.Kind.PDF,
        ArtifactEntity.Stage.DECK,
        "Apple Marketing Pitch - Revised",
        "application/pdf"
      )

      // Then
      verify { artifactRepository.save(any()) }
      val savedArtifact = artifactSlot.captured

      assertThat(savedArtifact.id).isEqualTo(artifactId)
      assertThat(savedArtifact.kind).isEqualTo(ArtifactEntity.Kind.PDF)
      assertThat(savedArtifact.stage).isEqualTo(ArtifactEntity.Stage.DECK)
      assertThat(savedArtifact.title).isEqualTo("Apple Marketing Pitch - Revised")
      assertThat(savedArtifact.mime).isEqualTo("application/pdf")
      assertThat(savedArtifact.storageKey).isNull() // No storage key initially
      assertThat(result).isEqualTo(savedArtifact)
    } finally {
      unmockkObject(UuidV7)
    }
  }
}