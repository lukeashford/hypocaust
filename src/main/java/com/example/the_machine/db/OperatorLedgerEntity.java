package com.example.the_machine.db;

import jakarta.persistence.Entity;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OperatorLedgerEntity extends BaseEntity {

  private Map<String, Object> values;

  private List<OperatorArgs> opsToArgs;

  public record OperatorArgs(String operatorName, List<String> inputNames, String outputName) {

  }
}
