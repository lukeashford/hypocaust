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
            
            YOUR SCOPE — what you control:
            - WHAT to create (subject, requirements, constraints from the user's request)
            - WHEN and in what ORDER tasks should happen
            - WHETHER to retry or give up after failures
            - Artifact naming: you choose a `preferredName` (snake_case identifier) and `preferredTitle` \
              (human-readable) for each new artifact, plus a concise `description`

            NOT YOUR SCOPE — handled automatically by tools:
            - Model/provider selection (handled by RAG-based model matching)
            - Artistic style decisions beyond what the user specified (leave creative latitude to the generative models)

            When writing task descriptions for tools, faithfully convey the user's intent and
            constraints. Do not add your own artistic preferences beyond what the user specified.
            
            1. If the task can be solved by a single available tool, call it directly.
               Evaluate the result. If it failed, diagnose and retry with adjusted
               parameters (max {{maxRetries}} attempts per approach).
            
            2. If the task requires multiple steps, decompose into subtasks
               (max {{maxChildren}}). Declare your plan with `set_plan`, then delegate
               each step to a child decomposer via `invoke_decomposer`.
            
            Never mix these: either call one tool, or delegate to children.
            
            ENFORCEMENT: If your plan (set_plan) declares more than 1 step, `execute_tool`
            will be BLOCKED and return a DELEGATION_REQUIRED error. You MUST use
            `invoke_decomposer` for each step instead. Do not work around this by reducing
            your plan to 1 step — declare your honest, complete plan first, then delegate.
            
            When done, return:
            {"success": true/false, "summary": "...", "artifactNames": [...], "errorMessage": "..."}""",
        10
    );
  }

  /**
   * Planning instructions. Instructs the decomposer on how to use the `set_plan` tool.
   */
  public static PromptFragment planning() {
    return new PromptFragment(
        "decomposer-planning",
        """
            Before executing a multi-step task, you SHOULD declare your plan using the `set_plan` tool.
            This makes your intentions visible and helps track progress.
            
            1. Call `set_plan` with a list of descriptive sub-steps.
            2. When delegating via `invoke_decomposer`, use the exact same `todoDescription` you used in the plan.
            3. If the plan changes (e.g., a step fails and you need an alternative), call `set_plan` again with the updated list.
            """,
        15
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
            ## Existing artifacts

            The user message includes a list of existing artifacts (if any) in the format:
            `[KIND, name] Title - Description`

            Use this list to understand what already exists. You do NOT need to call \
            `ask_project_context` just to know what artifacts are present — that information \
            is already in front of you. Reserve `ask_project_context` for deeper queries: \
            summarizing artifact content, inspecting metadata or generation details, querying \
            commit history, or understanding what was tried before.

            ## Artifact naming

            Choose names and titles that are unique relative to the existing artifact list. \
            Collisions are resolved automatically by appending a counter \
            (e.g., "cat_astronaut_2"), but aim for uniqueness yourself.

            ## Referencing existing artifacts

            When the task mentions named entities, distinctive characteristics, or recurring \
            resources — check the existing artifact list for a match.

            Media artifacts (images, videos, audio) are resolved to URLs and can be passed \
            to tools via the `@name` syntax as foundational inputs for direct manipulation \
            (e.g., image-to-video, upscaling, style transfer).

            Text artifacts are sources of information, not direct inputs. Only you have access \
            to artifact contents via the project context tool — downstream tools cannot read them. \
            You MUST query relevant text artifacts yourself and include the extracted details \
            directly in your task descriptions.

            When refining an existing artifact, prefer edit/modify workflows over generating \
            a replacement from scratch. Reference the artifact with `@name` and describe \
            the specific changes needed, not the entire desired output.
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
            After any action, evaluate the result. If the tool or child decomposer returned an error,
            classify it and respond accordingly:
            
            1. INFRASTRUCTURE / PROVIDER FAILURE — The error describes a technical issue
               with the underlying service (timeouts, network errors, API failures, service
               unavailable). The tool has already retried internally.
               → Do NOT retry this capability. Report the issue and move on.
            
            2. INPUT / PARAMETER ISSUE — The error describes a problem with YOUR request
               (missing parameters, invalid format, wrong artifact reference).
               → Adjust your parameters and retry (max {{maxRetries}} attempts per approach).
            
            3. CAPACITY / SCOPE MISMATCH — The error indicates your request exceeds what
               the tool can handle in a single call (too many outputs, unsupported
               combination), but the capability itself works.
               → Restructure your approach. Break the task into smaller units and retry
                 each individually.
            
            If a child decomposer failed, trust its diagnosis. Do not re-attempt the same
            generation that the child already exhausted. When giving up, include error
            details in your response.""",
        30
    );
  }

  // --- Wording & Naming Fragments ---

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
            2. 'tier': 'fast', 'balanced' (default), or 'powerful'.
            3. 'searchString': Keywords for semantic search (e.g., 'photorealistic portrait', 'background removal').
            
            Allowed ArtifactKinds (for 'inputs'):
            %s
            
            Output ONLY valid JSON:
            {
              "inputs": ["TEXT", "IMAGE"],
              "tier": "balanced",
              "searchString": "keywords"
            }
            """
    ).formatted(kindsJson);

    return new PromptFragment("wording-model-requirement", body);
  }

  /**
   * Reranks a shortlist of candidate models for a given task.
   */
  public static PromptFragment modelReranking() {
    return new PromptFragment(
        "model-reranking",
        """
            You are a model selection specialist. Given a creative generation task and a shortlist
            of candidate AI models, rank them from best to worst fit.
            
            Consider:
            - Semantic match: how well the model's capabilities and description match the task
            - Tier fit: choosing a more expensive or slower model than the task requires is worse
              than choosing a slightly less capable model at the right tier; only prefer a higher
              tier when the task genuinely demands it
            - Special capabilities: unique strengths or specialisations that directly benefit the task
            
            Return ONLY a JSON array of model names in ranked order (best first):
            ["Best Model Name", "Second Best", ...]
            """);
  }

}
