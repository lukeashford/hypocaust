package com.example.tool.filter;

import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

/**
 * Classifies whether page content is brand-relevant using heuristic rules.
 */
@Component
@Slf4j
public class BrandClassifier {

  private static final Set<String> COMPANY_DOMAINS = Set.of(
      ".com", ".org", ".net", ".co", ".io", ".ai", ".inc"
  );

  private static final Pattern BRAND_KEYWORDS = Pattern.compile(
      "\\b(?:mission|vision|values|brand|company|about\\s+us|our\\s+story|who\\s+we\\s+are|corporate|culture|philosophy|purpose|commitment)\\b",
      Pattern.CASE_INSENSITIVE
  );

  private static final Pattern CORPORATE_INDICATORS = Pattern.compile(
      "\\b(?:founded|established|headquarters|ceo|leadership|team|employees|staff|organization|enterprise|corporation|business)\\b",
      Pattern.CASE_INSENSITIVE
  );

  /**
   * Determines if the given page text and URL are brand-relevant.
   *
   * @param pageText the text content of the page
   * @param url the URL of the page
   * @return true if the content appears to be brand-relevant
   */
  public boolean isBrandRelevant(String pageText, URI url) {
    if (pageText == null || pageText.isBlank()) {
      log.debug("[BrandClassifier] Empty page text, not brand relevant");
      return false;
    }

    val urlScore = calculateUrlScore(url);
    val contentScore = calculateContentScore(pageText);
    val totalScore = urlScore + contentScore;

    val isRelevant = totalScore >= 0.0;
    log.debug(
        "[BrandClassifier] URL: {}, URL score: {}, Content score: {}, Total: {}, Relevant: {}",
        url, urlScore, contentScore, totalScore, isRelevant);

    return isRelevant;
  }

  private double calculateUrlScore(URI url) {
    if (url == null) {
      return 0.0;
    }

    double score = 0.0;
    val path = url.getPath() != null ? url.getPath().toLowerCase() : "";

    // Check if it's a company domain
    if (isCompanyDomain(url)) {
      score += 1.0;
    }

    // Check for brand-related paths
    if (path.contains("about") || path.contains("company") ||
        path.contains("mission") || path.contains("vision") ||
        path.contains("values") || path.contains("culture")) {
      score += 2.0;
    }

    // Check for corporate pages
    if (path.contains("leadership") || path.contains("team") ||
        path.contains("management") || path.contains("executives")) {
      score += 1.5;
    }

    // Penalize certain non-brand paths
    if (path.contains("blog") || path.contains("news") ||
        path.contains("product") || path.contains("support") ||
        path.contains("help") || path.contains("faq")) {
      score -= 0.5;
    }

    return Math.max(0.0, score);
  }

  private boolean isCompanyDomain(URI url) {
    if (url.getHost() == null) {
      return false;
    }

    val host = url.getHost().toLowerCase();

    // Check for common company domain patterns
    return COMPANY_DOMAINS.stream().anyMatch(host::endsWith) &&
        !host.contains("blog.") &&
        !host.contains("support.") &&
        !host.contains("help.") &&
        !host.contains("docs.") &&
        !host.contains("api.");
  }

  private double calculateContentScore(String pageText) {
    double score = 0.0;
    val lowerText = pageText.toLowerCase();

    // Count brand keyword matches
    val brandMatches = BRAND_KEYWORDS.matcher(lowerText);
    int brandKeywordCount = 0;
    while (brandMatches.find()) {
      brandKeywordCount++;
    }
    score += Math.min(brandKeywordCount * 0.5, 3.0); // Cap at 3.0

    // Count corporate indicator matches
    val corporateMatches = CORPORATE_INDICATORS.matcher(lowerText);
    int corporateCount = 0;
    while (corporateMatches.find()) {
      corporateCount++;
    }
    score += Math.min(corporateCount * 0.3, 2.0); // Cap at 2.0

    // Check for specific brand-related phrases
    if (lowerText.contains("our mission") || lowerText.contains("our vision")) {
      score += 1.5;
    }
    if (lowerText.contains("our values") || lowerText.contains("core values")) {
      score += 1.5;
    }
    if (lowerText.contains("about us") || lowerText.contains("who we are")) {
      score += 1.0;
    }
    if (lowerText.contains("our story") || lowerText.contains("our history")) {
      score += 1.0;
    }

    // Penalize content that seems non-brand related
    if (lowerText.contains("add to cart") || lowerText.contains("buy now") ||
        lowerText.contains("price") || lowerText.contains("shipping")) {
      score -= 1.0;
    }
    if (lowerText.contains("error") || lowerText.contains("404") ||
        lowerText.contains("not found")) {
      score -= 2.0;
    }

    return Math.max(0.0, score);
  }
}