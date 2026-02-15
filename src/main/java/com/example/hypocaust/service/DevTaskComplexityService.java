package com.example.hypocaust.service;

import com.example.hypocaust.domain.ArtifactKind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
@Slf4j
public class DevTaskComplexityService implements TaskComplexityService {

  @Override
  public String evaluate(String task, ArtifactKind kind) {
    log.info("Dev mode: bypassing complexity analysis, returning 'fast'");
    return "fast";
  }
}
