package com.example.tool.rank;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ranks text chunks using hybrid BM25 + cosine similarity scoring.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChunkRanker {

  @Value("${app.topK}")
  private int topK;

  private final EmbeddingModel embeddingModel;

  /**
   * Represents a scored text chunk.
   */
  public record ScoredChunk(String text, float score, int originalIndex) implements Serializable {

  }

  /**
   * Rank chunks using hybrid scoring: BM25 * 0.5 + cosine similarity * 0.5.
   *
   * @param query the search query
   * @param chunks the text chunks to rank
   * @return list of top-K scored chunks
   */
  public List<ScoredChunk> rank(String query, List<String> chunks) {
    if (chunks.isEmpty()) {
      return List.of();
    }

    val bm25Scores = computeBM25Scores(query, chunks);
    val cosineScores = computeCosineScores(query, chunks);
    val hybridScores = combineScores(bm25Scores, cosineScores);

    return selectTopK(hybridScores);
  }

  private List<ScoredChunk> computeBM25Scores(String query, List<String> chunks) {
    try (
        val directory = new ByteBuffersDirectory();
        val analyzer = new StandardAnalyzer()
    ) {

      // Index chunks
      val config = new IndexWriterConfig(analyzer);
      config.setSimilarity(new BM25Similarity());

      try (val writer = new IndexWriter(directory, config)) {
        for (int i = 0; i < chunks.size(); i++) {
          val doc = new Document();
          doc.add(new TextField("body", chunks.get(i), Field.Store.YES));
          doc.add(new TextField("id", String.valueOf(i), Field.Store.YES));
          writer.addDocument(doc);
        }
      }

      // Search
      try (val reader = DirectoryReader.open(directory)) {
        val searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new BM25Similarity());

        val parser = new QueryParser("body", analyzer);
        val luceneQuery = parser.parse(query);

        val results = searcher.search(luceneQuery, Math.min(topK * 2, chunks.size()));
        List<ScoredChunk> scoredChunks = new ArrayList<>();

        for (val scoreDoc : results.scoreDocs) {
          val doc = searcher.storedFields().document(scoreDoc.doc);
          val chunkId = Integer.parseInt(doc.get("id"));
          val score = scoreDoc.score;
          scoredChunks.add(new ScoredChunk(chunks.get(chunkId), score, chunkId));
        }

        return scoredChunks;
      }
    } catch (IOException | ParseException e) {
      log.error("[ChunkRanker] BM25 scoring failed: {}", e.getMessage());
      // Return all chunks with zero scores as fallback
      return IntStream.range(0, chunks.size())
          .mapToObj(i -> new ScoredChunk(chunks.get(i), 0.0f, i))
          .toList();
    }
  }

  private List<ScoredChunk> computeCosineScores(String query, List<String> chunks) {
    try {
      val queryEmbedding = embeddingModel.embed(query).content();
      val textSegments = chunks.stream().map(TextSegment::from).toList();
      val chunkEmbeddings = embeddingModel.embedAll(textSegments).content();

      List<ScoredChunk> scoredChunks = new ArrayList<>();
      for (int i = 0; i < chunks.size(); i++) {
        val similarity = cosineSimilarity(queryEmbedding, chunkEmbeddings.get(i));
        scoredChunks.add(new ScoredChunk(chunks.get(i), (float) similarity, i));
      }

      return scoredChunks;
    } catch (Exception e) {
      log.error("[ChunkRanker] Cosine similarity scoring failed: {}", e.getMessage());
      // Return all chunks with zero scores as fallback
      return IntStream.range(0, chunks.size())
          .mapToObj(i -> new ScoredChunk(chunks.get(i), 0.0f, i))
          .toList();
    }
  }

  private double cosineSimilarity(Embedding a, Embedding b) {
    val vectorA = a.vector();
    val vectorB = b.vector();

    if (vectorA.length != vectorB.length) {
      throw new IllegalArgumentException("Vectors must have the same dimension");
    }

    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    for (int i = 0; i < vectorA.length; i++) {
      dotProduct += vectorA[i] * vectorB[i];
      normA += vectorA[i] * vectorA[i];
      normB += vectorB[i] * vectorB[i];
    }

    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }

  private List<ScoredChunk> combineScores(List<ScoredChunk> bm25Scores,
      List<ScoredChunk> cosineScores) {
    // Normalize scores to [0, 1] range
    val maxBm25 = bm25Scores.stream().mapToDouble(ScoredChunk::score).max().orElse(1.0);
    val maxCosine = cosineScores.stream().mapToDouble(ScoredChunk::score).max().orElse(1.0);

    List<ScoredChunk> combined = new ArrayList<>();
    for (final ScoredChunk bm25Chunk : bm25Scores) {
      val cosineChunk = cosineScores.stream()
          .filter(c -> c.originalIndex() == bm25Chunk.originalIndex())
          .findFirst()
          .orElse(new ScoredChunk(bm25Chunk.text(), 0.0f, bm25Chunk.originalIndex()));

      val normalizedBm25 = bm25Chunk.score() / maxBm25;
      val normalizedCosine = cosineChunk.score() / maxCosine;
      val hybridScore = (float) (normalizedBm25 * 0.5 + normalizedCosine * 0.5);

      combined.add(new ScoredChunk(bm25Chunk.text(), hybridScore, bm25Chunk.originalIndex()));
    }

    return combined;
  }

  private List<ScoredChunk> selectTopK(List<ScoredChunk> scoredChunks) {
    return scoredChunks.stream()
        .sorted((a, b) -> Float.compare(b.score(), a.score()))
        .limit(topK)
        .toList();
  }
}