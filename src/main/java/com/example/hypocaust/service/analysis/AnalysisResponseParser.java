package com.example.hypocaust.service.analysis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AnalysisResponseParser {

  private static final Pattern NAME_PATTERN = Pattern.compile("(?m)^name:\\s*(.+)$");
  private static final Pattern TITLE_PATTERN = Pattern.compile("(?m)^title:\\s*(.+)$");
  private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("(?m)^description:\\s*(.+)$");

  private AnalysisResponseParser() {
  }

  static AnalysisResult parse(String response, boolean hasIndexableContent) {
    String name = extract(NAME_PATTERN, response);
    String title = extract(TITLE_PATTERN, response);
    String description = extract(DESCRIPTION_PATTERN, response);

    if (name == null || title == null || description == null) {
      return AnalysisResult.FALLBACK;
    }

    return new AnalysisResult(name.strip(), title.strip(), description.strip(),
        hasIndexableContent);
  }

  private static String extract(Pattern pattern, String text) {
    Matcher matcher = pattern.matcher(text);
    return matcher.find() ? matcher.group(1) : null;
  }
}
