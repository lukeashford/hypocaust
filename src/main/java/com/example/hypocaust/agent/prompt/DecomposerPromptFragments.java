package com.example.hypocaust.agent.prompt;

import com.example.hypocaust.prompt.PromptFragment;

/**
 * Prompt fragments for the recursive Decomposer agent. Concise and abstract -- the decomposer
 * discovers tool names at runtime via semantic search.
 */
public final class DecomposerPromptFragments {

  private DecomposerPromptFragments() {
  }

  /**
   * Core decomposition algorithm. Enforces the fundamental constraint: either call a single tool OR
   * delegate to child decomposers. Never both.
   *
   * <p>Requires parameters: {{maxChildren}}, {{maxRetries}}
   */
  public static PromptFragment core() {
    return new PromptFragment() {
      @Override
      public String text() {
        return """
            You are a recursive task decomposition agent. Given a task:
            
            1. If the task can be solved by a single available tool, call it.
               Evaluate the result. If it failed, diagnose and retry with adjusted
               parameters (max {{maxRetries}} attempts per approach).
            
            2. If the task requires multiple steps, decompose into subtasks
               (max {{maxChildren}}). Delegate each to a child decomposer.
            
            Never mix these: either call one tool, or delegate to children.
            
            When done, return:
            {"success": true/false, "summary": "...", "artifactNames": [...], "errorMessage": "..."}""";
      }

      @Override
      public String id() {
        return "decomposer-core";
      }

      @Override
      public int priority() {
        return 10;
      }
    };
  }

  /**
   * Artifact awareness instructions. Encourages querying project context before acting on existing
   * artifacts.
   */
  public static PromptFragment artifactAwareness() {
    return new PromptFragment() {
      @Override
      public String text() {
        return """
            When the task involves existing artifacts or prior work, query the project context \
            to understand the current state before acting. Consider what has
            been tried before and whether to regenerate or edit.""";
      }

      @Override
      public String id() {
        return "decomposer-artifact-awareness";
      }

      @Override
      public int priority() {
        return 20;
      }
    };
  }

  /**
   * Self-healing protocol. Applies uniformly to tool calls AND child decomposers.
   *
   * <p>Requires parameter: {{maxRetries}}
   */
  public static PromptFragment selfHealing() {
    return new PromptFragment() {
      @Override
      public String text() {
        return """
            After any action, evaluate the result. If it failed:
            - Diagnose the cause.
            - Retry with a different approach or different parameters.
            - If one strategy is exhausted, try a fundamentally different one.
            - Max {{maxRetries}} retries per approach.
            - When giving up, return a clear diagnosis.""";
      }

      @Override
      public String id() {
        return "decomposer-self-healing";
      }

      @Override
      public int priority() {
        return 30;
      }
    };
  }
}
