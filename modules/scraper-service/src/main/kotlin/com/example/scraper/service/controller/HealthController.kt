package com.example.scraper.service.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {

  @GetMapping("/health")
  fun health(): Map<String, String> {
    return mapOf("status" to "UP", "message" to "ScraperService is running")
  }
}