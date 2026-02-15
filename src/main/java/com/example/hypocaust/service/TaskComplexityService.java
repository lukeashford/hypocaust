package com.example.hypocaust.service;

import com.example.hypocaust.domain.ArtifactKind;

public interface TaskComplexityService {

  String evaluate(String task, ArtifactKind kind);
}
