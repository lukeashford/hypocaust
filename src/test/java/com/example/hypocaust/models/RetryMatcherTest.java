package com.example.hypocaust.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class RetryMatcherTest {

  private final RetryMatcher retryMatcher = new RetryMatcher();

  @Nested
  class IsTransient {

    @Test
    void resourceAccessException_isTransient() {
      assertThat(retryMatcher.isTransient(
          new ResourceAccessException("timeout"))).isTrue();
    }

    @Test
    void httpServerError502_isTransient() {
      assertThat(retryMatcher.isTransient(
          new HttpServerErrorException(HttpStatus.BAD_GATEWAY))).isTrue();
    }

    @Test
    void httpServerError503_isTransient() {
      assertThat(retryMatcher.isTransient(
          new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))).isTrue();
    }

    @Test
    void httpServerError500_isNotTransient() {
      assertThat(retryMatcher.isTransient(
          new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))).isFalse();
    }

    @Test
    void httpClientError429_isTransient() {
      assertThat(retryMatcher.isTransient(
          new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))).isTrue();
    }

    @Test
    void httpClientError400_isNotTransient() {
      assertThat(retryMatcher.isTransient(
          new HttpClientErrorException(HttpStatus.BAD_REQUEST))).isFalse();
    }

    @Test
    void connectExceptionCause_isTransient() {
      assertThat(retryMatcher.isTransient(
          new RuntimeException(new ConnectException("refused")))).isTrue();
    }

    @Test
    void socketTimeoutCause_isTransient() {
      assertThat(retryMatcher.isTransient(
          new RuntimeException(new SocketTimeoutException("timeout")))).isTrue();
    }

    @Test
    void deeplyNestedConnectException_isTransient() {
      var e = new RuntimeException("top",
          new RuntimeException("mid",
              new ConnectException("bottom")));
      assertThat(retryMatcher.isTransient(e)).isTrue();
    }

    @Test
    void messageContainingTimedOut_isTransient() {
      assertThat(retryMatcher.isTransient(
          new RuntimeException("The connection timed out after 10s"))).isTrue();
    }

    @Test
    void messageContainingRateLimit_isTransient() {
      assertThat(retryMatcher.isTransient(
          new RuntimeException("Rate limit reached, try again later"))).isTrue();
    }

    @Test
    void regularException_isNotTransient() {
      assertThat(retryMatcher.isTransient(
          new RuntimeException("Something went wrong"))).isFalse();
    }

    @Test
    void illegalArgumentException_isNotTransient() {
      assertThat(retryMatcher.isTransient(
          new IllegalArgumentException("Invalid input"))).isFalse();
    }
  }
}
