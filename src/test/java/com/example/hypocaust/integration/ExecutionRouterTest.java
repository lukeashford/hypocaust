package com.example.hypocaust.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutionRouterTest {

  @Test
  void resolve_byPlatform_returnsCorrectExecutor() {
    ModelExecutor replicate = mock(ModelExecutor.class);
    when(replicate.platform()).thenReturn(Platform.REPLICATE);

    ModelExecutor fal = mock(ModelExecutor.class);
    when(fal.platform()).thenReturn(Platform.FAL);

    var router = new ExecutionRouter(List.of(replicate, fal));

    assertThat(router.resolve(Platform.REPLICATE)).isSameAs(replicate);
    assertThat(router.resolve(Platform.FAL)).isSameAs(fal);
  }

  @Test
  void resolve_byString_returnsCorrectExecutor() {
    ModelExecutor replicate = mock(ModelExecutor.class);
    when(replicate.platform()).thenReturn(Platform.REPLICATE);

    var router = new ExecutionRouter(List.of(replicate));

    assertThat(router.resolve("REPLICATE")).isSameAs(replicate);
  }

  @Test
  void resolve_unknownPlatform_throwsException() {
    ModelExecutor replicate = mock(ModelExecutor.class);
    when(replicate.platform()).thenReturn(Platform.REPLICATE);

    var router = new ExecutionRouter(List.of(replicate));

    assertThatThrownBy(() -> router.resolve(Platform.FAL))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("FAL");
  }
}
