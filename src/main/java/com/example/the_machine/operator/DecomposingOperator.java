package com.example.the_machine.operator;

import com.example.the_machine.models.ModelProperties;
import com.example.the_machine.models.ModelRegistry;
import com.example.the_machine.operator.registry.OperatorRegistry;
import com.example.the_machine.operator.result.OperatorResult;
import com.example.the_machine.tool.InvokeTool;
import com.example.the_machine.tool.ModelSearchTool;
import com.example.the_machine.tool.WorkflowSearchTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class DecomposingOperator extends BaseOperator {

  private final OperatorRegistry operatorRegistry;
  private final ModelRegistry modelRegistry;
  private final ModelProperties modelProperties;
  private final ObjectMapper mapper;
  private final InvokeTool invokeTool;
  private final WorkflowSearchTool workflowSearchTool;
  private final ModelSearchTool modelSearchTool;

  @Value("${app.decomposer.max-branch-factor:3}")
  private int maxBranchFactor;

  public DecomposingOperator(
      @Lazy OperatorRegistry operatorRegistry,
      ModelRegistry modelRegistry,
      ModelProperties modelProperties,
      ObjectMapper mapper,
      InvokeTool invokeTool,
      WorkflowSearchTool workflowSearchTool,
      ModelSearchTool modelSearchTool
  ) {
    this.operatorRegistry = operatorRegistry;
    this.modelRegistry = modelRegistry;
    this.modelProperties = modelProperties;
    this.mapper = mapper;
    this.invokeTool = invokeTool;
    this.workflowSearchTool = workflowSearchTool;
    this.modelSearchTool = modelSearchTool;
  }

  private SystemMessage buildSystemMessage() {
    return new SystemMessage("""
        # DecomposingOperator
        
        You solve tasks through recursive decomposition. You are either a **leaf** that invokes a single operator, or a **decomposer** that delegates subtasks to child DecomposingOperators.
        
        ## Decision Algorithm
        
        Given a task and candidate operators from semantic search:
        
        1. **Leaf Case – Direct Match**: If exactly ONE candidate operator can fully solve this task → invoke it directly via the `invoke` tool with a single-child ledger.
        
        2. **Leaf Case – No Match**: If the task is atomic (cannot be meaningfully split) but no candidate fits → respond with exactly: `No operator found for atomic task: <task description>`
        
        3. **Decomposer Case**: If the task requires multiple steps or is too complex for any single operator:
           - Call `workflowSearchTool` to retrieve similar past workflows for guidance
           - Call `modelSearchTool` if creative model selection is relevant
           - Decompose into subtasks (max %d children), each delegated to a DecomposingOperator
           - If a child returns a "No operator found" failure, **re-attempt decomposition** with an alternative breakdown
        
        ## OperatorLedger Structure
        record OperatorLedger(
            Map<String, Object> values,       // Initial inputs + templates with {{key}} placeholders
            List<ChildConfig> children,       // Ordered operator invocations
            String finalOutputKey             // Key holding the final result
        ) {
          record ChildConfig(
              String operatorName,            // Operator to invoke
              Map<String, String> inputsToKeys,
              Map<String, String> outputsToKeys
          ) {}
        }
        
        - **values**: Append-only map; duplicate keys cause errors. Use `{{keyName}}` to reference prior outputs.
        - **children**: As a leaf, contains ONE tool operator. As a decomposer, contains ONLY `DecomposingOperator` entries.
        - **finalOutputKey**: Must reference a key populated during execution.
        
        ## Examples
        
        ### Leaf – Direct Match
        Task: "Generate an image of a sunset over mountains"
        Candidate `ImageGenerationOperator` matches directly.
        
        {
          "values": { "prompt": "a sunset over mountains" },
          "children": [{
            "operatorName": "ImageGenerationOperator",
            "inputsToKeys": { "prompt": "prompt" },
            "outputsToKeys": { "image": "generatedImage" }
          }],
          "finalOutputKey": "generatedImage"
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
              "inputsToKeys": { "task": "scriptTask" },
              "outputsToKeys": { "result": "script" }
            },
            {
              "operatorName": "DecomposingOperator",
              "inputsToKeys": { "task": "voiceoverTask" },
              "outputsToKeys": { "result": "voiceover" }
            },
            {
              "operatorName": "DecomposingOperator",
              "inputsToKeys": { "task": "videoTask" },
              "outputsToKeys": { "result": "finalVideo" }
            }
          ],
          "finalOutputKey": "finalVideo"
        }
        
        ## Constraints
        
        - **Type Safety**: Wire operators only when output/input types match exactly. Never assume coercion.
        - **Max Children**: %d per ledger.
        - **Context Isolation**: Child DecomposingOperators receive self-contained task descriptions with all necessary context embedded via `{{placeholder}}` references.
        - **Semantic Key Names**: Use descriptive names (`searchResults`, `brandAnalysis`) not generic ones (`output1`, `temp`).
        
        ## Execution
        
        1. **Design** your OperatorLedger based on the decision algorithm
        2. **Execute** by calling the `invoke` tool with your ledger
        
        The recursion terminates when every branch reaches a leaf that either successfully invokes an operator or returns a "No operator found" failure.
        """.formatted(maxBranchFactor, maxBranchFactor));
  }

  private static final OperatorSpec OPERATOR_SPEC = new OperatorSpec(
      "Decomposer",
      "0.0.1",
      "Execute or decompose",
      List.of(
          ParamSpec.string("task",
              "The task to execute or decompose. Accepts placeholders.",
              true)
      ),
      List.of(
          ParamSpec.string("result", "The result of the operation", true)
      )
  );

  @Override
  protected OperatorResult doExecute(Map<String, Object> params) {
    final var task = (String) params.get("task");
    final var candidates = operatorRegistry.searchByTask(task);
    if (!candidates.contains(spec())) {
      candidates.add(spec());
    }

    // Build a chat client for the decomposing model and expose this operator's tools
    final var client = ChatClient.builder(
            modelRegistry.get(modelProperties.getDecomposingModelName()))
        .defaultTools(invokeTool, workflowSearchTool, modelSearchTool)
        .build();

    // Build the user message payload with the task and candidate tool JSON Schemas
    final var root = mapper.createObjectNode();
    root.put("task", task);
    final var candArray = root.putArray("candidates");
    for (final var spec : candidates) {
      candArray.add(mapper.valueToTree(spec));
    }

    final var userMessage = new UserMessage(root.toPrettyString());
    final var prompt = new Prompt(List.of(buildSystemMessage(), userMessage));

    final var chatResponse = client.prompt(prompt).call();
    final var content = chatResponse.content();

    if (content != null && content.startsWith("No operator found for atomic task:")) {
      return OperatorResult.failure(content, Map.of("task", task));
    }

    return OperatorResult.success(
        "Successfully decomposed task",
        Map.of("task", task),
        Map.of("result", content)
    );
  }

  @Override
  public OperatorSpec spec() {
    return OPERATOR_SPEC;
  }
}
