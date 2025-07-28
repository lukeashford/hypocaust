package com.example.the_machine.retriever;

import com.example.the_machine.retriever.discovery.CandidateDiscovery;
import com.example.the_machine.retriever.extract.ContentExtractor;
import com.example.the_machine.retriever.fetch.PageFetcher;
import com.example.the_machine.retriever.filter.BrandClassifier;
import com.example.the_machine.retriever.rank.ChunkRanker;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Optimized Content Retriever implementation.
 */
@Slf4j
@RequiredArgsConstructor
public class OptimizedContentRetriever implements ContentRetriever {

  private static final int MAX_TOKENS_PER_CHUNK = 512;
  private static final int OVERLAP = 40;
  private static final int MAX_SEARCH_RESULTS = 30;

  private final CandidateDiscovery candidateDiscovery;
  private final PageFetcher pageFetcher;
  private final ContentExtractor contentExtractor;
  private final ChunkRanker chunkRanker;
  private final BrandClassifier brandClassifier;

  @Override
  public List<Content> retrieve(Query query) {
    val q = query.text();
    log.debug("[Retriever] Starting orchestrated retrieval for query '{}'", q);

    // 1) Candidate discovery via search engine
    val urls = candidateDiscovery.find(q, MAX_SEARCH_RESULTS);
    log.debug("[Retriever] Found {} candidate URLs", urls.size());

    // 2) Fetch pages with caching and robots.txt compliance
    val pages = urls.parallelStream()
        .map(pageFetcher::fetch)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
    log.debug("[Retriever] Successfully fetched {} pages", pages.size());

    // 3) Extract content from pages
    val allChunks = pages.stream()
        .map(page -> contentExtractor.extract(page.body()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(pageContent -> splitIntoChunks(pageContent.text()).stream())
        .collect(Collectors.toList());
    log.debug("[Retriever] Extracted {} text chunks", allChunks.size());

    // 4) Rank chunks using hybrid BM25 + cosine similarity
    val rankedChunks = chunkRanker.rank(q, allChunks);
    log.debug("[Retriever] Ranked chunks, top score: {}",
        rankedChunks.isEmpty() ? "N/A" : rankedChunks.get(0).score());

    // 5) Filter for brand-relevant content
    val brandRelevantChunks = rankedChunks.stream()
        .filter(chunk -> {
          // Find the original page URL for this chunk (simplified approach)
          val firstPageUrl = pages.isEmpty() ? null : pages.get(0).url();
          return brandClassifier.isBrandRelevant(chunk.text(), firstPageUrl);
        })
        .toList();
    log.debug("[Retriever] {} chunks passed brand relevance filter", brandRelevantChunks.size());

    // 6) Convert to Content objects with proper citations
    val contentList = new java.util.ArrayList<Content>();
    for (int i = 0; i < rankedChunks.size(); i++) {
      val chunk = brandRelevantChunks.get(i);
      val content = Content.from(
          TextSegment.from(formatChunkWithCitation(chunk, i + 1)),
          Map.of(ContentMetadata.SCORE, chunk.score())
      );
      contentList.add(content);
    }

    log.debug("[Retriever] Returning {} content items", contentList.size());
    return contentList;
  }

  private String formatChunkWithCitation(ChunkRanker.ScoredChunk chunk, int citationNumber) {
    return String.format("--- Chunk [%d] (Score: %.3f) ---\n%s",
        citationNumber, chunk.score(), chunk.text());
  }

  private List<String> splitIntoChunks(String text) {
    val splitter = DocumentSplitters.recursive(MAX_TOKENS_PER_CHUNK, OVERLAP);
    val doc = Document.document(text);
    return splitter.split(doc).stream()
        .map(segment -> segment.text())
        .collect(Collectors.toList());
  }

  public ContentRetriever getContentRetriever() {
    return this;
  }
}