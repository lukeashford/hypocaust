import {useState} from 'react';
import {ProcessData, TreatmentDocument, VisualAsset} from '../types/interfaces';

// Extended Message interface for ChatInterface
export interface ChatMessage {
  id: string | number;
  content: string;
  isUser: boolean;
  timestamp: Date;
  isTyping?: boolean;
  data?: any;
  storyContent?: string | null;
  treatmentData?: TreatmentData | null;
  assets?: VisualAsset[] | null;
}

// Treatment data interface for download functionality
export interface TreatmentData {
  title: string;
  pages: number;
  size: string;
  timestamp: string;
  fullData: TreatmentDocument;
}

// Return type for the custom hook
export interface UseChatStateReturn {
  // State variables
  messages: ChatMessage[];
  isProcessing: boolean;
  error: string | null;
  showWelcome: boolean;
  generatedAssets: VisualAsset[];
  finalTreatment: TreatmentData | null;
  processData: ProcessData | null;

  // State setters
  setMessages: React.Dispatch<React.SetStateAction<ChatMessage[]>>;
  setIsProcessing: React.Dispatch<React.SetStateAction<boolean>>;
  setError: React.Dispatch<React.SetStateAction<string | null>>;
  setShowWelcome: React.Dispatch<React.SetStateAction<boolean>>;
  setGeneratedAssets: React.Dispatch<React.SetStateAction<VisualAsset[]>>;
  setFinalTreatment: React.Dispatch<React.SetStateAction<TreatmentData | null>>;
  setProcessData: React.Dispatch<React.SetStateAction<ProcessData | null>>;

  // Functions
  addMessage: (
      content: string,
      isUser?: boolean,
      timestamp?: Date | null,
      isTyping?: boolean,
      data?: any,
      storyContent?: string | null,
      treatmentData?: TreatmentData | null
  ) => void;
}

/**
 * Custom hook for managing chat state in ChatInterface
 * Extracts all message and processing state management logic
 */
export const useChatState = (): UseChatStateReturn => {
  // Core state
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [showWelcome, setShowWelcome] = useState<boolean>(true);

  // Generated content state
  const [generatedAssets, setGeneratedAssets] = useState<VisualAsset[]>([]);
  const [finalTreatment, setFinalTreatment] = useState<TreatmentData | null>(null);
  const [processData, setProcessData] = useState<ProcessData | null>(null);

  /**
   * Add a new message to the chat
   */
  const addMessage = (
      content: string,
      isUser: boolean = false,
      timestamp: Date | null = null,
      isTyping: boolean = false,
      data: any = null,
      storyContent: string | null = null,
      treatmentData: TreatmentData | null = null
  ): void => {
    const newMessage: ChatMessage = {
      id: Date.now() + Math.random(),
      content,
      isUser,
      timestamp: timestamp || new Date(),
      isTyping,
      data,
      storyContent,
      treatmentData,
      assets: data && Array.isArray(data) ? data : null
    };

    setMessages(prev => [...prev, newMessage]);
  };

  return {
    // State variables
    messages,
    isProcessing,
    error,
    showWelcome,
    generatedAssets,
    finalTreatment,
    processData,

    // State setters
    setMessages,
    setIsProcessing,
    setError,
    setShowWelcome,
    setGeneratedAssets,
    setFinalTreatment,
    setProcessData,

    // Functions
    addMessage,
  };
};