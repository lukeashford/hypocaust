package com.example.hypocaust.prompt.fragments;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.prompt.PromptFragment;

/**
 * Central registry for all prompt fragments used across the system.
 */
public final class PromptFragments {

  private PromptFragments() {
  }

  // --- Common Fragments ---

  /**
   * Ability awareness and error handling fragment. Instructs the LLM to fail gracefully when it
   * cannot fulfill a request.
   */
  public static PromptFragment abilityAwareness() {
    return new PromptFragment(
        "common-ability-awareness",
        """
            1. If you cannot find the necessary tools or models to execute the task, do not hallucinate a solution.
            2. Trigger a failure by returning a result with 'success': false.
            3. Provide a clear, descriptive 'errorMessage' explaining exactly what is missing (e.g., "No tool found for video upscaling").
            4. This error message will be shown to the user/client to help them adjust their request.
            """,
        100
    );
  }

  /**
   * Core generation planning prompt fragment. Instructs the LLM how to map a user task to a model's
   * specific input schema.
   */
  public static PromptFragment planSystemPrompt() {
    return new PromptFragment(
        "common-plan-system-prompt",
        """
            You are an expert creative director and prompt engineer. Your goal is to prepare a generation plan \
            for a model based on a user's task.
            
            INPUTS PROVIDED:
            1. User Task: The natural language description of what to generate/edit.
            2. Model Docs: Contextual information about the selected model.
            
            OUTPUT:
            Return ONLY valid JSON.
            IMPORTANT: All string values MUST have newlines and special characters properly escaped (e.g., use \\n for newlines).
            
            {
              "providerInput": { ... },
              "errorMessage": null or "..."
            }
            """,
        0
    );
  }

  // --- Decomposer Fragments ---

  /**
   * Core decomposition algorithm. Enforces the fundamental constraint: either call a single tool OR
   * delegate to child decomposers. Never both.
   *
   * <p>Requires parameters: {{maxChildren}}, {{maxRetries}}
   */
  public static PromptFragment decomposerCore() {
    return new PromptFragment(
        "decomposer-core",
        """
            You are a recursive task decomposition agent and orchestrator. You work as a Lead Producer in a high-end production studio.
            
            CRITICAL: Do NOT perform creative tasks (writing, drawing, coding, etc.) yourself.
            Your role is to DECIDE which tool to use or how to break down the task.
            If the task requires generating or editing content, you MUST use a tool (like generate_creative) or delegate to sub-tasks.
            Never include the full content of an artifact in your summary.
            
            1. If the task can be solved by a single available tool, call it.
               Evaluate the result. If it failed, diagnose and retry with adjusted
               parameters (max {{maxRetries}} attempts per approach).
            
            2. If the task requires multiple steps, decompose into subtasks
               (max {{maxChildren}}). Delegate each to a child decomposer.
            
            Never mix these: either call one tool, or delegate to children.
            
            When done, return:
            {"success": true/false, "summary": "...", "artifactNames": [...], "errorMessage": "..."}""",
        10
    );
  }

  /**
   * Artifact awareness instructions. Encourages querying project context before acting on existing
   * artifacts.
   */
  public static PromptFragment artifactAwareness() {
    return new PromptFragment(
        "decomposer-artifact-awareness",
        """
            When the task involves existing artifacts or prior work, query the project context \
            to understand the current state before acting. Consider what has
            been tried before and whether to regenerate or edit.
            When a task involves changing, improving, or refining an existing artifact, prioritize tools and workflows that accept that artifact as a **foundational input** (edit/modify) rather than tools that generate a replacement from scratch (new).
            - Syntax: Explicitly reference existing artifacts using the `@artifact_name` syntax in tool parameters.
            - Intent: Formulate task descriptions to focus on the specific changes or "deltas" required, rather than re-describing the entire desired output.
            - Applicability: This applies to all tool types, including generative AI (e.g., image-to-video), deterministic engines (e.g., color grading), and utility tools (e.g., upscaling).
            """,
        20
    );
  }

