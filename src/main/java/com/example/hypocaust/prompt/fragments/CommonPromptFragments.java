package com.example.hypocaust.prompt.fragments;

import com.example.hypocaust.prompt.PromptFragment;

/**
 * Common prompt fragments shared across different agents and tools.
 */
public final class CommonPromptFragments {

  private CommonPromptFragments() {
  }

  /**
   * Ability awareness and error handling fragment. Instructs the LLM to fail gracefully when it
   * cannot fulfill a request.
   */
  public static PromptFragment abilityAwareness() {
    return new PromptFragment(
        "common-ability-awareness",
        """
            ABILITY AWARENESS & ERROR HANDLING:
            1. If you cannot find the necessary tools or models to execute the task, do not hallucinate a solution.
            2. Trigger a failure by returning a result with 'success': false.
            3. Provide a clear, descriptive 'errorMessage' explaining exactly what is missing (e.g., "No tool found for video upscaling").
            4. This error message will be shown to the user/client to help them adjust their request.
            """,
        5 // High priority to ensure it's noticed early
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
}
