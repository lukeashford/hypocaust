package com.example.hypocaust.models;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Component
public class RetryMatcher {

  public boolean isTransient(Throwable e) {
    if (e instanceof ResourceAccessException) {
      return true;
    }
    if (e instanceof HttpServerErrorException serverError) {
      int code = serverError.getStatusCode().value();
      return code == 502 || code == 503 || code == 504;
    }
    if (e instanceof HttpClientErrorException clientError) {
      return clientError.getStatusCode().value() == 429;
    }
    // Walk the full cause chain for underlying connection issues
    Throwable cause = e;
    while (cause != null) {
      if (cause instanceof ConnectException || cause instanceof SocketTimeoutException) {
        return true;
      }
      cause = cause.getCause();
    }
    String msg = e.getMessage();
    if (msg != null) {
      String lower = msg.toLowerCase();
      return lower.contains("timed out") || lower.contains("connection refused")
          || lower.contains("service unavailable") || lower.contains("rate limit")
          || lower.contains("too many requests");
    }
    return false;
  }
}
