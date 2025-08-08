import openaiService from './openaiService';
import type {
  ChatContext,
  FeedbackResponse,
  Message,
  StreamChunkCallback,
  Suggestion
} from '../types/interfaces';

/**
 * Chat Service for handling conversational interactions
 * Provides streaming responses and context management for the chat interface
 */

class ChatService {
  private conversationHistory: Message[] = [];

  /**
   * Adds a message to the conversation history
   * @param role - Message role ('user' or 'assistant')
   * @param content - Message content
   */
  addMessage(role: 'user' | 'assistant', content: string): void {
    this.conversationHistory?.push({
      role,
      content,
      timestamp: new Date()
    });

    // Keep only the last 10 messages to prevent token limit issues
    if (this.conversationHistory?.length > 10) {
      this.conversationHistory = this.conversationHistory?.slice(-10);
    }
  }

  /**
   * Processes user input and generates streaming response
   * @param userInput - User's message
   * @param onChunk - Callback for streaming response chunks
   * @param context - Additional context for the conversation
   * @returns Complete response
   */
  async processUserInput(
      userInput: string,
      onChunk: StreamChunkCallback,
      context: ChatContext = {}
  ): Promise<string> {
    try {
      // Add user message to history
      this.addMessage('user', userInput);

      // Build context-aware system prompt
      const systemPrompt = this.buildSystemPrompt(context);

      // Prepare messages for API
      const messages: Message[] = [
        {role: 'system', content: systemPrompt},
        ...this.conversationHistory?.map(msg => ({
          role: msg?.role,
          content: msg?.content
        }))
      ];

      let completeResponse = '';

      // Stream response
      await openaiService?.getStreamingResponse(
          userInput,
          (chunk: string) => {
            completeResponse += chunk;
            onChunk?.(chunk);
          }
      );

      // Add assistant response to history
      this.addMessage('assistant', completeResponse);

      return completeResponse;
    } catch (error) {
      console.error('Chat service error:', error);
      throw new Error(
          `Chat processing failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  /**
   * Builds system prompt based on current context
   * @param context - Conversation context
   * @returns System prompt
   */
  private buildSystemPrompt(context: ChatContext): string {
    let systemPrompt = `You are an expert creative director and brand strategist for CinematicBrand Director. 
You help users create professional director's treatments for cinematic marketing videos.

Your expertise includes:
- Brand analysis and positioning
- Cinematic storytelling techniques
- Visual direction and production planning
- Industry-standard treatment formatting
- Creative concept development

Always provide professional, actionable advice that would be valuable for video production teams and clients.`;

    // Add context-specific instructions
    if (context?.currentStep) {
      switch (context?.currentStep) {
        case 1:
          systemPrompt += `\n\nCurrently assisting with company research and brand analysis. Focus on providing insights about brand personality, target audience, and creative opportunities.`;
          break;
        case 2:
          systemPrompt += `\n\nCurrently working on story development. Help refine narrative concepts, character development, and cinematic structure.`;
          break;
        case 3:
          systemPrompt += `\n\nCurrently developing visual concepts. Provide guidance on visual style, production design, and creative execution.`;
          break;
        case 4:
          systemPrompt += `\n\nCurrently finalizing the treatment. Help with production considerations, technical specifications, and presentation formatting.`;
          break;
      }
    }

    if (context?.brandName) {
      systemPrompt += `\n\nCurrently working on a treatment for: ${context?.brandName}`;
    }

    if (context?.mode) {
      systemPrompt += `\n\nGeneration mode: ${context?.mode === 'interactive'
          ? 'Interactive (step-by-step with user feedback)' : 'One-shot (automated generation)'}`;
    }

    return systemPrompt;
  }

  /**
   * Processes feedback and suggestions
   * @param feedback - User feedback
   * @param currentData - Current step data
   * @returns Refined suggestions
   */
  async processFeedback(feedback: string, currentData: any): Promise<FeedbackResponse> {
    try {
      return await openaiService?.getStructuredChatCompletion<FeedbackResponse>(
          `Based on this user feedback: "${feedback}"
         
         Current data: ${JSON.stringify(currentData)}
         
         Provide specific suggestions for improving the current step based on the feedback.`,
          {
            suggestions: 'array of specific improvement suggestions',
            refinements: 'array of refinements to apply',
            nextSteps: 'recommended next steps'
          }
      );
    } catch (error) {
      console.error('Feedback processing error:', error);
      throw new Error(`Feedback processing failed: ${error instanceof Error ? error.message
          : 'Unknown error'}`);
    }
  }

  /**
   * Clears conversation history
   */
  clearHistory(): void {
    this.conversationHistory = [];
  }

  /**
   * Gets conversation history
   * @returns Conversation history
   */
  getHistory(): Message[] {
    return [...this.conversationHistory];
  }

  /**
   * Generates suggestions based on current context
   * @param context - Current context
   * @returns Array of suggestions
   */
  async generateSuggestions(context: ChatContext): Promise<Suggestion[]> {
    try {
      const prompt = `Based on the current context, suggest 3-5 helpful questions or actions the user might want to take next:
      
      Context: ${JSON.stringify(context)}`;

      const response = await openaiService?.getStructuredChatCompletion<{
        suggestions: Suggestion[]
      }>(prompt, {
        suggestions: 'array of suggestion objects with text and action properties'
      });

      return response?.suggestions || [];
    } catch (error) {
      console.error('Suggestion generation error:', error);
      return [];
    }
  }
}

// Export singleton instance
const chatService = new ChatService();
export default chatService;