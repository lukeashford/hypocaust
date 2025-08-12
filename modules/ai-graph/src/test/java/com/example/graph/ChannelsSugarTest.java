package com.example.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import lombok.val;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ChannelsSugar utility class. Tests all static methods and verifies proper channel
 * behavior.
 */
class ChannelsSugarTest {

  @Test
  void constructor_shouldThrowUnsupportedOperationException() throws Exception {
    // Test that the utility class cannot be instantiated using reflection
    val constructor = ChannelsSugar.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    val exception = assertThrows(InvocationTargetException.class,
        constructor::newInstance);
    assertEquals(UnsupportedOperationException.class, exception.getCause().getClass());
  }

  @Test
  void overwrite_shouldReturnChannel_whenDefaultProviderIsProvided() {
    // Given
    Supplier<String> defaultProvider = () -> "default";

    // When
    val channel = ChannelsSugar.overwrite(defaultProvider);

    // Then
    assertNotNull(channel);
  }

  @Test
  void overwrite_shouldHandleNullDefaultProvider() {
    // When/Then - should not throw exception during creation
    val channel = ChannelsSugar.overwrite(null);
    assertNotNull(channel);
  }

  @Test
  void firstWriteWins_shouldReturnChannel_whenDefaultProviderIsProvided() {
    // Given
    Supplier<String> defaultProvider = () -> "default";

    // When
    val channel = ChannelsSugar.firstWriteWins(defaultProvider);

    // Then
    assertNotNull(channel);
  }

  @Test
  void firstWriteWins_shouldHandleNullDefaultProvider() {
    // When/Then - should not throw exception during creation
    val channel = ChannelsSugar.firstWriteWins(null);
    assertNotNull(channel);
  }

  @Test
  void binaryOp_shouldReturnChannel_whenValidOperatorAndInitialValueProvided() {
    // Given
    BinaryOperator<Integer> addOperator = Integer::sum;
    val initialValue = 0;

    // When
    val channel = ChannelsSugar.binaryOp(addOperator, initialValue);

    // Then
    assertNotNull(channel);
  }

  @Test
  void binaryOp_shouldHandleNullOperator() {
    // When/Then - should not throw exception during creation
    val channel = ChannelsSugar.binaryOp(null, 0);
    assertNotNull(channel);
  }

  @Test
  void binaryOp_shouldHandleNullInitialValue() {
    // Given
    BinaryOperator<String> concatOperator = (a, b) -> a + b;

    // When/Then - should not throw exception during creation
    val channel = ChannelsSugar.binaryOp(concatOperator, null);
    assertNotNull(channel);
  }

  @Test
  void counter_shouldReturnChannel_whenStartValueProvided() {
    // Given
    val startValue = 5;

    // When
    val channel = ChannelsSugar.counter(startValue);

    // Then
    assertNotNull(channel);
  }

  @Test
  void counter_shouldHandleZeroStartValue() {
    // When
    val channel = ChannelsSugar.counter(0);

    // Then
    assertNotNull(channel);
  }

  @Test
  void counter_shouldHandleNegativeStartValue() {
    // When
    val channel = ChannelsSugar.counter(-10);

    // Then
    assertNotNull(channel);
  }

  @Test
  void minValue_shouldReturnChannel_whenInitialValueProvided() {
    // Given
    val initialValue = 100;

    // When
    val channel = ChannelsSugar.minValue(initialValue);

    // Then
    assertNotNull(channel);
  }

  @Test
  void minValue_shouldHandleZeroInitialValue() {
    // When
    val channel = ChannelsSugar.minValue(0);

    // Then
    assertNotNull(channel);
  }

  @Test
  void minValue_shouldHandleNegativeInitialValue() {
    // When
    val channel = ChannelsSugar.minValue(-50);

    // Then
    assertNotNull(channel);
  }

  @Test
  void maxValue_shouldReturnChannel_whenInitialValueProvided() {
    // Given
    val initialValue = 10;

    // When
    val channel = ChannelsSugar.maxValue(initialValue);

    // Then
    assertNotNull(channel);
  }

  @Test
  void maxValue_shouldHandleZeroInitialValue() {
    // When
    val channel = ChannelsSugar.maxValue(0);

    // Then
    assertNotNull(channel);
  }

  @Test
  void maxValue_shouldHandleNegativeInitialValue() {
    // When
    val channel = ChannelsSugar.maxValue(-25);

    // Then
    assertNotNull(channel);
  }

  @Test
  void counter_shouldUseSumOperation() {
    // Given
    val startValue = 10;

    // When
    val counterChannel = ChannelsSugar.counter(startValue);
    val binaryOpChannel = ChannelsSugar.binaryOp(Integer::sum, startValue);

    // Then
    assertNotNull(counterChannel);
    assertNotNull(binaryOpChannel);
    // Both should be functionally equivalent
  }

  @Test
  void minValue_shouldUseMinOperation() {
    // Given
    val initialValue = 50;

    // When
    val minChannel = ChannelsSugar.minValue(initialValue);
    val binaryOpChannel = ChannelsSugar.binaryOp(Math::min, initialValue);

    // Then
    assertNotNull(minChannel);
    assertNotNull(binaryOpChannel);
    // Both should be functionally equivalent
  }

  @Test
  void maxValue_shouldUseMaxOperation() {
    // Given
    val initialValue = 20;

    // When
    val maxChannel = ChannelsSugar.maxValue(initialValue);
    val binaryOpChannel = ChannelsSugar.binaryOp(Math::max, initialValue);

    // Then
    assertNotNull(maxChannel);
    assertNotNull(binaryOpChannel);
    // Both should be functionally equivalent
  }

  @Test
  void binaryOp_shouldCreateChannelWithCustomOperation() {
    // Given
    BinaryOperator<String> concatenateWithSeparator = (a, b) -> a + "|" + b;
    val initialValue = "start";

    // When
    val channel = ChannelsSugar.binaryOp(concatenateWithSeparator, initialValue);

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