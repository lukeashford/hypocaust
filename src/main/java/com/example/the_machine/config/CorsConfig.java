package com.example.the_machine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/threads/*/events")
        .allowedOrigins("*") // Add your React dev server ports
        .allowedMethods("*")
        .allowedHeaders("*")
        .exposedHeaders("*");
  }
}