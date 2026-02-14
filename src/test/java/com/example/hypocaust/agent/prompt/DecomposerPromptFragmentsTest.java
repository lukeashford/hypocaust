package com.example.hypocaust.agent.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.prompt.PromptBuilder;
import org.junit.jupiter.api.Test;

class DecomposerPromptFragmentsTest {

  @Test
  void core_containsDecompositionAlgorithm() {
    var fragment = DecomposerPromptFragments.core();

    assertThat(fragment.text()).contains("recursive task decomposition");
    assertThat(fragment.text()).contains("{{maxRetries}}");
    assertThat(fragment.text()).contains("{{maxChildren}}");
    assertThat(fragment.text()).contains("Never mix these");
    assertThat(fragment.id()).isEqualTo("decomposer-core");
    assertThat(fragment.priority()).isEqualTo(10);
  }

  @Test
  void artifactAwareness_containsContextGuidance() {
    var fragment = DecomposerPromptFragments.artifactAwareness();

    assertThat(fragment.text()).contains("existing artifacts");
    assertThat(fragment.text()).contains("project context");
    assertThat(fragment.id()).isEqualTo("decomposer-artifact-awareness");
    assertThat(fragment.priority()).isEqualTo(20);
  }

  @Test
  void selfHealing_containsRetryProtocol() {
    var fragment = DecomposerPromptFragments.selfHealing();

    assertThat(fragment.text()).contains("Diagnose the cause");
    assertThat(fragment.text()).contains("different approach");
    assertThat(fragment.text()).contains("{{maxRetries}}");
    assertThat(fragment.id()).isEqualTo("decomposer-self-healing");
    assertThat(fragment.priority()).isEqualTo(30);
  }

  @Test
  void placeholderResolution_replacesAllPlaceholders() {
    var prompt = PromptBuilder.create()
        .with(DecomposerPromptFragments.core())
        .with(DecomposerPromptFragments.selfHealing())
        .param("maxChildren", 3)
        .param("maxRetries", 2)
        .build();

    assertThat(prompt).doesNotContain("{{maxChildren}}");
    assertThat(prompt).doesNotContain("{{maxRetries}}");
    assertThat(prompt).contains("max 3");
    assertThat(prompt).contains("max 2");
  }

  @Test
  void priorityOrdering_coreBeforeAwarenessBeforeSelfHealing() {
    var ids = PromptBuilder.create()
        .with(DecomposerPromptFragments.selfHealing())
        .with(DecomposerPromptFragments.core())
        .with(DecomposerPromptFragments.artifactAwareness())
        .fragmentIds();

    assertThat(ids).containsExactly(
        "decomposer-core",
        "decomposer-artifact-awareness",
        "decomposer-self-healing"
    );
  }

  @Test
  void promptBuilder_deduplicatesByFragmentId() {
    var prompt = PromptBuilder.create()
        .with(DecomposerPromptFragments.core())
        .with(DecomposerPromptFragments.core())
        .param("maxRetries", 2)
        .param("maxChildren", 3)
        .build();

    // Count occurrences - should only appear once
    int count = 0;
    int idx = 0;
    while ((idx = prompt.indexOf("recursive task decomposition", idx)) != -1) {
      count++;
      idx++;
    }
    assertThat(count).isEqualTo(1);
  }
}
