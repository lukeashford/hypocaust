package com.example.the_machine.service;

import com.example.the_machine.domain.ArtifactEntity;
import com.example.the_machine.domain.RunEntity;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Mock implementation of AssistantEngine for MVP testing. Uses timers/sleeps to simulate streaming
 * and creates placeholder artifacts.
 */
@Service
@Slf4j
public class MockAssistantEngine implements AssistantEngine {

  @Value("${app.data.base-path:./data}")
  private String dataBasePath;

  @Override
  public void executePlanAskClarify(RunContext ctx) {
    log.info("Starting plan/clarify execution for run: {}", ctx.runId());

    try {
      // Simulate planning phase
      Thread.sleep(1000);

      // Create plan artifact directly
      val planArtifact = ctx.createArtifact(
          ArtifactEntity.Kind.STRUCTURED_JSON,
          ArtifactEntity.Stage.PLAN,
          "Marketing Pitch Planning",
          "application/json"
      );

      val planContent = Map.of(
          "content",
          "I need to create a marketing pitch. Could you please specify which brand you'd like me to analyze?",
          "status", "awaiting_input"
      );

      ctx.setArtifactContent(planArtifact.getId(), ctx.objectMapper().valueToTree(planContent));

      // Simulate assistant asking for clarification
      Thread.sleep(500);

      val questionMessageId = UUID.randomUUID();
      val questionData = Map.of(
          "id", questionMessageId,
          "author", "ASSISTANT",
          "content", Map.of(
              "type", "text",
              "text", "Which brand would you like me to analyze for the marketing pitch?"
          )
      );

      ctx.emitMessage(questionMessageId, questionData, true);
      ctx.updateRunStatus(RunEntity.Status.REQUIRES_ACTION);

      log.info("Plan/clarify execution completed for run: {}", ctx.runId());

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      ctx.updateRunStatus(RunEntity.Status.FAILED);
      log.error("Plan/clarify execution interrupted for run: {}", ctx.runId(), e);
    }
  }

  @Override
  public void executeFullPipeline(RunContext ctx) {
    log.info("Starting full pipeline execution for run: {}", ctx.runId());

    try {
      // Analysis phase
      Thread.sleep(1500);
      val analysisArtifact = ctx.createArtifact(
          ArtifactEntity.Kind.STRUCTURED_JSON,
          ArtifactEntity.Stage.ANALYSIS,
          "Apple Brand Analysis",
          "application/json"
      );

      val analysisContent = Map.of(
          "brand", "Apple",
          "key_points",
          new String[]{"Innovation", "Premium quality", "User experience", "Brand loyalty"},
          "target_audience", "Tech-savvy consumers, premium market"
      );

      ctx.setArtifactContent(analysisArtifact.getId(),
          ctx.objectMapper().valueToTree(analysisContent));

      // Script generation
      Thread.sleep(2000);
      val scriptArtifact = ctx.createArtifact(
          ArtifactEntity.Kind.STRUCTURED_JSON,
          ArtifactEntity.Stage.SCRIPT,
          "Apple Brand Marketing Pitch",
          "text/plain"
      );

      val scriptContent = Map.of(
          "content",
          "Revolutionary. Intuitive. Premium. Apple doesn't just create products - we craft experiences that transform how people connect with technology...",
          "duration", "30 seconds"
      );

      ctx.setArtifactContent(scriptArtifact.getId(), ctx.objectMapper().valueToTree(scriptContent));

      // Image generation simulation
      Thread.sleep(3000);
      val imageFiles = createPlaceholderFiles(ctx.threadId(), "png", 3);
      for (int i = 0; i < imageFiles.length; i++) {
        val imageArtifact = ctx.createArtifact(
            ArtifactEntity.Kind.IMAGE,
            ArtifactEntity.Stage.IMAGES,
            "Marketing Visual " + (i + 1),
            "image/png"
        );
        ctx.setArtifactFile(imageArtifact.getId(), imageFiles[i], "image/png");
        Thread.sleep(500); // Simulate streaming
      }

      ctx.updateRunStatus(RunEntity.Status.COMPLETED);
      log.info("Full pipeline execution completed for run: {}", ctx.runId());

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      ctx.updateRunStatus(RunEntity.Status.FAILED);
      log.error("Full pipeline execution interrupted for run: {}", ctx.runId(), e);
    }
  }

