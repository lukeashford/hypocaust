package com.example.hypocaust.service.analysis;

import com.example.hypocaust.db.ArtifactEntity;

public interface ArtifactContentAnalyzer {

  AnalysisResult analyze(ArtifactEntity entity);
}
