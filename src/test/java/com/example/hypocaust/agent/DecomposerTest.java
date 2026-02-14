package com.example.hypocaust.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.service.events.EventService;
import com.example.hypocaust.tool.ProjectContextTool;
import com.example.hypocaust.tool.WorkflowSearchTool;
import com.example.hypocaust.tool.decomposition.InvokeDecomposerTool;
import com.example.hypocaust.tool.discovery.ExecuteToolTool;
import com.example.hypocaust.tool.discovery.SearchToolsTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DecomposerTest {

  private Decomposer decomposer;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    decomposer = new Decomposer(
        mock(ModelRegistry.class),
        mock(InvokeDecomposerTool.class),
        mock(SearchToolsTool.class),
        mock(ExecuteToolTool.class),
        mock(ProjectContextTool.class),
        mock(WorkflowSearchTool.class),
        mock(EventService.class),
        objectMapper
    );
  }

  @Test
  void parseResult_validJson_returnsSuccess() {
    var result = decomposer.parseResult(
        "{\"success\": true, \"summary\": \"All done\", \"artifactNames\": [\"img-001\"]}");

    assertThat(result.success()).isTrue();
    assertThat(result.summary()).isEqualTo("All done");
    assertThat(result.artifactNames()).containsExactly("img-001");
    assertThat(result.errorMessage()).isNull();
  }

  @Test
  void parseResult_validJsonFailure_returnsFailure() {
    var result = decomposer.parseResult(
        "{\"success\": false, \"errorMessage\": \"No model found\"}");

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isEqualTo("No model found");
  }

  @Test
  void parseResult_markdownCodeBlock_extractsJson() {
    var response = """
        Here is the result:
        ```json
        {"success": true, "summary": "Generated image", "artifactNames": ["landscape-001"]}
        ```
        """;

    var result = decomposer.parseResult(response);

    assertThat(result.success()).isTrue();
    assertThat(result.summary()).isEqualTo("Generated image");
    assertThat(result.artifactNames()).containsExactly("landscape-001");
  }

  @Test
  void parseResult_genericCodeBlock_extractsJson() {
    var response = """
        Result:
        ```
        {"success": true, "summary": "Done", "artifactNames": []}
        ```
        """;

    var result = decomposer.parseResult(response);

    assertThat(result.success()).isTrue();
    assertThat(result.summary()).isEqualTo("Done");
  }

  @Test
  void parseResult_bareJsonInText_extractsJson() {
    var response = "I completed the task. {\"success\": true, \"summary\": \"Task complete\", "
        + "\"artifactNames\": []} That's all.";

    var result = decomposer.parseResult(response);

    assertThat(result.success()).isTrue();
    assertThat(result.summary()).isEqualTo("Task complete");
  }

  @Test
  void parseResult_nullInput_returnsFailure() {
    var result = decomposer.parseResult(null);

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isEqualTo("Empty response from decomposer");
  }

  @Test
  void parseResult_blankInput_returnsFailure() {
    var result = decomposer.parseResult("   ");

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isEqualTo("Empty response from decomposer");
  }

  @Test
  void parseResult_bareText_treatsAsSuccessSummary() {
    var result = decomposer.parseResult("I successfully completed the requested task.");

    assertThat(result.success()).isTrue();
    assertThat(result.summary()).isEqualTo("I successfully completed the requested task.");
    assertThat(result.artifactNames()).isEmpty();
  }

  @Test
  void parseResult_multipleArtifacts_parsesAll() {
    var result = decomposer.parseResult(
        "{\"success\": true, \"summary\": \"Created assets\", "
            + "\"artifactNames\": [\"img-001\", \"img-002\", \"audio-001\"]}");

    assertThat(result.artifactNames()).containsExactly("img-001", "img-002", "audio-001");
  }
}
