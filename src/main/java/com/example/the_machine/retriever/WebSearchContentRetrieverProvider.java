package com.example.the_machine.retriever;

import com.example.the_machine.search.WebSearchEngineProvider;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Web Search Content Retriever Provider implementation.
 */
@Component
@Profile("web-search")
public class WebSearchContentRetrieverProvider implements ContentRetrieverProvider {

  private final ContentRetriever contentRetriever;

  public WebSearchContentRetrieverProvider(WebSearchEngineProvider webSearchEngineProvider) {
    this.contentRetriever = WebSearchContentRetriever.builder()
        .webSearchEngine(webSearchEngineProvider.getWebSearchEngine())
        .maxResults(30)
        .build();
  }

  @Override
  public ContentRetriever getContentRetriever() {
    return contentRetriever;
  }
}