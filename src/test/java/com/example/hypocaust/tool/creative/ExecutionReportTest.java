package com.example.hypocaust.tool.creative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ExecutionReportTest {

  @Test
  void builder_createsReportWithAttempts() {
    var report = ExecutionReport.builder()
        .success(true)
        .summary("Generated image")
        .addAttempt(Map.of("model", "SDXL", "status", "success"))
        .addArtifactName("sunset-1")
        .build();

    assertThat(report.success()).isTrue();
    assertThat(report.summary()).isEqualTo("Generated image");
    assertThat(report.attempts()).hasSize(1);
    assertThat(report.attempts().getFirst()).containsEntry("model", "SDXL");
    assertThat(report.artifactNames()).containsExactly("sunset-1");
  }

  @Test
  void builder_truncatesLongValues() {
    String longValue = "A".repeat(300);
    var report = ExecutionReport.builder()
        .success(false)
        .addAttempt(Map.of("error", longValue))
        .build();

    var errorValue = report.attempts().getFirst().get("error");
    assertThat(errorValue.length()).isLessThanOrEqualTo(201); // 200 + ellipsis
  }

  @Test
  void builder_truncatesToTwoLines() {
    String multiline = "Line 1\nLine 2\nLine 3\nLine 4";
    var report = ExecutionReport.builder()
        .addAttempt(Map.of("error", multiline))
        .build();

    var errorValue = report.attempts().getFirst().get("error");
    assertThat(errorValue).startsWith("Line 1\nLine 2");
    assertThat(errorValue).doesNotContain("Line 3");
  }

  @Test
  void builder_nullValuePreserved() {
    var report = ExecutionReport.builder()
        .addAttempt(Map.of("model", "SDXL"))
        .build();

    assertThat(report.attempts().getFirst()).doesNotContainKey("error");
  }

  @Test
  void builder_multipleAttempts_preserveOrder() {
    var report = ExecutionReport.builder()
        .addAttempt(Map.of("attempt", "1", "status", "failed"))
        .addAttempt(Map.of("attempt", "2", "status", "success"))
        .build();

    assertThat(report.attempts()).hasSize(2);
    assertThat(report.attempts().get(0)).containsEntry("attempt", "1");
    assertThat(report.attempts().get(1)).containsEntry("attempt", "2");
  }

  @Test
  void builder_nullArtifactName_ignored() {
    var report = ExecutionReport.builder()
        .addArtifactName(null)
        .addArtifactName("real-artifact")
        .build();

    assertThat(report.artifactNames()).containsExactly("real-artifact");
  }

  @Test
  void builder_emptyReport() {
    var report = ExecutionReport.builder().build();

    assertThat(report.success()).isFalse();
    assertThat(report.summary()).isNull();
    assertThat(report.attempts()).isEmpty();
    assertThat(report.artifactNames()).isEmpty();
  }
}
