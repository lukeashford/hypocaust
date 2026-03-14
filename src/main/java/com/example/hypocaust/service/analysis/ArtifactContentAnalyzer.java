package com.example.hypocaust.service.analysis;

import com.example.hypocaust.service.staging.PendingUpload;

public interface ArtifactContentAnalyzer {

  AnalysisResult analyze(PendingUpload upload);
}
