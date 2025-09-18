package com.example.the_machine.operator;

import com.example.the_machine.domain.event.OperatorLedger;
import com.example.the_machine.models.ModelProperties;
import com.example.the_machine.models.ModelRegistry;
import com.example.the_machine.operator.registry.OperatorRegistry;
import com.example.the_machine.operator.result.OperatorResult;
import com.fasterxml.jackson.core.Version;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DecomposingOperator extends BaseOperator {

  private final OperatorRegistry operatorRegistry;
  private final ModelRegistry modelRegistry;
  private final ModelProperties modelProperties;

  private static final SystemMessage systemMessage = new SystemMessage("""
      You are part of a tree of operators that decompose a task into smaller pieces,
      until it is sufficiently simple to be executed by less than three tools. You have the option
      to either invoke the tools or decompose further.
      
      Potential candidates for operators will be listed below, including their name and required inputs and
      outputs. You can wire them together using a ledger. The ledger consists of a list of values,
      which serve as a common whiteboard for the operators. One's operator's output may be
      another's input. The ledger is used to store the intermediate results. The ledger also
      specifies the operators to call (children). Their operatorName must match one of the candidates.
      The inputNames and outputNames must be constructed in a way that they form up a valid path,
      according to the tool specifications of the candidates. If you call the invoke tool, the
      values map must contain exactly and only the inputNames of the first child. The first child's
      outputName must be the next child's inputName, and so on. Make sure the types match. You may
      use one child's output also as input for a non-immediate follower.
      """);

  private static final ToolSpec toolSpec = ToolSpec.builder()
      .name("decomposer")
      .version(new Version(0, 0, 1, null, null, null))
      .description("Execute or decompose")
      .inputs(List.of(
          ParamSpec.string("task", true)
      ))
      .outputs(List.of(
          ParamSpec.string("result", true)
      ))
      .build();

  private ChatClient chatClient;

  @Override
  protected OperatorResult doExecute(Map<String, Object> params) {
    final var task = (String) params.get("task");
    final var candidates = operatorRegistry.searchByTask(task);

    final var result = ChatClient.builder(
            modelRegistry.get(modelProperties.getDecomposingModelName()))
        .defaultTools(this)
        .build();

    return OperatorResult.success();
  }

  @Tool(name = "invoke", description = "Invoke a chain of operators, as specified in the ledger")
  public OperatorResult invoke(OperatorLedger ledger) {
    for (final var child : ledger.children()) {
      final var op = operatorRegistry.get(child.operatorName()).orElseThrow();
      final Map<String, Object> inputs = child.inputNames().stream()
          .collect(Collectors.toMap(
                  Function.identity(),
                  ledger.values()::get,
                  (a, b) -> b,
                  LinkedHashMap::new
              )
          );

      final var result = op.execute(inputs);
      if (!result.isOk()) {
        return result;
      }

      ledger.values().put(child.outputName(), result.getOutputs());
    }

// get last childs result
    final var resultKey = ledger.children().getLast().outputName();
    final var resultValue = ledger.values().get(resultKey);

    return OperatorResult.success(
        getName(),
        getVersionString(),
        Map.of("task", ledger.values().get("task")),
        Map.of("result", resultValue)
    );
  }

  @Override
  public ToolSpec spec() {
    return toolSpec;
  }
}
