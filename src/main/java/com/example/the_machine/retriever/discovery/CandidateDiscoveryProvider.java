package com.example.the_machine.retriever.discovery;

/**
 * Interface for candidate discovery providers that can be used for finding candidate URLs.
 */
public interface CandidateDiscoveryProvider {

  /**
   * Returns the configured candidate discovery implementation.
   *
   * @return The candidate discovery implementation
   */
  CandidateDiscovery getCandidateDiscovery();
}