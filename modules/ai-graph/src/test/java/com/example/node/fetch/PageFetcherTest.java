package com.example.node.fetch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.infrastructure.config.HttpClientConfiguration;
import com.example.tool.fetch.PageCacheConfiguration;
import com.example.tool.fetch.PageFetcher;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test for PageFetcher to verify that the HttpClient and Cache beans are properly configured and
 * that robots.txt parsing works correctly.
 */
@SpringBootTest
@ContextConfiguration(classes = {
    HttpClientConfiguration.class,
    PageCacheConfiguration.class,
    PageFetcher.class
})
class PageFetcherTest {

  @Autowired
  private PageFetcher pageFetcher;

  @Test
  void contextLoads() {
    assertNotNull(pageFetcher);
  }

  @Test
  void testRobotsTxtParsingWithSpecificUserAgent() throws Exception {
    val robotsTxt = """
        User-agent: Googlebot
        Disallow: /
        
        User-agent: the_machine
        Allow: /
        """;

    assertTrue(invokeParseRobotsTxt(robotsTxt),
        "Should allow the_machine when explicitly allowed");
  }

  @Test
  void testRobotsTxtParsingWithSpecificUserAgentDisallowed() throws Exception {
    val robotsTxt = """
        User-agent: the_machine
        Disallow: /
        
        User-agent: *
        Allow: /
        """;

    val result = invokeParseRobotsTxt(robotsTxt);
    assertFalse(result, "Should disallow the_machine when specifically disallowed");
  }

  @Test
  void testRobotsTxtParsingWithWildcardDisallow() throws Exception {
    val robotsTxt = """
        User-agent: *
        Disallow: /
        """;

    val result = invokeParseRobotsTxt(robotsTxt);
    assertFalse(result, "Should disallow when wildcard disallows all");
  }

  @Test
  void testRobotsTxtParsingWithWildcardAllow() throws Exception {
    val robotsTxt = """
        User-agent: *
        Allow: /
        """;

    val result = invokeParseRobotsTxt(robotsTxt);
    assertTrue(result, "Should allow when wildcard allows all");
  }

  @Test
  void testRobotsTxtParsingWithMixedRules() throws Exception {
    val robotsTxt = """
        User-agent: Googlebot
        Disallow: /
        
        User-agent: *
        Disallow: /
        
        User-agent: the_machine
        Allow: /
        """;

    val result = invokeParseRobotsTxt(robotsTxt);
    assertTrue(result, "Should allow the_machine despite wildcard disallow");
  }

  @Test
  void testRobotsTxtParsingWithEmptyContent() throws Exception {
    val robotsTxt = "";

    val result = invokeParseRobotsTxt(robotsTxt);
    assertTrue(result, "Should allow when robots.txt is empty");
  }

  @Test
  void testRobotsTxtParsingWithNoRelevantRules() throws Exception {
    val robotsTxt = """
        User-agent: Googlebot
        Disallow: /private/
        
        User-agent: Bingbot
        Disallow: /admin/
        """;

    val result = invokeParseRobotsTxt(robotsTxt);
    assertTrue(result, "Should allow when no relevant rules found");
  }

  @Test
  void testRobotsTxtParsingWithCommentsAndWhitespace() throws Exception {
    val robotsTxt = """
        # This is a comment
        User-agent: the_machine
        # Another comment
        Disallow: /
        
        # Final comment
        """;

    val result = invokeParseRobotsTxt(robotsTxt);
    assertFalse(result, "Should handle comments and whitespace correctly");
  }

  /**
   * Helper method to invoke the private parseRobotsTxt method using reflection. Note: The actual
   */
  private boolean invokeParseRobotsTxt(String robotsTxt) throws Exception {
    // Get the RobotsTxtCache inner class
    Class<?>[] innerClasses = PageFetcher.class.getDeclaredClasses();
    Class<?> robotsTxtCacheClass = null;
    for (Class<?> innerClass : innerClasses) {
      if (innerClass.getSimpleName().equals("RobotsTxtCache")) {
        robotsTxtCacheClass = innerClass;
        break;
      }
    }

    if (robotsTxtCacheClass == null) {
      throw new RuntimeException("RobotsTxtCache inner class not found");
    }

    java.lang.reflect.Method parseMethod = robotsTxtCacheClass.getDeclaredMethod("parseRobotsTxt",
        String.class, String.class);
    parseMethod.setAccessible(true);

    // Invoke the method (it's static, so no instance needed)
    return (Boolean) parseMethod.invoke(null, robotsTxt, "the_machine");
  }
}