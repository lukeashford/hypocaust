package com.example.the_machine.retriever;

import com.example.the_machine.search.GoogleCseEngineProvider;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Provider that supplies an {@link OptimizedContentRetriever} to LangChain4j.
 */
@Component
@Profile("optimized-retrieval")
public class OptimizedContentRetrieverProvider implements ContentRetrieverProvider {

  private final ContentRetriever contentRetriever;

  public OptimizedContentRetrieverProvider(GoogleCseEngineProvider searchEngineProvider) {
    this.contentRetriever = new OptimizedContentRetriever(
        searchEngineProvider.getWebSearchEngine());
  }

  @Override
  public ContentRetriever getContentRetriever() {
    return contentRetriever;
  }
}

/* ======================================================================================== */
/*  Actual retriever implementation                                                         */
/* ======================================================================================== */

@Slf4j
@RequiredArgsConstructor
class OptimizedContentRetriever implements ContentRetriever {

  /* ------------------------------------------------------------------ */
  /*  Constants                                                         */
  /* ------------------------------------------------------------------ */

  private static final int MAX_TOKENS_PER_CHUNK = 512;
  private static final int OVERLAP = 40;
  private static final int MAX_SEARCH_RESULTS = 30;
  private static final int TOP_K = 8;
  private static final int HTTP_CLIENT_TIMEOUT_SECONDS = 5;
  private static final int HTTP_REQUEST_TIMEOUT_SECONDS = 10;
  private static final int ROBOTS_TXT_TIMEOUT_SECONDS = 3;

  private final WebSearchEngine searchEngine;

