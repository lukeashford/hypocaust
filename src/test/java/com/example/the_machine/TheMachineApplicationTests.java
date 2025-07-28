package com.example.the_machine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestConfig.class)
@TestPropertySource(properties = {
    "openai.api-key=test-api-key",
    "google.custom.api-key=test-google-api-key",
    "google.custom.csi=test-google-csi",
    "app.chat-model=openai",
    "app.search-engine=google",
    "app.candidate-discovery=web-search",
    "app.content-retriever=optimized",
    "app.extractor=composite",
    "app.embedding=local",
    "app.topK=10"
})
class TheMachineApplicationTests {

  @Test
  void contextLoads() {
  }

}
