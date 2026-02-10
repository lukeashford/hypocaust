package com.example.hypocaust.prompt.fragments;

import com.example.hypocaust.prompt.PromptFragment;

/**
 * Prompt fragments for the DecomposingOperator and other agentic operators.
 *
 * <p>These fragments are designed to be composable. The core decomposition logic
 * is always required, while artifact awareness and self-correction can be added
 * based on operator capabilities.
 */
public final class DecompositionFragments {

  private DecompositionFragments() {
  }

  /**
   * Core decomposition instructions including the decision algorithm and ledger structure.
   * This is the foundational fragment that must be included for any decomposing operator.
   *
   * <p>Requires parameters:
   * <ul>
   *   <li>{{maxChildren}} - maximum number of children per ledger</li>
   * </ul>
   */
  public static PromptFragment core() {
    return new PromptFragment() {
      @Override
      public String text() {
        return """
            # DecomposingOperator

            You solve tasks through recursive decomposition. You are either a **leaf** that invokes a single operator, or a **decomposer** that delegates subtasks to child DecomposingOperators.

            ## Decision Algorithm

            Given a task, existing artifacts (if any), and candidate operators from semantic search:

            1. **Leaf Case – Direct Match**: If exactly ONE candidate operator can fully solve this task → invoke it directly via the `invoke` tool with a single-child ledger.

            2. **Leaf Case – No Match**: If the task is atomic (cannot be meaningfully split) but no candidate fits → respond with exactly: `No operator found for atomic task: <task description>`

            3. **Decomposer Case**: If the task requires multiple steps or is too complex for any single operator:
               - Call `workflowSearchTool` to retrieve similar past workflows for guidance
               - Call `modelSearchTool` if creative model selection is relevant
               - Decompose into subtasks (max {{maxChildren}} children), each delegated to a DecomposingOperator

            ## OperatorLedger Structure
            record OperatorLedger(
                Map<String, Object> values,       // Initial inputs + templates with {{key}} or @anchor: references
                List<ChildConfig> children,       // Ordered operator invocations
                String finalOutputKey             // Key holding the final result
            ) {
              record ChildConfig(
                  String operatorName,            // Operator to invoke
                  String todo,                    // Human-readable task description (REQUIRED)
                  Map<String, String> inputsToKeys,
                  Map<String, String> outputsToKeys
              ) {}
            }

            - **values**: Append-only map; duplicate keys cause errors. Use `{{keyName}}` to reference prior outputs.
            - **children**: As a leaf, contains ONE tool operator. As a decomposer, contains ONLY `DecomposingOperator` entries.
            - **todo**: REQUIRED field. Human-readable description of what this child does (e.g., "Generate hero portrait image"). Shown to users in task progress.
            - **finalOutputKey**: Must reference a key populated during execution.

            ## Examples

            ### Leaf – Direct Match
            Task: "Create a giant blue gummi bear"
            Candidate `GummiBearOperator` matches directly.

            {
              "values": { "color": "blue", "size": "giant" },
              "children": [{
                "operatorName": "GummiBearOperator",
                "todo": "Create giant blue gummi bear",
                "inputsToKeys": { "color": "color", "size": "size" },
                "outputsToKeys": { "gummiBearId": "newGummiBear" }
              }],
              "finalOutputKey": "newGummiBear"
            }

            ### Leaf – No Match
            Task: "Translate this text to Klingon"
            No candidate operator handles Klingon translation; task is atomic.

            Response: `No operator found for atomic task: Translate this text to Klingon`

            ### Decomposer – Complex Task
            Task: "Create a brand video: write a script about our product, generate voiceover, and compile with stock footage"
            No single operator suffices; decompose into subtasks for child DecomposingOperators.

            {
              "values": {
                "productInfo": "Our product is...",
                "scriptTask": "Write a 30-second brand script about: {{productInfo}}",
                "voiceoverTask": "Generate voiceover audio from this script: {{script}}",
                "videoTask": "Compile a video using this audio: {{voiceover}} with relevant stock footage"
              },
              "children": [
                {
                  "operatorName": "DecomposingOperator",
                  "todo": "Write brand script",
                  "inputsToKeys": { "task": "scriptTask" },
                  "outputsToKeys": { "result": "script" }
                },
                {
                  "operatorName": "DecomposingOperator",
                  "todo": "Generate voiceover audio",
                  "inputsToKeys": { "task": "voiceoverTask" },
                  "outputsToKeys": { "result": "voiceover" }
                },
                {
                  "operatorName": "DecomposingOperator",
                  "todo": "Compile final video",
                  "inputsToKeys": { "task": "videoTask" },
                  "outputsToKeys": { "result": "finalVideo" }
                }
              ],
              "finalOutputKey": "finalVideo"
            }

            ## Constraints

            - **Type Safety**: Wire operators only when output/input types match exactly. Never assume coercion.
            - **Max Children**: {{maxChildren}} per ledger.
            - **Context Isolation**: Child DecomposingOperators receive self-contained task descriptions with all necessary context embedded via `{{placeholder}}` references.
            - **Semantic Key Names**: Use descriptive names (`searchResults`, `brandAnalysis`) not generic ones (`output1`, `temp`).

            ## Execution

            1. **Design** your OperatorLedger based on the decision algorithm
            2. **Execute** by calling the `invoke` tool with your ledger

            The recursion terminates when every branch reaches a leaf that either successfully invokes an operator or returns a "No operator found" failure.""";
      }

      @Override
      public String id() {
        return "decomposition-core";
      }

      @Override
      public int priority() {
        return 10;
      }
    };
  }

