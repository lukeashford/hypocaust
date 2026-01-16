package com.example.hypocaust.config;

import com.example.hypocaust.domain.DefaultRequestContext;
import com.example.hypocaust.domain.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class RequestContextConfig {

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.INTERFACES)
  public RequestContext requestContext(HttpServletRequest request) {
    String librechatConversationId = request.getHeader("X-Conversation-ID");

    return new DefaultRequestContext(librechatConversationId);
  }
}
