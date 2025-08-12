/**
 * TypeScript interfaces for CinematicBrand Director services
 * Defines data structures used across aiAgentService, chatService, and openaiService
 */

// Message and Chat Interfaces
export interface Message {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp?: Date;
}

export interface ChatContext {
  currentStep?: number;
  brandName?: string;
  mode?: 'interactive' | 'oneshot';
}

export interface FeedbackResponse {
  suggestions: string[];
  refinements: string[];
  nextSteps: string;
}

export interface Suggestion {
  text: string;
  action: string;
}

// Story and Scene Interfaces
export interface Scene {
  sceneNumber: number;
  location: string;
  description: string;
  visualNotes?: string;
}

export interface StoryOutline {
  title: string;
  concept: string;
  storyOutline: string;
  keyScenes: Scene[];
  tone: string;
  duration: string;
}

// Visual Concepts Interfaces
export interface Character {
  name: string;
  description: string;
  costume: string;
  visualNotes?: string;
}

export interface SetDesign {
  location: string;
  description: string;
  props?: string[];
}

export interface VisualConcepts {
  characters: Character[];
  colorPalette: string[];
  lightingStyle: string;
  setDesign: SetDesign[];
  visualEffects?: string;
  productPlacement?: string;
}

// Asset Interfaces
export interface ImagePrompt {
  prompt: string;
  title: string;
  type: 'character' | 'set' | 'product' | 'scene';
}

export interface VisualAsset {
  type: 'image';
  url: string;
  title: string;
  description: string;
  category: 'character' | 'set' | 'product' | 'scene';
}

// Treatment Document Interfaces
export interface DocumentMetadata {
  pages: number;
  size: string;
  format: string;
}

export interface TreatmentDocument {
  title: string;
  executiveSummary: string;
  creativeStrategy: string;
  storyBreakdown: string;
  visualDirection: string;
  productionNotes: string;
  castingNotes?: string;
  locationRequirements?: string;
  technicalSpecifications?: string;
  budgetConsiderations?: string;
  timeline?: string;
  postProductionNotes?: string;
  brandName?: string;
  generatedAt?: string;
  totalAssets?: number;
  documentMetadata?: DocumentMetadata;
}

import type {
  CompanyAnalysisDto,
  StoryOutlineDto,
  TreatmentDocumentDto,
  VisualAssetDto,
  VisualConceptsDto
} from '@/generated';

// AI Agent Process Interfaces
export interface ProcessData {
  brandName?: string;
  companyAnalysis?: CompanyAnalysisDto;
  storyOutline?: StoryOutlineDto;
  visualConcepts?: VisualConceptsDto;
  assets?: VisualAssetDto[];
  finalTreatment?: TreatmentDocumentDto;
}

export interface GenerationProcess {
  brandName: string;
  mode: 'interactive' | 'oneshot';
  startTime: number;
  currentStep: number;
  totalSteps: number;
  data: ProcessData;
  feedback?: Record<number, string>;
}

export interface ProcessStatus {
  brandName: string;
  mode: 'interactive' | 'oneshot';
  currentStep: number;
  totalSteps: number;
  progress: number;
  elapsedTime: number;
}

export interface ProcessCallbacks {
  onProgress?: (status: string, step: number, total: number) => void;
  onStepComplete?: (step: number, type: string, data: any) => void;
  onError?: (error: Error, step: number) => void;
  onComplete?: (data: ProcessData) => void;
}

// Step Response Interfaces
export interface StepResponse {
  step: number;
  data: ProcessData;
  complete?: boolean;
}

export interface GenerationOptions {
  [key: string]: any;
}

// Streaming and API Interfaces
export interface StreamChunkCallback {
  (chunk: string): void;
}

export type GenerationMode = 'interactive' | 'oneshot';
export type MessageRole = 'user' | 'assistant' | 'system';
export type AssetCategory = 'character' | 'set' | 'product' | 'scene';
export type StepType = 'research' | 'story' | 'visuals' | 'final';