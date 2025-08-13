import {useCallback, useEffect, useState} from 'react';
import aiAgentService from '../services/aiAgentService';
import {
  GenerationMode,
  ProcessCallbacks,
  ProcessData,
  StepType,
  VisualAsset
} from '../types/interfaces';

// Agent status type
export type AgentStatus = 'idle' | 'processing' | 'completed' | 'error';

// Step data interface for interactive prompts
export interface StepData {
  summary?: string;
  content?: string;
  keyPoints?: string[];
  assets?: VisualAsset[];
}

// Agent callback function types
export interface AgentCallbacks {
  onStepComplete: (step: number, type: StepType, data: any) => void;
  onSendMessage: (message: string) => Promise<void>;
  onInteractiveContinue: () => Promise<void>;
  onInteractiveFeedback: (feedback: string) => Promise<void>;
  onInteractiveRegenerate: () => Promise<void>;
}

// Dependencies interface for callback functions
export interface UseAIAgentDependencies {
  mode: GenerationMode;
  addMessage: (
      content: string,
      isUser?: boolean,
      timestamp?: Date | null,
      isTyping?: boolean,
      data?: any,
      storyContent?: string | null,
      treatmentData?: any | null
  ) => void;
  setIsProcessing: React.Dispatch<React.SetStateAction<boolean>>;
  setError: React.Dispatch<React.SetStateAction<string | null>>;
  setShowWelcome: React.Dispatch<React.SetStateAction<boolean>>;
  setGeneratedAssets: React.Dispatch<React.SetStateAction<VisualAsset[]>>;
  setFinalTreatment: React.Dispatch<React.SetStateAction<any>>;
  setProcessData: React.Dispatch<React.SetStateAction<ProcessData | null>>;
}

// Return type for the custom hook
export interface UseAIAgentReturn {
  // State variables
  currentStep: number;
  agentStatus: AgentStatus;
  currentStepData: StepData | null;
  showInteractivePrompt: boolean;

  // State setters
  setCurrentStep: React.Dispatch<React.SetStateAction<number>>;
  setAgentStatus: React.Dispatch<React.SetStateAction<AgentStatus>>;
  setCurrentStepData: React.Dispatch<React.SetStateAction<StepData | null>>;
  setShowInteractivePrompt: React.Dispatch<React.SetStateAction<boolean>>;

  // Agent callback functions
  handleStepComplete: (step: number, type: StepType, data: any) => void;
  handleSendMessage: (message: string) => Promise<void>;
  handleInteractiveContinue: () => Promise<void>;
  handleInteractiveFeedback: (feedback: string) => Promise<void>;
  handleInteractiveRegenerate: () => Promise<void>;
}

/**
 * Custom hook for managing AI agent interactions
 * Extracts agent state management and callback functions from ChatInterface
 */
