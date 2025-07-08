package com.example.the_machine.langchain;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Hello World LangChain agent. Provides endpoints to interact with the
 * agent.
 */
@RestController
@RequestMapping("/api/langchain")
public class BrandIntelController {

  private final BrandIntelService brandIntelService;

  public BrandIntelController(BrandIntelService brandIntelService) {
    this.brandIntelService = brandIntelService;
  }

  /**
   * Endpoint to get a greeting from the LangChain agent.
   *
   * @param name The name to greet (optional, defaults to "World")
   * @return A greeting message from the agent
   */
  @GetMapping("/brand")
  public String getGreeting(@RequestParam(defaultValue = "Luke Ashford") String name) {
    return brandIntelService.analyzeBrand(name);
  }
}