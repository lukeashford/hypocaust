package com.example.graph;

import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

/**
 * Utility methods providing syntactic sugar on top of org.bsc.langgraph4j.state.Channels.
 *
 * <p>This class cannot be instantiated and provides only static utility methods
 * for creating commonly used channel configurations.
 */
public final class ChannelsSugar {

  private ChannelsSugar() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /* ---------- single-value helpers ---------- */

  public static <T> Channel<T> overwrite(Supplier<T> defaultProvider) {
    return Channels.base(defaultProvider);
  }

  public static <T> Channel<T> firstWriteWins(Supplier<T> defaultProvider) {
    return Channels.base((prev, ups) -> prev != null ? prev : ups, defaultProvider);
  }

  public static <T> Channel<T> binaryOp(BinaryOperator<T> op, T initial) {
    return Channels.base(
        (prev, next) -> op.apply(Optional.of(prev).orElse(initial), next),
        () -> initial
    );
  }

  public static Channel<Integer> counter(int start) {
    return binaryOp(Integer::sum, start);
  }

  public static Channel<Integer> minValue(int initial) {
    return binaryOp(Math::min, initial);
  }

  public static Channel<Integer> maxValue(int initial) {
    return binaryOp(Math::max, initial);
  }
}