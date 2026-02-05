package com.example.hypocaust.prompt.fragments;

import com.example.hypocaust.prompt.PromptFragment;

/**
 * Prompt fragments for inlineContent generation operators.
 *
 * <p>These fragments provide specialized instructions for image prompt engineering,
 * title generation, and creative writing tasks. They are designed to be used with smaller, faster
 * models like Haiku for efficiency.
 */
public final class GenerationFragments {

  private GenerationFragments() {
  }

  /**
   * Image prompt engineering instructions for transforming concepts into detailed prompts optimized
   * for AI image generation models.
   */
  public static PromptFragment imagePromptEngineering() {
    return new PromptFragment() {
      @Override
      public String text() {
        return """
            You are an expert at crafting optimal prompts for AI image generation models like DALL-E and Stable Diffusion.
            Given a concept, expand it into a detailed, effective prompt that will produce high-quality results.
            Include relevant artistic details, composition notes, lighting, and technical parameters.
            Also provide a negative prompt to avoid common issues.
            
            Guidelines:
            - Be specific about visual elements: subject, setting, lighting, camera angle
            - Include style keywords: photorealistic, cinematic, oil painting, digital art, etc.
            - Add quality modifiers: highly detailed, 8k, professional, masterpiece
            - Consider composition: rule of thirds, centered, dynamic pose, etc.
            - For negative prompts: exclude common artifacts, unwanted styles, quality issues
            
            Return your response in this exact format (one line each):
            PROMPT: [detailed positive prompt]
            NEGATIVE: [negative prompt]""";
      }

      @Override
      public String id() {
        return "image-prompt-engineering";
      }

      @Override
      public int priority() {
        return 10;
      }
    };
  }

  /**
   * Title and metadata generation for images. Extracts concise, descriptive titles and alt text
   * from image prompts.
   */
  public static PromptFragment imageTitleGeneration() {
    return new PromptFragment() {
      @Override
      public String text() {
        return """
            Generate a short, descriptive title, subtitle, and alt text for an AI-generated image.
            
            Guidelines:
            - Title: Catchy and concise, captures the essence (max 6 words)
            - Subtitle: Describes the style, mood, or technique (max 8 words)
            - Alt: Accessibility-focused description for screen readers (max 20 words)
            
            Return your response in exactly this format (one line each, no extra text):
            TITLE: [A concise, engaging title, max 6 words]
            SUBTITLE: [A brief subtitle describing the style or mood, max 8 words]
            ALT: [Descriptive alt text for accessibility, max 20 words]""";
      }

      @Override
      public String id() {
        return "image-title-generation";
      }

      @Override
      public int priority() {
        return 10;
      }
    };
  }

  /**
   * Title generation for creative writing pieces. Extracts engaging titles from inlineContent.
   */
  public static PromptFragment writingTitleGeneration() {
    return new PromptFragment() {
      @Override
      public String text() {
        return """
            Generate a short, engaging title for this creative writing piece.
            
            Guidelines:
            - Capture the essence or theme of the content
            - Be intriguing but not misleading
            - Avoid generic titles like "A Story" or "Creative Writing"
            - Maximum 8 words
            
            Return ONLY the title, nothing else.""";
      }

      @Override
      public String id() {
        return "writing-title-generation";
      }

      @Override
      public int priority() {
        return 10;
      }
    };
  }

  /**
   * Creative writing instructions.
   *
   * <p>Requires parameters:
   * <ul>
   *   <li>{{style}} - writing style (creative, professional, casual, etc.)</li>
   *   <li>{{maxLength}} - maximum length in words</li>
   * </ul>
   */
  public static PromptFragment creativeWriting() {
    return new PromptFragment() {
      @Override
      public String text() {
        return """
            You are a creative writer. Generate content in a {{style}} style.
            Keep your response under {{maxLength}} words.
            
            Guidelines:
            - Match the requested style consistently throughout
            - Use vivid, engaging language appropriate to the style
            - Structure the content with a clear beginning, middle, and end
            - Avoid clichés and generic phrases
            - Focus on originality and reader engagement""";
      }

      @Override
      public String id() {
        return "creative-writing";
      }

      @Override
      public int priority() {
        return 10;
      }
    };
  }
}
