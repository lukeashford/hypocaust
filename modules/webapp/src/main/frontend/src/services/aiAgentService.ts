import openaiService from './openaiService';
import type {
  CompanyAnalysis,
  GenerationMode,
  GenerationOptions,
  GenerationProcess,
  ProcessCallbacks,
  ProcessData,
  ProcessStatus,
  StepResponse,
  StoryOutline,
  TreatmentDocument,
  VisualAsset,
  VisualConcepts
} from '../types/interfaces';

/**
 * AI Agent Service for orchestrating the complete treatment generation process
 * Manages the step-by-step workflow with error handling and progress tracking
 */

class AIAgentService {
  private currentProcess: GenerationProcess | null = null;
  private stepCallbacks: ProcessCallbacks = {};

  /**
   * Registers callbacks for different steps and events
   * @param callbacks - Object containing callback functions
   */
  setCallbacks(callbacks: ProcessCallbacks): void {
    this.stepCallbacks = {
      onProgress: callbacks?.onProgress || (() => {
      }),
      onStepComplete: callbacks?.onStepComplete || (() => {
      }),
      onError: callbacks?.onError || (() => {
      }),
      onComplete: callbacks?.onComplete || (() => {
      }),
      ...callbacks
    };
  }

  /**
   * Starts the treatment generation process
   * @param brandName - Name of the brand to analyze
   * @param mode - Generation mode ('interactive' or 'oneshot')
   * @param options - Additional options for generation
   * @returns Complete treatment data
   */
  async generateTreatment(
      brandName: string,
      mode: GenerationMode = 'interactive',
      options: GenerationOptions = {}
  ): Promise<StepResponse | ProcessData> {
    try {
      this.currentProcess = {
        brandName,
        mode,
        startTime: Date.now(),
        currentStep: 1,
        totalSteps: 4,
        data: {}
      };

      this.stepCallbacks?.onProgress?.('starting', 1, 4);

      // Step 1: Company Research
      await this.executeStep1_CompanyResearch(brandName);

      if (mode === 'interactive') {
        return {step: 1, data: this.currentProcess?.data};
      }

      // Continue with remaining steps for one-shot mode
      await this.executeStep2_StoryGeneration();
      await this.executeStep3_VisualGeneration();
      await this.executeStep4_FinalTreatment();

      return this.currentProcess?.data;
    } catch (error) {
      this.stepCallbacks?.onError?.(error as Error, this.currentProcess?.currentStep || 1);
      throw error;
    }
  }

  /**
   * Continues to the next step in interactive mode
   * @param feedback - Optional user feedback
   * @returns Next step data or complete treatment
   */
  async continueToNextStep(feedback: string | null = null): Promise<StepResponse> {
    if (!this.currentProcess) {
      throw new Error('No active generation process');
    }

    try {
      const {currentStep} = this.currentProcess;

      // Apply feedback if provided
      if (feedback) {
        await this.applyFeedback(currentStep, feedback);
      }

      switch (currentStep) {
        case 1:
          await this.executeStep2_StoryGeneration();
          return {step: 2, data: this.currentProcess?.data};
        case 2:
          await this.executeStep3_VisualGeneration();
          return {step: 3, data: this.currentProcess?.data};
        case 3:
          await this.executeStep4_FinalTreatment();
          return {step: 4, data: this.currentProcess?.data, complete: true};
        default:
          throw new Error('Invalid step progression');
      }
    } catch (error) {
      this.stepCallbacks?.onError?.(error as Error, this.currentProcess?.currentStep || 1);
      throw error;
    }
  }

