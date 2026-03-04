package com.example.hypocaust.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class NamingUtilsTest {

  @Test
  void sanitize_trimsAndReplacesCorrectly() {
    assertThat(NamingUtils.sanitize("  Hello World!  ", 50)).isEqualTo("hello_world");
    assertThat(NamingUtils.sanitize("Task: Make a Cat", 50)).isEqualTo("task_make_a_cat");
    assertThat(NamingUtils.sanitize("__multiple__underscores__", 50)).isEqualTo(
        "multiple_underscores");
    assertThat(NamingUtils.sanitize("123-abc-XYZ", 50)).isEqualTo("123_abc_xyz");
  }

  @Test
  void sanitize_respectsMaxLength() {
    String longName = "this_is_a_very_long_name_that_should_be_truncated_at_some_point";
    assertThat(NamingUtils.sanitize(longName, 10)).isEqualTo("this_is_a");
    assertThat(NamingUtils.sanitize(longName, 11)).isEqualTo("this_is_a_v");
  }

  @Test
  void sanitize_removesTrailingUnderscoreAfterTruncation() {
    assertThat(NamingUtils.sanitize("hello_world", 6)).isEqualTo("hello");
  }

  @Test
  void appendCounterIfExists_appendsCorrectCounter() {
    Set<String> taken = Set.of("cat_art", "cat_art_2");
    assertThat(NamingUtils.appendCounterIfExists("cat_art", taken)).isEqualTo("cat_art_3");
    assertThat(NamingUtils.appendCounterIfExists("new_art", taken)).isEqualTo("new_art");
  }

  @Test
  void appendCounterIfExists_worksWithMultipleCollisions() {
    Set<String> taken = Set.of("art", "art_2", "art_3", "art_4");
    assertThat(NamingUtils.appendCounterIfExists("art", taken)).isEqualTo("art_5");
  }
}
