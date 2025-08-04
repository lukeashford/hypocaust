package com.example.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.web",
    "com.example.model",
    "com.example.node",
    "com.example.tool",
    "com.example.infrastructure"
})
public class TheMachineApplication {

  public static void main(String[] args) {
    SpringApplication.run(TheMachineApplication.class, args);
  }

}
