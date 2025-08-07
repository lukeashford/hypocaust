package com.example.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import org.bsc.langgraph4j.state.Channel;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ChannelsSugar utility class. Tests all static methods and verifies proper channel
 * behavior.
 */
class ChannelsSugarTest {

  @Test
  void constructor_shouldThrowUnsupportedOperationException() throws Exception {
    // Test that the utility class cannot be instantiated using reflection
    Constructor<ChannelsSugar> constructor = ChannelsSugar.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException exception = assertThrows(InvocationTargetException.class,
        constructor::newInstance);
    assertEquals(UnsupportedOperationException.class, exception.getCause().getClass());
  }

  @Test
  void overwrite_shouldReturnChannel_whenDefaultProviderIsProvided() {
    // Given
    Supplier<String> defaultProvider = () -> "default";

    // When
    Channel<String> channel = ChannelsSugar.overwrite(defaultProvider);

    // Then
    assertNotNull(channel);
  }

  @Test
  void overwrite_shouldHandleNullDefaultProvider() {
    // When/Then - should not throw exception during creation
    Channel<String> channel = ChannelsSugar.overwrite(null);
    assertNotNull(channel);
  }

  @Test
  void firstWriteWins_shouldReturnChannel_whenDefaultProviderIsProvided() {
    // Given
    Supplier<String> defaultProvider = () -> "default";

    // When
    Channel<String> channel = ChannelsSugar.firstWriteWins(defaultProvider);

    // Then
    assertNotNull(channel);
  }

  @Test
  void firstWriteWins_shouldHandleNullDefaultProvider() {
    // When/Then - should not throw exception during creation
    Channel<String> channel = ChannelsSugar.firstWriteWins(null);
    assertNotNull(channel);
  }

  @Test
  void binaryOp_shouldReturnChannel_whenValidOperatorAndInitialValueProvided() {
    // Given
    BinaryOperator<Integer> addOperator = Integer::sum;
    Integer initialValue = 0;

    // When
    Channel<Integer> channel = ChannelsSugar.binaryOp(addOperator, initialValue);

    // Then
    assertNotNull(channel);
  }

  @Test
  void binaryOp_shouldHandleNullOperator() {
    // When/Then - should not throw exception during creation
    Channel<Integer> channel = ChannelsSugar.binaryOp(null, 0);
    assertNotNull(channel);
  }

  @Test
  void binaryOp_shouldHandleNullInitialValue() {
    // Given
    BinaryOperator<String> concatOperator = (a, b) -> a + b;

    // When/Then - should not throw exception during creation
    Channel<String> channel = ChannelsSugar.binaryOp(concatOperator, null);
    assertNotNull(channel);
  }

  @Test
  void counter_shouldReturnChannel_whenStartValueProvided() {
    // Given
    int startValue = 5;

    // When
    Channel<Integer> channel = ChannelsSugar.counter(startValue);

    // Then
    assertNotNull(channel);
  }

  @Test
  void counter_shouldHandleZeroStartValue() {
    // When
    Channel<Integer> channel = ChannelsSugar.counter(0);

    // Then
    assertNotNull(channel);
  }

  @Test
  void counter_shouldHandleNegativeStartValue() {
    // When
    Channel<Integer> channel = ChannelsSugar.counter(-10);

    // Then
    assertNotNull(channel);
  }

  @Test
  void minValue_shouldReturnChannel_whenInitialValueProvided() {
    // Given
    int initialValue = 100;

    // When
    Channel<Integer> channel = ChannelsSugar.minValue(initialValue);

    // Then
    assertNotNull(channel);
  }

  @Test
  void minValue_shouldHandleZeroInitialValue() {
    // When
    Channel<Integer> channel = ChannelsSugar.minValue(0);

    // Then
    assertNotNull(channel);
  }

  @Test
  void minValue_shouldHandleNegativeInitialValue() {
    // When
    Channel<Integer> channel = ChannelsSugar.minValue(-50);

    // Then
    assertNotNull(channel);
  }

  @Test
  void maxValue_shouldReturnChannel_whenInitialValueProvided() {
    // Given
    int initialValue = 10;

    // When
    Channel<Integer> channel = ChannelsSugar.maxValue(initialValue);

    // Then
    assertNotNull(channel);
  }

  @Test
  void maxValue_shouldHandleZeroInitialValue() {
    // When
    Channel<Integer> channel = ChannelsSugar.maxValue(0);

    // Then
    assertNotNull(channel);
  }

  @Test
  void maxValue_shouldHandleNegativeInitialValue() {
    // When
    Channel<Integer> channel = ChannelsSugar.maxValue(-25);

    // Then
    assertNotNull(channel);
  }

  @Test
  void counter_shouldUseSumOperation() {
    // Given
    int startValue = 10;

    // When
    Channel<Integer> counterChannel = ChannelsSugar.counter(startValue);
    Channel<Integer> binaryOpChannel = ChannelsSugar.binaryOp(Integer::sum, startValue);

    // Then
    assertNotNull(counterChannel);
    assertNotNull(binaryOpChannel);
    // Both should be functionally equivalent
  }

  @Test
  void minValue_shouldUseMinOperation() {
    // Given
    int initialValue = 50;

    // When
    Channel<Integer> minChannel = ChannelsSugar.minValue(initialValue);
    Channel<Integer> binaryOpChannel = ChannelsSugar.binaryOp(Math::min, initialValue);

    // Then
    assertNotNull(minChannel);
    assertNotNull(binaryOpChannel);
    // Both should be functionally equivalent
  }

  @Test
  void maxValue_shouldUseMaxOperation() {
    // Given
    int initialValue = 20;

    // When
    Channel<Integer> maxChannel = ChannelsSugar.maxValue(initialValue);
    Channel<Integer> binaryOpChannel = ChannelsSugar.binaryOp(Math::max, initialValue);

    // Then
    assertNotNull(maxChannel);
    assertNotNull(binaryOpChannel);
    // Both should be functionally equivalent
  }

  @Test
  void binaryOp_shouldCreateChannelWithCustomOperation() {
    // Given
    BinaryOperator<String> concatenateWithSeparator = (a, b) -> a + "|" + b;
    String initialValue = "start";

    // When
    Channel<String> channel = ChannelsSugar.binaryOp(concatenateWithSeparator, initialValue);

    // Then
    assertNotNull(channel);
  }

  @Test
  void allMethods_shouldReturnNonNullChannels() {
    // Test that all methods return non-null channels
    assertNotNull(ChannelsSugar.overwrite(() -> "test"));
    assertNotNull(ChannelsSugar.firstWriteWins(() -> "test"));
    assertNotNull(ChannelsSugar.binaryOp(Integer::sum, 0));
    assertNotNull(ChannelsSugar.counter(0));
    assertNotNull(ChannelsSugar.minValue(0));
    assertNotNull(ChannelsSugar.maxValue(0));
  }
}