  private final HttpClient http = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(HTTP_CLIENT_TIMEOUT_SECONDS))
      .build();

  /**
   * In‑memory cache for previously fetched pages. Replace with Redis/Caffeine for production.
   */
  private final Map<URI, Optional<Page>> cache = new ConcurrentHashMap<>();

  /* ------------------------------------------------------------------ */
  /*  Public API                                                        */
  /* ------------------------------------------------------------------ */

  @Override
  public List<Content> retrieve(Query query) {
    val q = query.text();
    val urls = searchUrls(q);
    log.debug("[Retriever] Found {} candidate URLs for query '{}'.", urls.size(), q);

    // Extract chunks from all pages
    val rawChunks = urls.parallelStream()
        .map(this::safeFetch)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(this::extractChunks)
        .map(content -> content.textSegment().text())
        .collect(Collectors.toList());

    // Rank chunks using Lucene BM25
    val ranked = new LuceneBm25Ranker(rawChunks, TOP_K).rank(q);

    // Convert back to Content objects
    return ranked.stream()
        .map(scoredChunk -> Content.from(
            TextSegment.from(scoredChunk.text()),
            Map.of(ContentMetadata.SCORE, Float.toString(scoredChunk.score()))))
        .collect(Collectors.toList());
  }

  /* ------------------------------------------------------------------ */
  /*  Internal steps                                                    */
  /* ------------------------------------------------------------------ */

  /**
   * 1. Candidate discovery via Google Custom Search API
   */
  private List<URI> searchUrls(String query) {
    val request = WebSearchRequest.builder()
        .searchTerms(query)
        .maxResults(MAX_SEARCH_RESULTS)
        .build();

    return searchEngine.search(request).results().stream()
        .map(WebSearchOrganicResult::url)
        .collect(Collectors.toList());
  }

  /**
   * 2. Safe HTTP fetch with simple robots.txt and timeout adherence.
   */
  private Optional<Page> safeFetch(URI url) {
    try {
      // honor robots.txt (very naive – production should cache robots files and use a proper parser)
      if (!RobotsTxtCache.isAllowed(url)) {
        log.debug("[Retriever] Skipping disallowed URL {}", url);
        return Optional.empty();
      }

      return cache.computeIfAbsent(url, this::downloadAndExtract);
    } catch (Exception e) {
      log.warn("[Retriever] Failed fetching {} – {}", url, e.toString());
      return Optional.empty();
    }
  }

  private Optional<Page> downloadAndExtract(URI url) {
    try {
      val req = HttpRequest.newBuilder(url)
          .header("User-Agent", "OptimizedContentRetriever/1.0 (+mailto:info@lukeashford.com)")
          .timeout(Duration.ofSeconds(HTTP_REQUEST_TIMEOUT_SECONDS))
          .GET()
          .build();
      val resp = http.send(req, HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() >= 400) {
        return Optional.empty();
      }
      val html = resp.body();
      val readable = Readability.extract(html); // see helper below
      if (readable.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(new Page(url, readable));
    } catch (Exception ex) {
      log.debug("[Retriever] Exception during download {} – {}", url, ex.toString());
      return Optional.empty();
    }
  }

  /**
   * 3. Clean text → token chunks → Content objects.
   */
  private Stream<Content> extractChunks(Page page) {
    val splitter = DocumentSplitters.recursive(MAX_TOKENS_PER_CHUNK, OVERLAP);

    val doc = Document.document(page.body());

    return splitter.split(doc).stream().map(chunk -> Content.from(chunk.text()));
  }

  /* ------------------------------------------------------------------ */
  /*  Helpers & nested classes                                          */
  /* ------------------------------------------------------------------ */

  /**
   * Minimal representation of a fetched page.
   */
  private record Page(URI url, String body) {

  }

  /**
   * Simple record to hold a chunk of text and its BM25 score.
   */
  private record ScoredChunk(String text, float score) {

  }

  /**
   * Lucene-based BM25 ranker for text chunks.
   */
  private static class LuceneBm25Ranker {

    private final List<String> chunks;
    private final int topK;
    private final Directory directory;
    private final StandardAnalyzer analyzer;

    LuceneBm25Ranker(List<String> chunks, int topK) {
      this.chunks = chunks;
      this.topK = topK;
      this.directory = new ByteBuffersDirectory();
      this.analyzer = new StandardAnalyzer();

      // Index the chunks
      try {
        val config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new BM25Similarity());
        val writer = new IndexWriter(directory, config);

        for (int i = 0; i < chunks.size(); i++) {
          val doc = new org.apache.lucene.document.Document();
          doc.add(new TextField("body", chunks.get(i), Field.Store.YES));
          doc.add(new TextField("id", String.valueOf(i), Field.Store.YES));
          writer.addDocument(doc);
        }
        writer.close();
      } catch (IOException e) {
        throw new RuntimeException("Failed to index chunks", e);
      }
    }

    List<ScoredChunk> rank(String queryText) {
      try {
        val reader = DirectoryReader.open(directory);
        val searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new BM25Similarity());

        val parser = new QueryParser("body", analyzer);
        val query = parser.parse(queryText);

        val results = searcher.search(query, topK);
        List<ScoredChunk> scoredChunks = new ArrayList<>();

        for (int i = 0; i < results.scoreDocs.length; i++) {
          val doc = searcher.storedFields().document(results.scoreDocs[i].doc);
          val chunkId = Integer.parseInt(doc.get("id"));
          val score = results.scoreDocs[i].score;
          scoredChunks.add(new ScoredChunk(chunks.get(chunkId), score));
        }

        reader.close();
        return scoredChunks;
      } catch (IOException | ParseException e) {
        throw new RuntimeException("Failed to search chunks", e);
      }
    }
  }

  /* ---------------------------- robots.txt --------------------------- */

  private static class RobotsTxtCache {

    private static final Map<String, Boolean> allowed = new ConcurrentHashMap<>();
    private static final HttpClient http = HttpClient.newHttpClient();

    static boolean isAllowed(URI url) {
      try {
        String host = url.getHost().toLowerCase();
        return allowed.computeIfAbsent(host, RobotsTxtCache::fetchRules);
      } catch (Exception e) {
        return true; // be permissive on failure
      }
    }

    private static boolean fetchRules(String host) {
      try {
        URI robots = URI.create("https://" + host + "/robots.txt");
        HttpRequest req = HttpRequest.newBuilder(robots)
            .timeout(Duration.ofSeconds(ROBOTS_TXT_TIMEOUT_SECONDS))
            .GET()
            .header("User-Agent", "OptimizedContentRetriever/1.0")
            .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
          return true; // treat missing robots as allowed
        }
        return !resp.body().toLowerCase().contains("disallow: /");
      } catch (Exception ignore) {
        return true; // network errors → allow
      }
    }
  }

  /* ---------------------------- Readability ------------------------- */

  /**
   * Minimal readability wrapper. Replace with a robust library for production.
   */
  private static class Readability {

    static String extract(String html) {
      // Placeholder – replace with real library call
      return JsoupExtractors.text(html);
    }
  }

  /* ---------------------------- Jsoup fallback ---------------------- */

  private static class JsoupExtractors {

    static String text(String html) {
      return org.jsoup.Jsoup.parse(html).text();
    }
  }
}
