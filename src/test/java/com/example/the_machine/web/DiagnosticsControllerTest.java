package com.example.the_machine.web;

import static com.example.the_machine.common.DiagnosticsConstants.EXPECTED_RESPONSE;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_CONNECTION_VALID;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_DATABASE;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_HEALTHY_MODELS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_MATCHES;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_MODEL;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_OVERALL_STATUS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_QUERY_SUCCESS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_RESPONSE;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_STATUS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_TOTAL_MODELS;
import static com.example.the_machine.common.DiagnosticsConstants.FIELD_UNHEALTHY_MODELS;
import static com.example.the_machine.common.DiagnosticsConstants.STATUS_HEALTHY;
import static com.example.the_machine.common.DiagnosticsConstants.STATUS_SOME_UNHEALTHY;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.the_machine.service.DatabaseHealthService;
import com.example.the_machine.service.ModelHealthService;
import java.util.Map;
import java.util.Set;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiagnosticsController.class)
class DiagnosticsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ModelHealthService modelHealthService;

  @MockitoBean
  private DatabaseHealthService databaseHealthService;

  @Test
  void testCheckModelHealth() throws Exception {
    // Given
    val modelName = "openai:gpt4o";
    Map<String, Object> healthResult = Map.of(
        FIELD_MODEL, modelName,
        FIELD_STATUS, STATUS_HEALTHY,
        FIELD_RESPONSE, EXPECTED_RESPONSE,
        FIELD_MATCHES, true
    );

    when(modelHealthService.checkModelHealth(modelName)).thenReturn(healthResult);

    // When & Then
    mockMvc.perform(get("/api/diagnostics/models/{modelName}/health", modelName))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.model").value(modelName))
        .andExpect(jsonPath("$.status").value(STATUS_HEALTHY))
        .andExpect(jsonPath("$.response").value(EXPECTED_RESPONSE))
        .andExpect(jsonPath("$.matches").value(true));
  }

  @Test
  void testCheckAllModelsHealth() throws Exception {
    // Given
    Map<String, Object> allHealthResult = Map.of(
        FIELD_TOTAL_MODELS, 2,
        FIELD_HEALTHY_MODELS, 1,
        FIELD_UNHEALTHY_MODELS, 1,
        FIELD_OVERALL_STATUS, STATUS_SOME_UNHEALTHY
    );

    when(modelHealthService.checkAllModelsHealth()).thenReturn(allHealthResult);

    // When & Then
    mockMvc.perform(get("/api/diagnostics/models/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalModels").value(2))
        .andExpect(jsonPath("$.healthyModels").value(1))
        .andExpect(jsonPath("$.unhealthyModels").value(1))
        .andExpect(jsonPath("$.overallStatus").value(STATUS_SOME_UNHEALTHY));
  }

  @Test
  void testListAvailableModels() throws Exception {
    // Given
    val models = Set.of("openai:gpt4o", "anthropic:sonnet4");
    when(modelHealthService.listAvailableModels()).thenReturn(models);

    // When & Then
    mockMvc.perform(get("/api/diagnostics/models"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void testCheckDatabaseHealth() throws Exception {
    // Given
    Map<String, Object> dbHealthResult = Map.of(
        FIELD_DATABASE, "postgresql",
        FIELD_STATUS, STATUS_HEALTHY,
        FIELD_CONNECTION_VALID, true,
        FIELD_QUERY_SUCCESS, true
    );

    when(databaseHealthService.checkDatabaseHealth()).thenReturn(dbHealthResult);

    // When & Then
    mockMvc.perform(get("/api/diagnostics/database/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.database").value("postgresql"))
        .andExpect(jsonPath("$.status").value(STATUS_HEALTHY))
        .andExpect(jsonPath("$.connectionValid").value(true))
        .andExpect(jsonPath("$.querySuccess").value(true));
  }
}