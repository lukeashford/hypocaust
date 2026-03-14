package com.example.hypocaust.service.analysis;

import lombok.experimental.UtilityClass;

@UtilityClass
class AnalysisPrompts {

  static final String OUTPUT_FORMAT = """
      Respond with exactly three lines:
      name: <snake_case_name>
      title: <Human Readable Title>
      description: <One sentence describing what this %s contains>

      The name should be a short, semantic identifier (e.g. "hero_headshot", "movie_script", \
      "battle_sfx"). The title should be human-readable (e.g. "Hero Headshot", "Movie Script", \
      "Battle Sound Effect").""";

  static String outputFormatFor(String contentType) {
    return OUTPUT_FORMAT.formatted(contentType);
  }
}