  /**
   * Step 1: Company Research and Analysis
   */
  private async executeStep1_CompanyResearch(brandName: string): Promise<void> {
    this.stepCallbacks?.onProgress?.('researching', 1, 4);
    if (this.currentProcess) {
      this.currentProcess.currentStep = 1;
    }

    try {
      const companyData: CompanyAnalysis = await openaiService?.analyzeCompany(brandName);

      if (this.currentProcess) {
        this.currentProcess.data.companyAnalysis = companyData;
        this.currentProcess.data.brandName = brandName;
      }

      this.stepCallbacks?.onStepComplete?.(1, 'research', companyData);
    } catch (error) {
      console.error('Step 1 error:', error);
      throw new Error(
          `Company research failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  /**
   * Step 2: Story Outline Generation
   */
  private async executeStep2_StoryGeneration(): Promise<void> {
    this.stepCallbacks?.onProgress?.('analyzing', 2, 4);
    if (this.currentProcess) {
      this.currentProcess.currentStep = 2;
    }

    try {
      const {brandName, companyAnalysis} = this.currentProcess?.data || {};
      if (!brandName || !companyAnalysis) {
        throw new Error('Missing required data for story generation');
      }

      const storyData: StoryOutline = await openaiService?.generateStoryOutline(brandName,
          companyAnalysis);

      if (this.currentProcess) {
        this.currentProcess.data.storyOutline = storyData;
      }

      this.stepCallbacks?.onStepComplete?.(2, 'story', storyData);
    } catch (error) {
      console.error('Step 2 error:', error);
      throw new Error(
          `Story generation failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  /**
   * Step 3: Visual Concepts and Asset Generation
   */
  private async executeStep3_VisualGeneration(): Promise<void> {
    this.stepCallbacks?.onProgress?.('generating', 3, 4);
    if (this.currentProcess) {
      this.currentProcess.currentStep = 3;
    }

    try {
      const {brandName, companyAnalysis, storyOutline} = this.currentProcess?.data || {};
      if (!brandName || !companyAnalysis || !storyOutline) {
        throw new Error('Missing required data for visual generation');
      }

      // Generate visual concepts
      const visualConcepts: VisualConcepts = await openaiService?.generateVisualConcepts(
          brandName,
          storyOutline,
          companyAnalysis
      );

      if (this.currentProcess) {
        this.currentProcess.data.visualConcepts = visualConcepts;
      }

      // Generate visual assets
      const assets: VisualAsset[] = await openaiService?.generateVisualAssets(
          brandName,
          visualConcepts,
          storyOutline
      );

      if (this.currentProcess) {
        this.currentProcess.data.assets = assets;
      }

      this.stepCallbacks?.onStepComplete?.(3, 'visuals', {visualConcepts, assets});
    } catch (error) {
      console.error('Step 3 error:', error);
      throw new Error(
          `Visual generation failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  /**
   * Step 4: Final Treatment Document Generation
   */
  private async executeStep4_FinalTreatment(): Promise<void> {
    this.stepCallbacks?.onProgress?.('finalizing', 4, 4);
    if (this.currentProcess) {
      this.currentProcess.currentStep = 4;
    }

    try {
      const {
        brandName,
        companyAnalysis,
        storyOutline,
        visualConcepts,
        assets
      } = this.currentProcess?.data || {};

      if (!brandName || !companyAnalysis || !storyOutline || !visualConcepts || !assets) {
        throw new Error('Missing required data for treatment generation');
      }

      const treatmentDocument: TreatmentDocument = await openaiService?.generateTreatmentDocument(
          brandName,
          companyAnalysis,
          storyOutline,
          visualConcepts,
          assets
      );

      if (this.currentProcess) {
        this.currentProcess.data.finalTreatment = treatmentDocument;
      }

      this.stepCallbacks?.onStepComplete?.(4, 'final', treatmentDocument);
      this.stepCallbacks?.onComplete?.(this.currentProcess?.data);
    } catch (error) {
      console.error('Step 4 error:', error);
      throw new Error(`Final treatment generation failed: ${error instanceof Error ? error.message
          : 'Unknown error'}`);
    }
  }

  /**
   * Applies user feedback to refine the current step
   * @param step - Current step number
   * @param feedback - User feedback text
   */
  private async applyFeedback(step: number, feedback: string): Promise<void> {
    // This would integrate feedback into the next generation cycle
    // For now, we'll store it for context in future API calls
    if (!this.currentProcess?.feedback) {
      if (this.currentProcess) {
        this.currentProcess.feedback = {};
      }
    }
    if (this.currentProcess?.feedback) {
      this.currentProcess.feedback[step] = feedback;
    }
  }

  /**
   * Regenerates the current step with new creative direction
   * @param step - Step to regenerate
   * @returns Regenerated step data
   */
  async regenerateStep(step: number): Promise<CompanyAnalysis | StoryOutline | {
    visualConcepts: VisualConcepts;
    assets: VisualAsset[]
  } | TreatmentDocument> {
    if (this.currentProcess) {
      this.currentProcess.currentStep = step;
    }

    switch (step) {
      case 1:
        if (!this.currentProcess?.data.brandName) {
          throw new Error('Brand name required for step regeneration');
        }
        await this.executeStep1_CompanyResearch(this.currentProcess.data.brandName);
        return this.currentProcess?.data?.companyAnalysis!;
      case 2:
        await this.executeStep2_StoryGeneration();
        return this.currentProcess?.data?.storyOutline!;
      case 3:
        await this.executeStep3_VisualGeneration();
        return {
          visualConcepts: this.currentProcess?.data?.visualConcepts!,
          assets: this.currentProcess?.data?.assets!
        };
      case 4:
        await this.executeStep4_FinalTreatment();
        return this.currentProcess?.data?.finalTreatment!;
      default:
        throw new Error('Invalid step for regeneration');
    }
  }

  /**
   * Cancels the current generation process
   */
  cancelGeneration(): void {
    if (this.currentProcess) {
      this.currentProcess = null;
      this.stepCallbacks?.onError?.(new Error('Generation cancelled by user'), 0);
    }
  }

  /**
   * Gets the current process status
   * @returns Current process information
   */
  getProcessStatus(): ProcessStatus | null {
    return this.currentProcess ? {
      brandName: this.currentProcess?.brandName,
      mode: this.currentProcess?.mode,
      currentStep: this.currentProcess?.currentStep,
      totalSteps: this.currentProcess?.totalSteps,
      progress: (this.currentProcess?.currentStep / this.currentProcess?.totalSteps) * 100,
      elapsedTime: Date.now() - this.currentProcess?.startTime
    } : null;
  }
}

// Export singleton instance
const aiAgentService = new AIAgentService();
export default aiAgentService;