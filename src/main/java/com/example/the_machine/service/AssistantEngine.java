package com.example.the_machine.service;

/**
 * Interface for assistant execution engines that handle different types of run workflows.
 */
public interface AssistantEngine {

  /**
   * Executes planning phase and possibly asks for clarification. Emits PLAN event and possibly
   * assistant question (REQUIRES_ACTION status).
   *
   * @param ctx the run context containing thread, run, repos, mappers, and event publisher
   */
  void executePlanAskClarify(RunContext ctx);

  /**
   * Executes the full pipeline from analysis to completion. Emits ANALYSIS, SCRIPT, IMAGES..., and
   * completes the run.
   *
   * @param ctx the run context containing thread, run, repos, mappers, and event publisher
   */
  void executeFullPipeline(RunContext ctx);

  /**
   * Executes partial revision with selective superseding. Emits selective supersedes and DECK
   * artifacts.
   *
   * @param ctx the run context containing thread, run, repos, mappers, and event publisher
   * @param reason the reason for the partial revision
   */
  void executePartialRevision(RunContext ctx, String reason);
}