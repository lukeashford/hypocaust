package com.example.graph;

import com.example.tool.extract.PageContent;
import com.example.tool.fetch.PageFetcher.Page;
import com.example.tool.rank.ChunkRanker.ScoredChunk;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;

public class RetrievalState extends AgentState {

  public static final String BRAND_NAME = "brandName";
  public static final String CANDIDATE_URLS_KEY = "candidateUrls";
  public static final String PAGES_KEY = "pages";
  public static final String PAGE_CONTENTS_KEY = "pageContents";
  public static final String CHUNKS_KEY = "chunks";
  public static final String RANKED_CHUNKS_KEY = "rankedChunks";
  public static final String FILTERED_CHUNKS_KEY = "filteredChunks";
  public static final String ANALYSIS_KEY = "analysis";

  public static final Map<String, Channel<?>> SCHEMA = Map.of(
      BRAND_NAME, ChannelsSugar.overwrite(() -> ""),
      CANDIDATE_URLS_KEY, ChannelsSugar.overwrite(ArrayList::new),
      PAGES_KEY, ChannelsSugar.overwrite(ArrayList::new),
      PAGE_CONTENTS_KEY, ChannelsSugar.overwrite(ArrayList::new),
      CHUNKS_KEY, ChannelsSugar.overwrite(ArrayList::new),
      RANKED_CHUNKS_KEY, ChannelsSugar.overwrite(ArrayList::new),
      FILTERED_CHUNKS_KEY, ChannelsSugar.overwrite(ArrayList::new),
      ANALYSIS_KEY, ChannelsSugar.overwrite(() -> "")
  );

  public RetrievalState(Map<String, Object> data) {
    super(data);
  }

  public String getBrandName() {
    return this.<String>value(BRAND_NAME).orElse("");
  }

  public List<URI> getCandidateUrls() {
    return this.<List<URI>>value(CANDIDATE_URLS_KEY).orElse(List.of());
  }

  public List<Page> getPages() {
    return this.<List<Page>>value(PAGES_KEY).orElse(List.of());
  }

  public List<PageContent> getPageContents() {
    return this.<List<PageContent>>value(PAGE_CONTENTS_KEY).orElse(List.of());
  }

  public List<String> getChunks() {
    return this.<List<String>>value(CHUNKS_KEY).orElse(List.of());
  }

  public List<ScoredChunk> getRankedChunks() {
    return this.<List<ScoredChunk>>value(RANKED_CHUNKS_KEY).orElse(List.of());
  }

  public List<ScoredChunk> getFilteredChunks() {
    return this.<List<ScoredChunk>>value(FILTERED_CHUNKS_KEY).orElse(List.of());
  }
}
