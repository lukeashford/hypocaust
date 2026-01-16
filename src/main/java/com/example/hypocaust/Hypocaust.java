package com.example.hypocaust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Hypocaust {

  public static void main(String[] args) {
    SpringApplication.run(Hypocaust.class, args);
  }

}
