package com.example.hypocaust.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PromptBuilderTest {

  private static PromptFragment fragment(String id, int priority, String text) {
    return new PromptFragment() {
      @Override
      public String text() {
        return text;
      }

      @Override
      public String id() {
        return id;
      }

      @Override
      public int priority() {
        return priority;
      }
    };
  }

  @Test
  void placeholderResolution_replacesValues() {
    var prompt = PromptBuilder.create()
        .with(fragment("f1", 1, "Max children: {{maxChildren}}, retries: {{maxRetries}}"))
        .param("maxChildren", 3)
        .param("maxRetries", 2)
        .build();

    assertThat(prompt).isEqualTo("Max children: 3, retries: 2");
  }

  @Test
  void priorityOrdering_lowerFirst() {
    var prompt = PromptBuilder.create()
        .with(fragment("c", 30, "Third"))
        .with(fragment("a", 10, "First"))
        .with(fragment("b", 20, "Second"))
        .build();

    assertThat(prompt).isEqualTo("First\n\nSecond\n\nThird");
  }

  @Test
  void deduplication_byFragmentId() {
    var prompt = PromptBuilder.create()
        .with(fragment("same-id", 1, "Original"))
        .with(fragment("same-id", 1, "Duplicate"))
        .build();

    assertThat(prompt).isEqualTo("Original");
  }

  @Test
  void emptyBuilder_producesEmptyString() {
    var prompt = PromptBuilder.create().build();

    assertThat(prompt).isEmpty();
  }

  @Test
  void nullFragment_ignored() {
    var prompt = PromptBuilder.create()
        .with(null)
        .with(fragment("f1", 1, "Hello"))
        .build();

    assertThat(prompt).isEqualTo("Hello");
  }

  @Test
  void customSeparator() {
    var prompt = PromptBuilder.create()
        .separator(" | ")
        .with(fragment("a", 1, "A"))
        .with(fragment("b", 2, "B"))
        .build();

    assertThat(prompt).isEqualTo("A | B");
  }
}