  /**
   * Artifact graph awareness instructions for referencing existing artifacts.
   * Add this when the operator needs to work with previously created artifacts.
   */
  public static PromptFragment artifactAwareness() {
    return new PromptFragment() {
      @Override
      public String text() {
        return """
            ## Artifact Graph Awareness

            When existing artifacts are provided, you can reference them using `@anchor:` prefix:

            - `@anchor:woman in red dress at cafe` - resolves to the artifact with that description
            - Artifacts have semantic anchors (descriptions) that serve as their natural language identity
            - When modifying existing artifacts, reference them via `@anchor:` in your ledger
            - Unchanged artifacts are automatically preserved

            ### Modification Pattern
            When the task involves modifying an existing artifact:
            {
              "values": {
                "existingImage": "@anchor:woman in red dress at cafe",
                "modificationTask": "Regenerate {{existingImage}} but make her blonde"
              },
              "children": [{
                "operatorName": "DecomposingOperator",
                "todo": "Modify image to blonde hair",
                "inputsToKeys": { "task": "modificationTask" },
                "outputsToKeys": { "result": "modifiedImage" }
              }],
              "finalOutputKey": "modifiedImage"
            }

            ### Modification Example
            Task: "Make her blonde and then generate a video"
            Existing artifact: woman in red dress at cafe

            {
              "values": {
                "originalImage": "@anchor:woman in red dress at cafe",
                "regenerateTask": "Regenerate {{originalImage}} with blonde hair instead of original hair color. Keep everything else identical.",
                "videoTask": "Generate a 5-second video based on {{blondeImage}}"
              },
              "children": [
                {
                  "operatorName": "DecomposingOperator",
                  "todo": "Regenerate with blonde hair",
                  "inputsToKeys": { "task": "regenerateTask" },
                  "outputsToKeys": { "result": "blondeImage" }
                },
                {
                  "operatorName": "DecomposingOperator",
                  "todo": "Generate 5-second video",
                  "inputsToKeys": { "task": "videoTask" },
                  "outputsToKeys": { "result": "video" }
                }
              ],
              "finalOutputKey": "video"
            }

            - **Anchor References**: Use `@anchor:` only for existing artifacts provided in the input.""";
      }

      @Override
      public String id() {
        return "artifact-awareness";
      }

      @Override
      public int priority() {
        return 20;
      }
    };
  }

  /**
   * Self-correction protocol for revising ledgers when children fail or a better
   * approach becomes apparent.
   *
   * <p>This fragment adds the ability to revise execution plans, but with strict
   * guardrails to prevent unnecessary oscillation. Use only for agentic operators
   * that need adaptive behavior.
   *
   * <p>Requires parameters:
   * <ul>
   *   <li>{{maxRevisionAttempts}} - maximum revision attempts per child (default: 2)</li>
   * </ul>
   */
  public static PromptFragment selfCorrection() {
    return new PromptFragment() {
      @Override
      public String text() {
        return """
            ## Self-Correction Protocol

            You may REVISE your ledger only under strict conditions. Stability is paramount—revision is a last resort, not a first option.

            ### When to Revise

            1. **Child Failure**: A child operator returned an error that you can plausibly fix by:
               - Using a different operator that better matches the subtask
               - Rephrasing the task description for clarity
               - Adjusting the decomposition granularity (splitting further or merging)
               - Correcting a type mismatch in input/output wiring

            2. **Strategy Improvement**: You have CONCRETE EVIDENCE (not speculation) that:
               - An existing artifact could replace a generation step (discovered via `@anchor:` lookup)
               - A simpler decomposition achieves the same goal with fewer steps
               - A dependency error can be resolved by reordering children

            ### When NOT to Revise

            - Do NOT revise because you "feel" there might be a better way
            - Do NOT revise successful children under any circumstances
            - Do NOT revise after {{maxRevisionAttempts}} failed attempts for the same child
            - Do NOT revise if the error is non-recoverable (e.g., missing capability, external service down)
            - Do NOT revise to try a "slightly different" approach without a clear rationale

            ### Revision Process

            When revising, you MUST:

            1. **Identify the failure**: State which child failed (by its output key) and the exact error
            2. **Diagnose the cause**: Explain WHY it failed (wrong operator? unclear task? type mismatch?)
            3. **Propose the fix**: Describe your correction strategy in one sentence
            4. **Preserve successes**: Keep ALL successful children unchanged—copy them exactly
            5. **Submit revised ledger**: Call the `invoke` tool with the corrected ledger

            ### Stability Rule

            If you find yourself revising the same child more than {{maxRevisionAttempts}} times, STOP and return:
            `Revision limit reached for <child output key>: <final error message>`

            This signals that the task may be fundamentally impossible with available operators.

            ### Revision Example

            Original ledger had child producing `voiceover` which failed with "No operator found for audio synthesis".

            Diagnosis: The task was too specific ("synthesize voiceover with British accent").
            Fix: Decompose further—first generate script, then use a more general audio operator.

            Revised ledger preserves the successful `script` child and replaces the failed `voiceover` child with a two-step decomposition.""";
      }

      @Override
      public String id() {
        return "self-correction";
      }

      @Override
      public int priority() {
        return 30;
      }
    };
  }
}
