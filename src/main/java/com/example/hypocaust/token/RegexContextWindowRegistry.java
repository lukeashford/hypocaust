package com.example.hypocaust.token;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Registry of known context window sizes for common models. Matching is done via ordered regex
 * rules with boundaries to avoid false positives.
 */
@Service
public final class RegexContextWindowRegistry implements ContextWindowRegistry {

  private static final int DEFAULT_WINDOW = 32_000;

  // Order matters: first matching rule wins
  private static final List<Rule> RULES = List.of(
      rule("\\bgpt-4o(\\b|-)", 128_000),
      rule("\\bgpt-4(\\b|-)", 128_000),

      rule("\\bgpt-3\\.5(\\b|-)", 16_000),

      rule("\\bclaude-3\\.5(\\b|-)", 200_000),
      rule("\\bclaude-3(\\b|-)", 200_000),
      rule("\\bclaude(\\b|-)", 100_000)
  );

  @Override
  public int getContextWindow(final String modelName) {
    if (modelName == null || modelName.isBlank()) {
      return DEFAULT_WINDOW;
    }
    final var needle = modelName.trim();

    for (final var r : RULES) {
      if (r.pattern.matcher(needle).find()) {
        return r.window;
      }
    }
    return DEFAULT_WINDOW;
  }

  private static Rule rule(String regex, final int window) {
    return new Rule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), window);
  }

  private record Rule(Pattern pattern, int window) {

  }
}