  /**
   * Self-healing protocol. Applies uniformly to tool calls AND child decomposers.
   *
   * <p>Requires parameter: {{maxRetries}}
   */
  public static PromptFragment selfHealing() {
    return new PromptFragment(
        "decomposer-self-healing",
        """
            After any action, evaluate the result. If it failed:
            - Diagnose the cause.
            - Retry with a different approach or different parameters.
            - If one strategy is exhausted, try a fundamentally different one.
            - Max {{maxRetries}} retries per approach.
            - When giving up, return a clear diagnosis.""",
        30
    );
  }

  // --- Wording & Naming Fragments ---

  /**
   * Generates a brief progress label (1-5 words) for a task.
   */
  public static PromptFragment todoLabel() {
    return new PromptFragment(
        "wording-todo-label",
        """
            Generate a brief progress label (max 50 characters) for this task.
            Focus on the action (e.g., 'Creating poem', 'Adding characters').
            IMPORTANT: You are only labeling the task, NOT performing it.
            Output ONLY the label."""
    );
  }

  /**
   * Generates a brief commit message (1 sentence, max 100 chars) summarizing what was done.
   */
  public static PromptFragment commitMessage() {
    return new PromptFragment(
        "wording-commit-message",
        """
            Generate a brief commit message (1 sentence, max 100 chars) summarizing what was done.
            Focus on the outcome, not the process. Start with a verb like 'Added', 'Created', 'Updated'.
            Output ONLY the message."""
    );
  }

  /**
   * Generates a short snake_case name for a task execution (max 50 chars).
   */
  public static PromptFragment taskExecutionName() {
    return new PromptFragment(
        "wording-task-execution-name",
        """
            Generate a short snake_case name for a task execution (max 50 chars).
            The name should describe the intent of the task.
            Use only lowercase letters, numbers, and underscores.
            Reply with ONLY the name, nothing else.
            Examples: initial_character_designs, hair_color_change, forest_background_added"""
    );
  }

  /**
   * Generates a short snake_case name for an artifact (max 30 chars).
   */
  public static PromptFragment artifactName() {
    return new PromptFragment(
        "wording-artifact-name",
        """
            Generate a short snake_case name for an artifact (max 30 chars).
            Use only lowercase letters, numbers, and underscores.
            Reply with ONLY the name, nothing else.
            Examples: hero_portrait, forest_background, main_script"""
    );
  }

  /**
   * Generates a catchy title (max 60 chars) for a creative artifact.
   */
  public static PromptFragment artifactTitle() {
    return new PromptFragment(
        "wording-artifact-title",
        """
            The user will provide a prompt describing a creative generation task.
            Your job is to generate a catchy, human-friendly title (max 60 characters) for the artifact that will be produced by this task.
            
            IMPORTANT: You are NOT performing the task. You are only naming the expected outcome.
            Output ONLY the title, without quotes or explanation."""
    );
  }

  /**
   * Generates a brief description (1-2 sentences) for a creative artifact.
   */
  public static PromptFragment artifactDescription() {
    return new PromptFragment(
        "wording-artifact-description",
        """
            The user will provide a prompt describing a creative generation task.
            Your job is to generate a brief description (max 100 characters) for the artifact that will be produced by this task.
            Focus on the intended content, style, and subject matter.
            
            IMPORTANT: You are NOT performing the task. You are only describing the expected outcome.
            Output ONLY the description."""
    );
  }

  /**
   * Translates the user's task into a technical model requirement.
   */
  public static PromptFragment modelRequirement() {
    String kindsJson = ArtifactKind.toJsonArray();
    String body = (
        """
            Translate the user's task into a technical model requirement.
            
            Analyze the task and available artifacts to decide:
            1. 'inputs': Which artifact types are REQUIRED as source?
               - If the task involves a prompt (text instruction), ALWAYS include "TEXT" as input.
               - If editing an existing artifact (e.g., '@image'), include its kind (e.g., "IMAGE").
            2. 'output': The target ArtifactKind.
            3. 'tier': 'fast', 'balanced' (default), or 'powerful'.
            4. 'searchString': Keywords for semantic search (e.g., 'photorealistic portrait', 'background removal').
            
            Allowed ArtifactKinds (for both 'inputs' and 'output'):
            %s
            
            Output ONLY valid JSON:
            {
              "inputs": ["TEXT", "IMAGE"],
              "output": "IMAGE",
              "tier": "balanced",
              "searchString": "keywords"
            }
            """
    ).formatted(kindsJson);

    return new PromptFragment("wording-model-requirement", body);
  }
}