export const useAIAgent = (dependencies: UseAIAgentDependencies): UseAIAgentReturn => {
  const {
    mode,
    addMessage,
    setIsProcessing,
    setError,
    setShowWelcome,
    setGeneratedAssets,
    setFinalTreatment,
    setProcessData,
  } = dependencies;

  // AI Agent state
  const [currentStep, setCurrentStep] = useState<number>(0);
  const [agentStatus, setAgentStatus] = useState<AgentStatus>('idle');
  const [showInteractivePrompt, setShowInteractivePrompt] = useState<boolean>(false);
  const [currentStepData, setCurrentStepData] = useState<StepData | null>(null);

  /**
   * Handle completion of a step in the AI agent process
   */
  const handleStepComplete = useCallback((_step: number, type: StepType, data: any) => {
    switch (type) {
      case 'research':
        addMessage(`I've completed the research for your brand. Here's what I found:`, false, null,
            false, data);
        if (mode === 'interactive') {
          setShowInteractivePrompt(true);
          setCurrentStepData(data);
          setIsProcessing(false);
        }
        break;

      case 'story':
        const storyContent = `${data?.title}\n\n${data?.concept}\n\n${data?.storyOutline}`;
        addMessage("I've created a cinematic story outline for your brand:", false, null, false,
            null, storyContent);
        if (mode === 'interactive') {
          setShowInteractivePrompt(true);
          setCurrentStepData({summary: data?.concept, content: storyContent});
          setIsProcessing(false);
        }
        break;

      case 'visuals':
        setGeneratedAssets(data?.assets || []);
        addMessage("Visual assets and concepts have been generated:", false, null, false,
            data?.assets);
        if (mode === 'interactive') {
          setShowInteractivePrompt(true);
          setCurrentStepData({
            summary: "High-quality visual assets for video production",
            keyPoints: [
              `${data?.visualConcepts?.characters?.length || 0} character designs created`,
              `${data?.assets?.length || 0} AI-generated images produced`,
              `Color palette and lighting specifications defined`
            ]
          });
          setIsProcessing(false);
        }
        break;

      case 'final':
        const treatmentData = {
          title: data?.title || "Director's Treatment",
          pages: data?.documentMetadata?.pages || 12,
          size: data?.documentMetadata?.size || "2.4 MB",
          timestamp: "Just now",
          fullData: data
        };

        setFinalTreatment(treatmentData);
        addMessage("Your complete director's treatment is ready for download!", false, null, false,
            null, null, treatmentData);
        setIsProcessing(false);
        setAgentStatus('completed');
        break;
    }
  }, [mode, addMessage, setIsProcessing, setGeneratedAssets, setFinalTreatment]);

  /**
   * Handle sending a message and starting AI processing
   */
  const handleSendMessage = useCallback(async (message: string) => {
    if (setShowWelcome) {
      setShowWelcome(false);
    }

    // Add user message
    addMessage(message, true);

    // Reset error state
    setError(null);

    try {
      // Start AI processing
      await aiAgentService?.generateTreatment(message, mode);
    } catch (error: any) {
      console.error('Generation error:', error);
      setError(error?.message || 'Failed to generate treatment');
      addMessage(`Sorry, I encountered an error: ${error?.message || 'Unknown error'}`, false);
      setIsProcessing(false);
      setAgentStatus('idle');
    }
  }, [mode, addMessage, setError, setShowWelcome, setIsProcessing]);

  /**
   * Handle continuing to the next step in interactive mode
   */
  const handleInteractiveContinue = useCallback(async () => {
    try {
      setShowInteractivePrompt(false);
      const result = await aiAgentService?.continueToNextStep();

      if (result?.complete) {
        // Final step completed
        setProcessData(result?.data);
      }
    } catch (error: any) {
      console.error('Continue error:', error);
      setError(error?.message || 'Failed to continue to next step');
      addMessage(`Error continuing: ${error?.message || 'Unknown error'}`, false);
      setIsProcessing(false);
    }
  }, [setShowInteractivePrompt, setProcessData, setError, addMessage, setIsProcessing]);

  /**
   * Handle user feedback in interactive mode
   */
  const handleInteractiveFeedback = useCallback(async (feedback: string) => {
    addMessage(feedback, true);
    addMessage(
        "Thank you for the feedback! I'll incorporate your suggestions and continue with the next step.",
        false);

    try {
      const result = await aiAgentService?.continueToNextStep(feedback);

      if (result?.complete) {
        setProcessData(result?.data);
      }
    } catch (error: any) {
      console.error('Feedback error:', error);
      setError(error?.message || 'Failed to apply feedback');
      addMessage(`Error applying feedback: ${error?.message || 'Unknown error'}`, false);
      setIsProcessing(false);
    }
  }, [addMessage, setProcessData, setError, setIsProcessing]);

  /**
   * Handle regenerating the current step
   */
  const handleInteractiveRegenerate = useCallback(async () => {
    addMessage("Regenerating this step with fresh creative direction...", false);
    setShowInteractivePrompt(false);

    try {
      await aiAgentService?.regenerateStep(currentStep);
    } catch (error: any) {
      console.error('Regenerate error:', error);
      setError(error?.message || 'Failed to regenerate step');
      addMessage(`Error regenerating: ${error?.message || 'Unknown error'}`, false);
      setIsProcessing(false);
    }
  }, [addMessage, setShowInteractivePrompt, currentStep, setError, setIsProcessing]);

  // Setup AI Agent callbacks
  useEffect(() => {
    const callbacks: ProcessCallbacks = {
      onProgress: (status: string, step: number) => {
        if (step >= 4) {
          // Completion state
          setAgentStatus('completed');
          setCurrentStep(4);  // Set to totalSteps for 100% progress
        } else {
          setAgentStatus('processing');
          setCurrentStep(step);
        }
        setIsProcessing(step < 4);  // Stop processing when complete
        setError(null);
      },
      onStepComplete: (step: number, type: string, data: any) => {
        handleStepComplete(step, type as StepType, data);
      },
      onError: (error: Error, step: number) => {
        console.error('AI Agent Error:', error);
        setError(error?.message || 'An error occurred during generation');
        setIsProcessing(false);
        setAgentStatus('idle');
        addMessage(`Error in step ${step}: ${error?.message || 'Unknown error occurred'}`, false);
      },
      onComplete: (completeData: ProcessData) => {
        setProcessData(completeData);
        setIsProcessing(false);
        setAgentStatus('completed');
      }
    };

    aiAgentService?.setCallbacks(callbacks);
  }, [handleStepComplete, setAgentStatus, setCurrentStep, setIsProcessing, setError, addMessage,
    setProcessData]);

  return {
    // State variables
    currentStep,
    agentStatus,
    currentStepData,
    showInteractivePrompt,

    // State setters
    setCurrentStep,
    setAgentStatus,
    setCurrentStepData,
    setShowInteractivePrompt,

    // Agent callback functions
    handleStepComplete,
    handleSendMessage,
    handleInteractiveContinue,
    handleInteractiveFeedback,
    handleInteractiveRegenerate,
  };
};