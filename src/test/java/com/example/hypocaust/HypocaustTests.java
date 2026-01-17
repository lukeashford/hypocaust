package com.example.hypocaust;

import com.example.hypocaust.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class HypocaustTests {

  @MockitoBean
  private StorageService storageService;

  @Test
  void contextLoads() {
  }
}
