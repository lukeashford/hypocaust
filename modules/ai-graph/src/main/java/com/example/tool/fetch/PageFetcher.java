package com.example.tool.fetch;

import com.github.benmanes.caffeine.cache.Cache;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Handles fetching web pages with caching and robots.txt compliance.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PageFetcher {

  private static final int HTTP_REQUEST_TIMEOUT_SECONDS = 10;
  private static final int ROBOTS_TXT_TIMEOUT_SECONDS = 3;

  private final HttpClient httpClient;
  private final Cache<URI, Page> cache;

  @Value("${spring.application.name}")
  private String applicationName;

  /**
   * Represents a fetched web page.
   */
  public record Page(URI url, String body) implements Serializable {

  }

  /**
   * Fetch a page from the given URI, respecting robots.txt and using cache.
   *
   * @param url the URL to fetch
   * @return the fetched page, or empty if failed or disallowed
   */
  public Optional<Page> fetch(URI url) {
    try {
      // Check robots.txt
      if (!RobotsTxtCache.isAllowed(url, httpClient, applicationName)) {
        log.debug("[PageFetcher] Skipping disallowed URL {}", url);
        return Optional.empty();
      }

      // Check cache first
      val cachedPage = cache.getIfPresent(url);
      if (cachedPage != null) {
        log.debug("[PageFetcher] Cache hit for URL {}", url);
        return Optional.of(cachedPage);
      }

      // Fetch and cache
      val page = downloadPage(url);
      page.ifPresent(p -> cache.put(url, p));
      return page;

    } catch (Exception e) {
      log.warn("[PageFetcher] Failed fetching {} – {}", url, e.toString());
      return Optional.empty();
    }
  }

  private Optional<Page> downloadPage(URI url) {
    try {
      val request = HttpRequest.newBuilder(url)
          .header("User-Agent", applicationName + " (+mailto:info@lukeashford.com)")
          .timeout(Duration.ofSeconds(HTTP_REQUEST_TIMEOUT_SECONDS))
          .GET()
          .build();

      val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() >= 400) {
        log.debug("[PageFetcher] HTTP error {} for URL {}", response.statusCode(), url);
        return Optional.empty();
      }

      val html = response.body();
      if (html == null || html.isBlank()) {
        log.debug("[PageFetcher] Empty response for URL {}", url);
        return Optional.empty();
      }

      log.debug("[PageFetcher] Successfully fetched URL {}", url);
      return Optional.of(new Page(url, html));

    } catch (Exception ex) {
      log.debug("[PageFetcher] Exception during download {} – {}", url, ex.toString());
      return Optional.empty();
    }
  }

  /**
   * Simple robots.txt cache implementation.
   */
  private static class RobotsTxtCache {

    private static final Map<String, Boolean> allowed = new ConcurrentHashMap<>();

    static boolean isAllowed(URI url, HttpClient httpClient, String applicationName) {
      try {
        val host = url.getHost().toLowerCase();
        return allowed.computeIfAbsent(host, h -> fetchRules(h, httpClient, applicationName));
      } catch (Exception e) {
        return true; // be permissive on failure
      }
    }

    private static boolean fetchRules(String host, HttpClient httpClient, String applicationName) {
      try {
        val robots = URI.create("https://" + host + "/robots.txt");
        val req = HttpRequest.newBuilder(robots)
            .timeout(Duration.ofSeconds(ROBOTS_TXT_TIMEOUT_SECONDS))
            .GET()
            .header("User-Agent", applicationName)
            .build();

        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400) {
          return true; // treat missing robots as allowed
        }

        // Parse robots.txt properly for our specific user agent
        return parseRobotsTxt(resp.body(), applicationName);

      } catch (Exception ignore) {
        return true; // network errors → allow
      }
    }

    /**
     * Parse robots.txt content to check if the given user agent is allowed to access root path.
     *
     * @param robotsTxt the robots.txt content
     * @return true if allowed, false if disallowed
     */
    private static boolean parseRobotsTxt(String robotsTxt, String applicationName) {
      if (robotsTxt == null || robotsTxt.isBlank()) {
        return true; // empty robots.txt means allow all
      }

      String[] lines = robotsTxt.toLowerCase().split("\n");
      boolean inRelevantSection = false;
      boolean foundSpecificAgent = false;
      boolean isAllowed = true; // default to allowed

      for (String line : lines) {
        line = line.trim();

        // Skip empty lines and comments
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        if (line.startsWith("user-agent:")) {
          String agent = line.substring("user-agent:".length()).trim();

          // Check if this section applies to our user agent
          // Only use wildcard rules if we haven't found specific rules
          if (agent.equals(applicationName.toLowerCase())) {
            inRelevantSection = true;
            foundSpecificAgent = true;
          } else {
            inRelevantSection = agent.equals("*") && !foundSpecificAgent;
          }
        } else if (inRelevantSection && line.startsWith("disallow:")) {
          String path = line.substring("disallow:".length()).trim();

          // Check if root path is disallowed
          if (path.equals("/")) {
            isAllowed = false;
            // If we found a specific rule for our agent, we can stop here
            if (foundSpecificAgent) {
              break;
            }
          }
        } else if (inRelevantSection && line.startsWith("allow:")) {
          String path = line.substring("allow:".length()).trim();

          // Check if root path is explicitly allowed
          if (path.equals("/")) {
            isAllowed = true;
            // If we found a specific rule for our agent, we can stop here
            if (foundSpecificAgent) {
              break;
            }
          }
        }
      }

      return isAllowed;
    }
  }
}