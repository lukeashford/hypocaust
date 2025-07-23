package com.example.the_machine.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.example.the_machine.model.ChatModelProvider;
import com.example.the_machine.retriever.ContentRetrieverProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Simple test to verify that the BrandIntelService can be created with mocked dependencies. This
 * demonstrates that the dependency injection is set up correctly.
 */
@ExtendWith(MockitoExtension.class)
class BrandIntelServiceTest {

  @Mock
  private ChatModelProvider chatModelProvider;

  @Mock
  private ContentRetrieverProvider contentRetrieverProvider;

  @Mock
  private ChatModel chatModel;

  @Mock
  private ContentRetriever contentRetriever;

  private BrandIntelService brandIntelService;

  @BeforeEach
  void setUp() {
    // Set up the mocks to return non-null values
    when(chatModelProvider.getChatModel()).thenReturn(chatModel);
    when(contentRetrieverProvider.getContentRetriever()).thenReturn(contentRetriever);

    // Manually create the service with the mocked dependencies
    brandIntelService = new BrandIntelService(chatModelProvider, contentRetrieverProvider);
  }

  @Test
  void contextLoads() {
    // Verify that the service is created with the mocked dependencies
    assertNotNull(brandIntelService);
  }
}
