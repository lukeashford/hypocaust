package com.example.the_machine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TheMachineApplication {

  public static void main(String[] args) {
    SpringApplication.run(TheMachineApplication.class, args);
  }

}
