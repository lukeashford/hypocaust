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

  private BrandIntelService brandIntelService;

  @BeforeEach
  void setUp() {
    // Create the service with the mocked StateGraph
    brandIntelService = new BrandIntelService(stateGraph);
  }

  @Test
  void contextLoads() {
    // Verify that the service is created with the mocked dependencies
    assertNotNull(brandIntelService);
  }

  @Test
  void analyzeBrand_SuccessfulAnalysis_ReturnsDto() {
    // Arrange
    String companyName = "TestCompany";
    CompanyAnalysisDto expectedDto = new CompanyAnalysisDto(
        "Test summary",
        List.of("Point 1", "Point 2"),
        "Brand personality",
        "Target audience",
        "Visual style",
        List.of("Key message 1"),
        List.of("Advantage 1")
    );

    Map<String, Object> stateData = Map.of(RetrievalState.ANALYSIS_KEY, expectedDto);
    RetrievalState mockState = new RetrievalState(stateData);

    when(stateGraph.invoke(any(Map.class))).thenReturn(Optional.of(mockState));

    // Act
    CompanyAnalysisDto result = brandIntelService.analyzeBrand(companyName);

    // Assert
    assertNotNull(result);
    assertEquals(expectedDto.summary(), result.summary());
  }

  @Test
  void analyzeBrand_NoDataAvailable_ThrowsBrandAnalysisException() {
    // Arrange
    String companyName = "TestCompany";
    // Create state without ANALYSIS_KEY to simulate no data available
    Map<String, Object> stateData = Map.of();
    RetrievalState mockState = new RetrievalState(stateData);

    when(stateGraph.invoke(any(Map.class))).thenReturn(Optional.of(mockState));

    // Act & Assert
    BrandAnalysisException exception = assertThrows(
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
    String companyName = "TestCompany";

    when(stateGraph.invoke(any(Map.class))).thenReturn(Optional.empty());

    // Act & Assert
    BrandAnalysisException exception = assertThrows(
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
    String companyName = "TestCompany";
    RuntimeException cause = new RuntimeException("Unexpected database error");

    when(stateGraph.invoke(any(Map.class))).thenThrow(cause);

    // Act & Assert
    BrandAnalysisException exception = assertThrows(
        BrandAnalysisException.class,
        () -> brandIntelService.analyzeBrand(companyName)
    );

    assertEquals(companyName, exception.getCompanyName());
    assertEquals("UNEXPECTED_ERROR", exception.getErrorCode());
    assertEquals("Unexpected error during brand analysis", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }
}
