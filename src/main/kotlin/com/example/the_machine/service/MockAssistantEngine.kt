package com.example.the_machine.service

import com.example.the_machine.domain.ArtifactEntity
import com.example.the_machine.domain.RunEntity
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Mock implementation of AssistantEngine for MVP testing. Uses timers/sleeps to simulate streaming
 * and creates placeholder artifacts.
 */
@Service
class MockAssistantEngine(
  @Value("\${app.data.base-path:./data}")
  private val dataBasePath: String
) : AssistantEngine {

  override fun executePlanAskClarify(ctx: RunContext) {
    log.info { "Starting plan/clarify execution for run: ${ctx.runId}" }

    try {
      // Simulate planning phase
      Thread.sleep(1000)

      // Create plan artifact directly
      val planArtifact = ctx.createArtifact(
        ArtifactEntity.Kind.STRUCTURED_JSON,
        ArtifactEntity.Stage.PLAN,
        "Marketing Pitch Planning",
        "application/json"
      )

      val planContent = mapOf(
        "content" to "I need to create a marketing pitch. Could you please specify which brand you'd like me to analyze?",
        "status" to "awaiting_input"
      )

      ctx.setArtifactContent(planArtifact.id!!, ctx.objectMapper.valueToTree(planContent))

      // Simulate assistant asking for clarification
      Thread.sleep(500)

      val questionMessageId = UUID.randomUUID()
      val questionData = mapOf(
        "id" to questionMessageId,
        "author" to "ASSISTANT",
        "content" to mapOf(
          "type" to "text",
          "text" to "Which brand would you like me to analyze for the marketing pitch?"
        )
      )

      ctx.emitMessage(questionMessageId, questionData, true)
      ctx.updateRunStatus(RunEntity.Status.REQUIRES_ACTION)

      log.info { "Plan/clarify execution completed for run: ${ctx.runId}" }

    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      ctx.updateRunStatus(RunEntity.Status.FAILED)
      log.error(e) { "Plan/clarify execution interrupted for run: ${ctx.runId}" }
    }
  }

  override fun executeFullPipeline(ctx: RunContext) {
    log.info { "Starting full pipeline execution for run: ${ctx.runId}" }

    try {
      // Analysis phase
      Thread.sleep(1500)
      val analysisArtifact = ctx.createArtifact(
        ArtifactEntity.Kind.STRUCTURED_JSON,
        ArtifactEntity.Stage.ANALYSIS,
        "Apple Brand Analysis",
        "application/json"
      )

      val analysisContent = mapOf(
        "brand" to "Apple",
        "key_points" to arrayOf(
          "Innovation",
          "Premium quality",
          "User experience",
          "Brand loyalty"
        ),
        "target_audience" to "Tech-savvy consumers, premium market"
      )

      ctx.setArtifactContent(
        analysisArtifact.id!!,
        ctx.objectMapper.valueToTree(analysisContent)
      )

      // Script generation
      Thread.sleep(2000)
      val scriptArtifact = ctx.createArtifact(
        ArtifactEntity.Kind.STRUCTURED_JSON,
        ArtifactEntity.Stage.SCRIPT,
        "Apple Brand Marketing Pitch",
        "text/plain"
      )

      val scriptContent = mapOf(
        "content" to "Revolutionary. Intuitive. Premium. Apple doesn't just create products - we craft experiences that transform how people connect with technology...",
        "duration" to "30 seconds"
      )

      ctx.setArtifactContent(scriptArtifact.id!!, ctx.objectMapper.valueToTree(scriptContent))

      // Image generation simulation
      Thread.sleep(3000)
      val imageFiles = createPlaceholderFiles(ctx.threadId, "png", 3)
      for (i in imageFiles.indices) {
        val imageArtifact = ctx.createArtifact(
          ArtifactEntity.Kind.IMAGE,
          ArtifactEntity.Stage.IMAGES,
          "Marketing Visual ${i + 1}",
          "image/png"
        )
        ctx.setArtifactFile(imageArtifact.id!!, imageFiles[i], "image/png")
        Thread.sleep(500) // Simulate streaming
      }

      ctx.updateRunStatus(RunEntity.Status.COMPLETED)
      log.info { "Full pipeline execution completed for run: ${ctx.runId}" }

    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      ctx.updateRunStatus(RunEntity.Status.FAILED)
      log.error(e) { "Full pipeline execution interrupted for run: ${ctx.runId}" }
    }
  }

  override fun executePartialRevision(ctx: RunContext, reason: String) {
    log.info { "Starting partial revision execution for run: ${ctx.runId} with reason: $reason" }

    try {
      // Simulate revision analysis
      Thread.sleep(1000)

      // Generate updated images with female character
      Thread.sleep(2000)
      val revisedImageFiles = createPlaceholderFiles(ctx.threadId, "png", 2)
      for (i in revisedImageFiles.indices) {
        val revisedImageArtifact = ctx.createArtifact(
          ArtifactEntity.Kind.IMAGE,
          ArtifactEntity.Stage.IMAGES,
          "Updated Marketing Visual ${i + 1}",
          "image/png"
        )
        ctx.setArtifactFile(revisedImageArtifact.id!!, revisedImageFiles[i], "image/png")
        Thread.sleep(500)
      }

      // Generate presentation deck
      Thread.sleep(1500)
      val deckFile = createPlaceholderFiles(ctx.threadId, "pdf", 1)[0]
      val deckArtifact = ctx.createArtifact(
        ArtifactEntity.Kind.PDF,
        ArtifactEntity.Stage.DECK,
        "Apple Marketing Pitch - Revised",
        "application/pdf"
      )

      ctx.setArtifactFile(deckArtifact.id!!, deckFile, "application/pdf")

      // Set metadata with page count
      val deckMetadata = mapOf("pages" to 12)
      ctx.setArtifactMetadata(deckArtifact.id, ctx.objectMapper.valueToTree(deckMetadata))

      ctx.updateRunStatus(RunEntity.Status.COMPLETED)
      log.info { "Partial revision execution completed for run: ${ctx.runId}" }

    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      ctx.updateRunStatus(RunEntity.Status.FAILED)
      log.error(e) { "Partial revision execution interrupted for run: ${ctx.runId}" }
    }
  }

  private fun createPlaceholderFiles(threadId: UUID, extension: String, count: Int): Array<String> {
    return try {
      val threadDir = Paths.get(dataBasePath, threadId.toString())
      Files.createDirectories(threadDir)

      val filePaths = Array(count) { "" }
      for (i in 0 until count) {
        val fileName = "${UUID.randomUUID()}.$extension"
        val filePath = threadDir.resolve(fileName)

        // Create small placeholder file
        val content = if (extension == "pdf") {
          createPdfPlaceholder()
        } else {
          createImagePlaceholder()
        }
        Files.write(filePath, content)

        filePaths[i] = filePath.toString()
        log.debug { "Created placeholder file: ${filePaths[i]}" }
      }

      filePaths
    } catch (e: Exception) {
      log.error(e) { "Failed to create placeholder files for thread: $threadId" }
      throw e
    }
  }

  private fun createImagePlaceholder(): ByteArray {
    // Minimal PNG header for a 1x1 pixel transparent image
    return byteArrayOf(
      0x89.toByte(),
      0x50,
      0x4E,
      0x47,
      0x0D,
      0x0A,
      0x1A,
      0x0A,
      0x00,
      0x00,
      0x00,
      0x0D,
      0x49,
      0x48,
      0x44,
      0x52,
      0x00,
      0x00,
      0x00,
      0x01,
      0x00,
      0x00,
      0x00,
      0x01,
      0x08,
      0x06,
      0x00,
      0x00,
      0x00,
      0x1F,
      0x15,
      0xC4.toByte(),
      0x89.toByte(),
      0x00,
      0x00,
      0x00,
      0x0D,
      0x49,
      0x44,
      0x41,
      0x54,
      0x78,
      0xDA.toByte(),
      0x63,
      0xF8.toByte(),
      0xFF.toByte(),
      0xFF.toByte(),
      0xFF.toByte(),
      0x7F,
      0x00,
      0x05,
      0xFE.toByte(),
      0x02,
      0xFE.toByte(),
      0xDC.toByte(),
      0xCC.toByte(),
      0x59,
      0xE7.toByte(),
      0x00,
      0x00,
      0x00,
      0x00,
      0x49,
      0x45,
      0x4E,
      0x44,
      0xAE.toByte(),
      0x42,
      0x60,
      0x82.toByte()
    )
  }

  private fun createPdfPlaceholder(): ByteArray {
    // Minimal PDF file content
    val pdfContent = """
            %PDF-1.3
            1 0 obj
            << /Type /Catalog /Pages 2 0 R >>
            endobj
            2 0 obj
            << /Type /Pages /Kids [3 0 R] /Count 1 >>
            endobj
            3 0 obj
            << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>
            endobj
            xref
            0 4
            0000000000 65535 f 
            0000000009 00000 n 
            0000000058 00000 n 
            0000000115 00000 n 
            trailer
            << /Size 4 /Root 1 0 R >>
            startxref
            185
            %%EOF
             """.trimIndent()
    return pdfContent.toByteArray()
  }
}