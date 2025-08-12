package com.example.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.api.dto.CompanyAnalysisDto;
import com.example.api.exception.BrandAnalysisException;
import com.example.graph.RetrievalState;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.val;
import org.bsc.langgraph4j.CompiledGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Simple test to verify that the BrandIntelService can be created with mocked dependencies. This
 * demonstrates that the dependency injection is set up correctly with the Configuration
 * architecture.
 */
@ExtendWith(MockitoExtension.class)
class BrandIntelServiceTest {

  @Mock
  private CompiledGraph<RetrievalState> stateGraph;

  @Mock
  private StoryGenerationService storyService;

  @Mock
  private VisualConceptsService visualConceptsService;

  @Mock
  private VisualAssetsService visualAssetsService;

  @Mock
  private TreatmentService treatmentService;

  private BrandIntelService brandIntelService;

  @BeforeEach
  void setUp() {
    // Create the service with the mocked dependencies
    brandIntelService = new BrandIntelService(stateGraph, storyService, visualConceptsService,
        visualAssetsService, treatmentService);
  }

  @Test
  void contextLoads() {
    // Verify that the service is created with the mocked dependencies
    assertNotNull(brandIntelService);
  }

  @Test
  void analyzeBrand_SuccessfulAnalysis_ReturnsDto() {
    // Arrange
    val companyName = "TestCompany";
    val expectedDto = new CompanyAnalysisDto(
        "Test summary",
        List.of("Point 1", "Point 2"),
        "Brand personality",
        "Target audience",
        "Visual style",
        List.of("Key message 1"),
        List.of("Advantage 1")
    );

    Map<String, Object> stateData = Map.of(RetrievalState.ANALYSIS_KEY, expectedDto);
    val mockState = new RetrievalState(stateData);

    when(stateGraph.invoke(any())).thenReturn(Optional.of(mockState));

    // Act
    val result = brandIntelService.analyzeBrand(companyName);

    // Assert
    assertNotNull(result);
    assertEquals(expectedDto.summary(), result.summary());
  }

  @Test
  void analyzeBrand_NoDataAvailable_ThrowsBrandAnalysisException() {
    // Arrange
    val companyName = "TestCompany";
    // Create state without ANALYSIS_KEY to simulate no data available
    Map<String, Object> stateData = Map.of();
    val mockState = new RetrievalState(stateData);

    when(stateGraph.invoke(any())).thenReturn(Optional.of(mockState));

    // Act & Assert
    val exception = assertThrows(
        BrandAnalysisException.class,
        () -> brandIntelService.analyzeBrand(companyName)
    );

    assertEquals(companyName, exception.getCompanyName());
    assertEquals("NO_DATA_AVAILABLE", exception.getErrorCode());
    assertEquals("No analysis data available", exception.getMessage());
  }

  @Test
  void analyzeBrand_InvocationFailed_ThrowsBrandAnalysisException() {
    // Arrange
    val companyName = "TestCompany";

    when(stateGraph.invoke(any())).thenReturn(Optional.empty());

    // Act & Assert
    val exception = assertThrows(
        BrandAnalysisException.class,
        () -> brandIntelService.analyzeBrand(companyName)
    );

    assertEquals(companyName, exception.getCompanyName());
    assertEquals("INVOCATION_FAILED", exception.getErrorCode());
    assertEquals("Analysis invocation failed - no result returned", exception.getMessage());
  }

  @Test
  void analyzeBrand_UnexpectedError_ThrowsBrandAnalysisException() {
    // Arrange
    val companyName = "TestCompany";
    val cause = new RuntimeException("Unexpected database error");

    when(stateGraph.invoke(any())).thenThrow(cause);

    // Act & Assert
    val exception = assertThrows(
        BrandAnalysisException.class,
        () -> brandIntelService.analyzeBrand(companyName)
    );

    assertEquals(companyName, exception.getCompanyName());
    assertEquals("UNEXPECTED_ERROR", exception.getErrorCode());
    assertEquals("Unexpected error during brand analysis", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }
}
