package com.example.the_machine.operator;

import com.example.the_machine.operator.result.OperatorResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegerAdderOperator extends BaseOperator {

  @Override
  protected OperatorResult doExecute(Map<String, Object> normalizedInputs) {
    Integer first = (Integer) normalizedInputs.get("first");
    Integer second = (Integer) normalizedInputs.get("second");

    Integer result = first + second;

    Map<String, Object> outputs = Map.of("result", result);

    return OperatorResult.success(
        "Successfully added integers",
        normalizedInputs,
        outputs
    );
  }

  @Override
  public OperatorSpec spec() {
    return new OperatorSpec(
        "IntegerAdder",
        "1.0.0",
        "Adds two integers together",
        List.of(
            ParamSpec.integer("first", "First integer to add", true),
            ParamSpec.integer("second", "Second integer to add", true)
        ),
        List.of(
            ParamSpec.integer("result", "Sum of the two integers", true)
        )
    );
  }
}