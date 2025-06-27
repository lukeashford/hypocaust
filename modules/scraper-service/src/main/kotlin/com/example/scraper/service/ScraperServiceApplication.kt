package com.example.scraper.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example"])
class ScraperServiceApplication

fun main(args: Array<String>) {
  runApplication<ScraperServiceApplication>(*args)
}
