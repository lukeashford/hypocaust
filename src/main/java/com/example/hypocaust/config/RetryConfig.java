package com.example.hypocaust.config;

import com.example.hypocaust.models.RetryMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableRetry
public class RetryConfig {

  @Bean
  public RetryTemplate modelRetryTemplate(RetryMatcher retryMatcher) {
    RetryTemplate retryTemplate = new RetryTemplate();

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3) {
      @Override
      public boolean canRetry(RetryContext context) {
        Throwable lastThrowable = context.getLastThrowable();
        return super.canRetry(context) &&
            (lastThrowable == null || retryMatcher.isTransient(lastThrowable));
      }
    };

    ExponentialRandomBackOffPolicy backOffPolicy = new ExponentialRandomBackOffPolicy();
    backOffPolicy.setInitialInterval(1000);
    backOffPolicy.setMultiplier(2.0);

    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setBackOffPolicy(backOffPolicy);

    return retryTemplate;
  }
}