  @Override
  public void executePartialRevision(RunContext ctx, String reason) {
    log.info("Starting partial revision execution for run: {} with reason: {}", ctx.runId(),
        reason);

    try {
      // Simulate revision analysis
      Thread.sleep(1000);

      // Generate updated images with female character
      Thread.sleep(2000);
      val revisedImageFiles = createPlaceholderFiles(ctx.threadId(), "png", 2);
      for (int i = 0; i < revisedImageFiles.length; i++) {
        val revisedImageArtifact = ctx.createArtifact(
            ArtifactEntity.Kind.IMAGE,
            ArtifactEntity.Stage.IMAGES,
            "Updated Marketing Visual " + (i + 1),
            "image/png"
        );
        ctx.setArtifactFile(revisedImageArtifact.getId(), revisedImageFiles[i], "image/png");
        Thread.sleep(500);
      }

      // Generate presentation deck
      Thread.sleep(1500);
      val deckFile = createPlaceholderFiles(ctx.threadId(), "pdf", 1)[0];
      val deckArtifact = ctx.createArtifact(
          ArtifactEntity.Kind.PDF,
          ArtifactEntity.Stage.DECK,
          "Apple Marketing Pitch - Revised",
          "application/pdf"
      );

      ctx.setArtifactFile(deckArtifact.getId(), deckFile, "application/pdf");

      // Set metadata with page count
      val deckMetadata = Map.of("pages", 12);
      ctx.setArtifactMetadata(deckArtifact.getId(), ctx.objectMapper().valueToTree(deckMetadata));

      ctx.updateRunStatus(RunEntity.Status.COMPLETED);
      log.info("Partial revision execution completed for run: {}", ctx.runId());

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      ctx.updateRunStatus(RunEntity.Status.FAILED);
      log.error("Partial revision execution interrupted for run: {}", ctx.runId(), e);
    }
  }

  @SneakyThrows
  private String[] createPlaceholderFiles(UUID threadId, String extension, int count) {
    val threadDir = Paths.get(dataBasePath, threadId.toString());
    Files.createDirectories(threadDir);

    val filePaths = new String[count];
    for (int i = 0; i < count; i++) {
      val fileName = UUID.randomUUID() + "." + extension;
      val filePath = threadDir.resolve(fileName);

      // Create small placeholder file
      val content = extension.equals("pdf")
          ? createPdfPlaceholder()
          : createImagePlaceholder();
      Files.write(filePath, content);

      filePaths[i] = filePath.toString();
      log.debug("Created placeholder file: {}", filePaths[i]);
    }

    return filePaths;
  }

  private byte[] createImagePlaceholder() {
    // Minimal PNG header for a 1x1 pixel transparent image
    return new byte[]{
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
        0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89, 0x00, 0x00, 0x00, 0x0D,
        0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0xDA, 0x63, (byte) 0xF8, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, 0x7F,
        0x00, 0x05, (byte) 0xFE, 0x02, (byte) 0xFE, (byte) 0xDC, (byte) 0xCC, 0x59, (byte) 0xE7,
        0x00, 0x00, 0x00, 0x00,
        0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };
  }

  private byte[] createPdfPlaceholder() {
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
         0000000000 65535 f\s
         0000000009 00000 n\s
         0000000058 00000 n\s
         0000000115 00000 n\s
         trailer
         << /Size 4 /Root 1 0 R >>
         startxref
         185
         %%EOF
        \s""";
    return pdfContent.getBytes();
  }
}