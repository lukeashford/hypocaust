package com.example.hypocaust.service.analysis;

import com.example.hypocaust.service.staging.PendingUpload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VideoAnalyzer implements ArtifactContentAnalyzer {

  @Override
  public AnalysisResult analyze(PendingUpload upload) {
    // Video analysis requires FFmpeg integration (not yet available).
    return new AnalysisResult("uploaded_video", "Uploaded Video",
        "User-uploaded video (analysis requires FFmpeg integration)", null);
  }
}
