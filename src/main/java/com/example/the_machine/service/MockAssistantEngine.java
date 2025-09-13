package com.example.the_machine.service;

import com.example.the_machine.domain.ArtifactEntity;
import com.example.the_machine.domain.RunEntity;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
      final var planArtifact = ctx.createArtifact(
          ArtifactEntity.Kind.STRUCTURED_JSON,
          ArtifactEntity.Stage.PLAN,
          "Marketing Pitch Planning",
          "application/json"
      );

      final var planContent = Map.of(
          "content",
          "I need to create a marketing pitch. Could you please specify which brand you'd like me to analyze?",
          "status", "awaiting_input"
      );

      ctx.setArtifactContent(planArtifact.getId(), ctx.objectMapper().valueToTree(planContent));

      // Simulate assistant asking for clarification
      Thread.sleep(500);

      final var questionMessageId = UUID.randomUUID();
      final var questionData = Map.of(
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
      final var analysisArtifact = ctx.createArtifact(
          ArtifactEntity.Kind.STRUCTURED_JSON,
          ArtifactEntity.Stage.ANALYSIS,
          "Apple Brand Analysis",
          "application/json"
      );

      final var analysisContent = Map.of(
          "brand", "Apple",
          "key_points",
          new String[]{"Innovation", "Premium quality", "User experience", "Brand loyalty"},
          "target_audience", "Tech-savvy consumers, premium market"
      );

      ctx.setArtifactContent(analysisArtifact.getId(),
          ctx.objectMapper().valueToTree(analysisContent));

      // Script generation
      Thread.sleep(2000);
      final var scriptArtifact = ctx.createArtifact(
          ArtifactEntity.Kind.STRUCTURED_JSON,
          ArtifactEntity.Stage.SCRIPT,
          "Apple Brand Marketing Pitch",
          "text/plain"
      );

      final var scriptContent = Map.of(
          "content",
          "Revolutionary. Intuitive. Premium. Apple doesn't just create products - we craft experiences that transform how people connect with technology...",
          "duration", "30 seconds"
      );

      ctx.setArtifactContent(scriptArtifact.getId(), ctx.objectMapper().valueToTree(scriptContent));

      // Image generation simulation
      Thread.sleep(3000);
      final var imageFiles = createPlaceholderFiles(ctx.threadId(), "png", 3);
      for (int i = 0; i < imageFiles.length; i++) {
        final var imageArtifact = ctx.createArtifact(
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
      final var revisedImageFiles = createPlaceholderFiles(ctx.threadId(), "png", 2);
      for (int i = 0; i < revisedImageFiles.length; i++) {
        final var revisedImageArtifact = ctx.createArtifact(
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
      final var deckFile = createPlaceholderFiles(ctx.threadId(), "pdf", 1)[0];
      final var deckArtifact = ctx.createArtifact(
          ArtifactEntity.Kind.PDF,
          ArtifactEntity.Stage.DECK,
          "Apple Marketing Pitch - Revised",
          "application/pdf"
      );

      ctx.setArtifactFile(deckArtifact.getId(), deckFile, "application/pdf");

      // Set metadata with page count
      final var deckMetadata = Map.of("pages", 12);
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
    final var threadDir = Paths.get(dataBasePath, threadId.toString());
    Files.createDirectories(threadDir);

    final var filePaths = new String[count];
    for (int i = 0; i < count; i++) {
      final var fileName = UUID.randomUUID() + "." + extension;
      final var filePath = threadDir.resolve(fileName);

      // Create small placeholder file
      final var content = extension.equals("pdf")
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
    final var pdfContent = """
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