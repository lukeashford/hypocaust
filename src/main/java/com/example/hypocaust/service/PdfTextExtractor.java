package com.example.hypocaust.service;

import java.io.ByteArrayInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PdfTextExtractor {

  public String extract(byte[] pdfBytes) {
    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(document);
    } catch (Exception e) {
      log.warn("PDF text extraction failed: {}", e.getMessage());
      return "";
    }
  }
}
