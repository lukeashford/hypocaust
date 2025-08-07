package com.example.web.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.graph.RetrievalState;
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
}
