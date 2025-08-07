package com.example.tool.extract;

import java.io.Serializable;

/**
 * Represents extracted page content.
 */
public record PageContent(String title, String text, String excerpt) implements Serializable {

}