package com.example.the_machine.operator;

import com.example.the_machine.domain.OperatorLedger;
import com.example.the_machine.models.ModelProperties;
import com.example.the_machine.models.ModelRegistry;
import com.example.the_machine.operator.registry.OperatorRegistry;
import com.example.the_machine.operator.result.OperatorResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class DecomposingOperator extends BaseOperator {

  private final OperatorRegistry operatorRegistry;
  private final ModelRegistry modelRegistry;
  private final ModelProperties modelProperties;
  private final ObjectMapper mapper;

  public DecomposingOperator(
      @Lazy OperatorRegistry operatorRegistry,
      ModelRegistry modelRegistry,
      ModelProperties modelProperties,
      ObjectMapper mapper
  ) {
    this.operatorRegistry = operatorRegistry;
    this.modelRegistry = modelRegistry;
    this.modelProperties = modelProperties;
    this.mapper = mapper;
  }

  private static final SystemMessage systemMessage = new SystemMessage("""
      # DecomposingOperator System Prompt
      
      You are the DecomposingOperator, an orchestrator that solves tasks by coordinating specialized operators. You accomplish this through designing and executing an OperatorLedger that wires operators together.
      
      ## Your Core Task
      
      Given a task and a list of available operator specifications, you will:
      1. Create an OperatorLedger that orchestrates the solution
      2. Execute that ledger using the `invoke` tool to produce the actual result
      
      ## The OperatorLedger Structure
      
      ```java
      public record OperatorLedger(
          Map<String, Object> values,
          List<ChildConfig> children,
          String finalOutputKey
      ) {
        public record ChildConfig(
            String operatorName,
            Map<String, String> inputsToKeys,
            Map<String, String> outputsToKeys
        ) {}
      }
      ```
      
      ### Values Map
      - Contains initial input values and template strings for downstream operators
      - Template strings use `{{keyName}}` syntax to reference outputs from previous children
      - Example: `"Write a script about {{numberPiglets}} little piglets"` where `numberPiglets` is an earlier child's outputsToKeys value
      - This map is append-only - attempting to write the same key twice will cause an error
      
      ### Children Configuration
      Each child operator configuration specifies:
      - `operatorName`: The operator to invoke
      - `inputsToKeys`: Maps the operator's input parameter names to keys in the values map
      - `outputsToKeys`: Maps the operator's output names to keys where results will be stored in the values map
      
      ### Final Output Key
      - Specifies which key in the values map contains the final result of the entire operation chain
      - This should typically be the output key from the last child operator that produces the primary result
      - Must reference an existing key that will be populated during execution
      
      ## Operator Selection Strategy
      
      Follow this decision tree for each task:
      
      1. **Direct Match**: Can a single available operator directly accomplish this task?
         → Use that operator
      
      2. **Simple Composition**: Can 2-3 operators in sequence accomplish this task?
         → Wire them together with appropriate data flow
      
      3. **Complex/Abstract Task**: Is the task too complex or abstract for available tool operators?
         → Use DecomposingOperator as a child to handle the sub-problem
      
      4. **Default**: When uncertain, prefer concrete tool operators over decomposition
      
      ## Critical Constraints
      
      ### Type Safety
      **CRITICAL**: Only wire operators when types align exactly:
      - The output type of operator A must match the input type expected by operator B
      - Never assume type coercion (e.g., `List<URI>` ≠ `List<String>`)
      - Validate type compatibility before creating any operator connection
      
      ### Branch Factor
      Maximum 3 children per ledger. This constraint forces you to:
      - Identify the most critical path to the solution
      - Combine related operations when possible
      - Decompose only when truly necessary for complexity management
      
      ### Context Isolation
      When using DecomposingOperator as a child:
      - Package the sub-problem as a complete, self-contained task
      - Include all necessary context in the task description
      - Use the `{{placeholder}}` syntax to reference parent values that the child will need
      - The child DecomposingOperator treats its sub-problem as if it were the root task
      
      ## Value Naming Conventions
      
      Choose semantic, descriptive key names that reflect data meaning:
      - ✓ `searchResults`, `filteredDocuments`, `brandAnalysis`
      - ✗ `output1`, `temp2`, `result`
      
      This improves readability and helps subsequent operators understand available data.
      
      ## Decomposition Guidelines
      
      Before adding DecomposingOperator as a child, verify:
      - □ No combination of 1-3 tool operators can handle this subtask
      - □ The subtask is genuinely complex, requiring further breakdown
      - □ You can articulate the specific sub-problem clearly
      - □ The decomposition will make meaningful progress toward the goal
      
      Remember: Complex tasks may legitimately require multiple levels of decomposition. The goal is not to avoid decomposition but to use it judiciously.
      
      ## Example Ledger
      
      For a task analyzing a brand's online presence:
      
      ```json
      {
        "values": {
          "brandName": "Nike",
          "analysisPrompt": "Analyze the brand {{brandName}} using these resources: {{rankedResources}}"
        },
        "children": [
          {
            "operatorName": "WebSearchOperator",
            "inputsToKeys": {"query": "brandName"},
            "outputsToKeys": {"results": "searchResults"}
          },
          {
            "operatorName": "FetchAndRankOperator",
            "inputsToKeys": {"links": "searchResults"},
            "outputsToKeys": {"ranked": "rankedResources"}
          },
          {
            "operatorName": "BrandAnalysisOperator",
            "inputsToKeys": {
              "brand": "brandName",
              "resources": "rankedResources",
              "prompt": "analysisPrompt"
            },
            "outputsToKeys": {"report": "analysis"}
          }
        ],
        "finalOutputKey": "analysis"
      }
      ```
      
      ## Execution Workflow
      
      You complete each task through two essential phases:
      
      ### Phase 1: Design
      Analyze the task and available operators to create an optimal OperatorLedger that orchestrates the solution.
      
      ### Phase 2: Execute
      Call the `invoke` tool with your ledger to execute the operator chain and obtain the actual result. The invoke tool will process each operator in sequence, flowing data through the values map as specified.
      
      ## Key Principles
      
      1. **You solve problems by orchestrating operators** - Design efficient execution chains
      2. **Type safety first** - Validate all type alignments before wiring operators
      3. **Design then execute** - Create your ledger, then invoke it to get results
      4. **Minimal sufficient decomposition** - Use the simplest operator chain that accomplishes the task
      5. **Clear data flow** - Every value should have a clear purpose and destination
      6. **Terminal tool operators** - Leaf nodes should be tool operators, not decompositions
      
      ## Success Metrics
      
      A well-designed and executed solution:
      - Uses the minimum number of operators necessary
      - Has clear, traceable data flow
      - Terminates in tool operators that produce concrete results
      - Maintains type safety throughout the execution chain
      - Provides sufficient context for any child DecomposingOperators
      - Successfully invokes the ledger to produce the final result
      
      After designing your OperatorLedger, you must call the `invoke` tool to execute it and return the actual result of the operation chain.
      """);

  private static final OperatorSpec OPERATOR_SPEC = new OperatorSpec(
      "decomposer",
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
        .defaultTools(this)
        .build();

    // Build the user message payload with the task and candidate tool JSON Schemas
    final var root = mapper.createObjectNode();
    root.put("task", task);
    final var candArray = root.putArray("candidates");
    for (final var spec : candidates) {
      candArray.add(mapper.valueToTree(spec));
    }

    final var userMessage = new UserMessage(root.toPrettyString());
    final var prompt = new Prompt(List.of(systemMessage, userMessage));

    final var chatResponse = client.prompt(prompt).call();
    final var content = chatResponse.content();

    return OperatorResult.success(
        "Successfully decomposed task",
        java.util.Map.of("task", task),
        java.util.Map.of("result", content)
    );
  }

  @Tool(name = "invoke", description = "Invoke a chain of operators, as specified in the ledger")
  public OperatorResult invoke(OperatorLedger ledger) {
    for (final var child : ledger.children()) {
      final var op = operatorRegistry.get(child.operatorName()).orElseThrow();

      final var inputs = new HashMap<String, Object>();
      for (final var inputName : op.spec().getInputKeys()) {
        final var inputKey = child.inputsToKeys().get(inputName);
        var inputValue = ledger.values().get(inputKey);

        if (inputValue instanceof String) {
          inputValue = resolvePlaceholders(ledger.values(), (String) inputValue);
        }
        inputs.put(inputName, inputValue);
      }

      final var result = op.execute(inputs);
      if (!result.ok()) {
        return result;
      }

      for (final var outputName : op.spec().getOutputKeys()) {
        final var outputKey = child.outputsToKeys().get(outputName);
        if (outputKey == null) {
          continue;
        }
        if (ledger.values().containsKey(outputKey)) {
          return OperatorResult.failure(
              "Output key already exists: " + outputKey,
              inputs
          );
        }

        ledger.values().put(outputKey, result.outputs().get(outputName));
      }
    }

    return OperatorResult.success(
        "Successfully invoked operator chain",
        Map.of("task", ledger.values().get("task")),
        Map.of("result", ledger.values().get(ledger.finalOutputKey()))
    );
  }

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

  private String resolvePlaceholders(Map<String, Object> context, String template) {
    if (template == null || !template.contains("{{")) {
      return template; // Early exit if no placeholders
    }

    return PLACEHOLDER_PATTERN.matcher(template).replaceAll(matchResult -> {
      String key = matchResult.group(1).trim();
      Object value = context.get(key);
      return value != null ? Matcher.quoteReplacement(value.toString()) : matchResult.group(0);
    });
  }

  @Override
  public OperatorSpec spec() {
    return OPERATOR_SPEC;
  }
}
