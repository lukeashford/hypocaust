package com.example.the_machine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestConfig.class)
@TestPropertySource(properties = {
    "spring.ai.openai.api-key=test-api-key"
})
class TheMachineApplicationTests {

  @Test
  void contextLoads() {
  }

}
