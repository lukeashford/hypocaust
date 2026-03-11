package com.example.hypocaust.models.anthropic;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpRequest;

/**
 * Intercepts outgoing Anthropic API requests and injects {@code cache_control} markers,
 * enabling Anthropic's prompt-caching feature without any changes at call sites.
 *
 * <p>Strategy: mark the end of the stable, reusable prefix of each request so Anthropic
 * can cache everything up to that point:
 * <ol>
 *   <li>The {@code system} block — always static for a given agent type.</li>
 *   <li>The last {@code tool} definition — tool schemas are also static per call site.</li>
 * </ol>
 *
 * <p>Dynamic content (user messages, tool results) is deliberately left uncached because it
 * changes on every call and would invalidate the cache immediately.
 *
 * <p>Minimum cacheable size is model-dependent (1 024 tokens for Sonnet, 2 048 for Opus/Haiku).
 * Calls below that threshold incur no caching overhead — the API silently ignores the marker.
 */
@Component
@RequiredArgsConstructor
public class AnthropicPromptCachingInterceptor implements ClientHttpRequestInterceptor {

  private static final String MESSAGES_PATH = "/v1/messages";

  private static final String FIELD_SYSTEM = "system";
  private static final String FIELD_TOOLS = "tools";
  private static final String FIELD_TYPE = "type";
  private static final String FIELD_TEXT = "text";
  private static final String FIELD_CACHE_CONTROL = "cache_control";

  private static final String CONTENT_TYPE_TEXT = "text";
  private static final String CACHE_CONTROL_TYPE_EPHEMERAL = "ephemeral";

  private final ObjectMapper objectMapper;

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    if (isMessagesRequest(request)) {
      body = applyPromptCaching(body);
    }

    return execution.execute(request, body);
  }

  private boolean isMessagesRequest(HttpRequest request) {
    return HttpMethod.POST.equals(request.getMethod())
        && request.getURI().getPath().endsWith(MESSAGES_PATH);
  }

  private byte[] applyPromptCaching(byte[] body) throws IOException {
    var root = (ObjectNode) objectMapper.readTree(body);
    markSystemForCaching(root);
    markLastToolForCaching(root);
    return objectMapper.writeValueAsBytes(root);
  }

  /**
   * Converts a plain-string {@code system} value to the structured block format required by the
   * caching API, then marks the last (or only) block as the cache boundary.
   *
   * <p>Anthropic accepts {@code system} as either a plain string or an array of content blocks.
   * Spring AI 1.0 always serialises it as a plain string, so we normalise it here.
   */
  private void markSystemForCaching(ObjectNode root) {
    var system = root.get(FIELD_SYSTEM);
    if (system == null || system.isNull()) {
      return;
    }

    if (system.isTextual()) {
      var block = objectMapper.createObjectNode();
      block.put(FIELD_TYPE, CONTENT_TYPE_TEXT);
      block.put(FIELD_TEXT, system.asText());
      block.set(FIELD_CACHE_CONTROL, ephemeralCacheControl());

      var array = objectMapper.createArrayNode();
      array.add(block);
      root.set(FIELD_SYSTEM, array);

    } else if (system.isArray() && !system.isEmpty()) {
      ((ObjectNode) system.get(system.size() - 1))
          .set(FIELD_CACHE_CONTROL, ephemeralCacheControl());
    }
  }

  /**
   * Marks the last tool definition as the cache boundary so that the entire
   * system-plus-tools prefix is cached as a single unit.
   */
  private void markLastToolForCaching(ObjectNode root) {
    var tools = root.get(FIELD_TOOLS);
    if (tools == null || !tools.isArray() || tools.isEmpty()) {
      return;
    }
    ((ObjectNode) tools.get(tools.size() - 1))
        .set(FIELD_CACHE_CONTROL, ephemeralCacheControl());
  }

  private ObjectNode ephemeralCacheControl() {
    var node = objectMapper.createObjectNode();
    node.put(FIELD_TYPE, CACHE_CONTROL_TYPE_EPHEMERAL);
    return node;
  }
}
