package com.example.hypocaust.token;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default implementation of TokenizerFactory using jtokkit encodings.
 */
@Service
@RequiredArgsConstructor
public final class DefaultTokenizerFactory implements TokenizerFactory {

  @Override
  public Tokenizer forModel(final String modelName) {
    // Map model name patterns to appropriate encodings
    // For most modern OpenAI/Anthropic-like chat models, CL100K_BASE is a reasonable baseline
    final var registry = Encodings.newDefaultEncodingRegistry();

    final var encoding = registry
        .getEncodingForModel(modelName)
        .orElse(registry.getEncoding(EncodingType.CL100K_BASE));

    return new OpenAiLikeTokenizer(encoding);
  }
}