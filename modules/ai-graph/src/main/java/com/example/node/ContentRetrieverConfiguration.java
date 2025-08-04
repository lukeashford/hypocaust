package com.example.node;

import com.example.tool.discovery.CandidateDiscovery;
import com.example.tool.extract.ContentExtractor;
import com.example.tool.fetch.PageFetcher;
import com.example.tool.filter.BrandClassifier;
import com.example.tool.rank.ChunkRanker;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.web.search.WebSearchEngine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for content retrievers.
 */
@Configuration
public class ContentRetrieverConfiguration {

  /**
   * Web Search Content Retriever bean.
   */
  @Bean
  @ConditionalOnProperty(name = "app.content-retriever", havingValue = "web-search")
  public ContentRetriever webSearchContentRetriever(WebSearchEngine webSearchEngine) {
    return new WebSearchContentRetriever(webSearchEngine);
  }

  /**
   * Optimized Content Retriever bean.
   */
  @Bean
  @ConditionalOnProperty(name = "app.content-retriever", havingValue = "optimized")
  public ContentRetriever optimizedContentRetriever(
      CandidateDiscovery candidateDiscovery,
      PageFetcher pageFetcher,
      ContentExtractor contentExtractor,
      ChunkRanker chunkRanker,
      BrandClassifier brandClassifier) {
    return new OptimizedContentRetriever(
        candidateDiscovery,
        pageFetcher,
        contentExtractor,
        chunkRanker,
        brandClassifier).getContentRetriever();
  }
}