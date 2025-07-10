package com.example.the_machine.langchain;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(HelloWorldAgentControllerTest.TestConfig.class)
public class HelloWorldAgentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private HelloWorldAgentService helloWorldAgentService;

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    public HelloWorldAgentService helloWorldAgentService() {
      return Mockito.mock(HelloWorldAgentService.class);
    }
  }

  @Test
  public void testGetGreeting() throws Exception {
    // Arrange
    String testName = "TestUser";
    String expectedGreeting = "Hello, TestUser! How can I assist you today?";
    when(helloWorldAgentService.getGreeting(testName)).thenReturn(expectedGreeting);

    // Act & Assert
    mockMvc.perform(get("/api/langchain/greeting")
            .param("name", testName))
        .andExpect(status().isOk())
        .andExpect(content().string(expectedGreeting));
  }

  @Test
  public void testGetGreetingWithDefaultName() throws Exception {
    // Arrange
    String expectedGreeting = "Hello, World! How can I assist you today?";
    when(helloWorldAgentService.getGreeting("World")).thenReturn(expectedGreeting);

    // Act & Assert
    mockMvc.perform(get("/api/langchain/greeting"))
        .andExpect(status().isOk())
        .andExpect(content().string(expectedGreeting));
  }
}
