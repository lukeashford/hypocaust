package com.example.exception;

/**
 * Custom exception thrown when JSON parsing fails during brand analysis. This exception indicates
 * that the LLM response could not be parsed into a CompanyAnalysisDto.
 */
public class BrandAnalysisParsingException extends RuntimeException {

  private final String companyName;
  private final String rawResponse;

  public BrandAnalysisParsingException(String companyName, String rawResponse, String message,
      Throwable cause) {
    super(message, cause);
    this.companyName = companyName;
    this.rawResponse = rawResponse;
  }

  public BrandAnalysisParsingException(String companyName, String rawResponse, String message) {
    super(message);
    this.companyName = companyName;
    this.rawResponse = rawResponse;
  }

  public String getCompanyName() {
    return companyName;
  }

  public String getRawResponse() {
    return rawResponse;
  }
}