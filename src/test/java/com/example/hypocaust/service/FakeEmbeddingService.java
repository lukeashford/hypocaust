package com.example.hypocaust.service;

import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class FakeEmbeddingService extends EmbeddingService {

  public FakeEmbeddingService() {
    super(null);
  }

  @Override
  public float[] generateEmbedding(String text) {
    return new float[1536];
  }

  @Override
  public List<float[]> generateEmbeddings(List<String> texts) {
    return texts.stream().map(t -> new float[1536]).toList();
  }
}
