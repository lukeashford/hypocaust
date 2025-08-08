import openaiService from './openaiService.js';

/**
 * AI Agent Service for orchestrating the complete treatment generation process
 * Manages the step-by-step workflow with error handling and progress tracking
 */

class AIAgentService {
  constructor() {
    this.currentProcess = null;
    this.stepCallbacks = {};
  }

  /**
   * Registers callbacks for different steps and events
   * @param {object} callbacks - Object containing callback functions
   */
  setCallbacks(callbacks) {
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
   * @param {string} brandName - Name of the brand to analyze
   * @param {string} mode - Generation mode ('interactive' or 'oneshot')
   * @param {object} options - Additional options for generation
   * @returns {Promise<object>} Complete treatment data
   */
  async generateTreatment(brandName, mode = 'interactive', options = {}) {
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
      this.stepCallbacks?.onError?.(error, this.currentProcess?.currentStep || 1);
      throw error;
    }
  }

  /**
   * Continues to the next step in interactive mode
   * @param {object} feedback - Optional user feedback
   * @returns {Promise<object>} Next step data or complete treatment
   */
  async continueToNextStep(feedback = null) {
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
      this.stepCallbacks?.onError?.(error, this.currentProcess?.currentStep || 1);
      throw error;
    }
  }

  /**
   * Step 1: Company Research and Analysis
   */
  async executeStep1_CompanyResearch(brandName) {
    this.stepCallbacks?.onProgress?.('researching', 1, 4);
    this.currentProcess.currentStep = 1;

    try {
      const companyData = await openaiService?.analyzeCompany(brandName);

      this.currentProcess.data.companyAnalysis = companyData;
      this.currentProcess.data.brandName = brandName;

      this.stepCallbacks?.onStepComplete?.(1, 'research', companyData);
    } catch (error) {
      console.error('Step 1 error:', error);
      throw new Error(`Company research failed: ${error.message}`);
    }
  }

  /**
   * Step 2: Story Outline Generation
   */
  async executeStep2_StoryGeneration() {
    this.stepCallbacks?.onProgress?.('analyzing', 2, 4);
    this.currentProcess.currentStep = 2;

    try {
      const {brandName, companyAnalysis} = this.currentProcess?.data;
      const storyData = await openaiService?.generateStoryOutline(brandName, companyAnalysis);

      this.currentProcess.data.storyOutline = storyData;

      this.stepCallbacks?.onStepComplete?.(2, 'story', storyData);
    } catch (error) {
      console.error('Step 2 error:', error);
      throw new Error(`Story generation failed: ${error.message}`);
    }
  }

  /**
   * Step 3: Visual Concepts and Asset Generation
   */
  async executeStep3_VisualGeneration() {
    this.stepCallbacks?.onProgress?.('generating', 3, 4);
    this.currentProcess.currentStep = 3;

    try {
      const {brandName, companyAnalysis, storyOutline} = this.currentProcess?.data;

      // Generate visual concepts
      const visualConcepts = await openaiService?.generateVisualConcepts(
          brandName,
          storyOutline,
          companyAnalysis
      );

      this.currentProcess.data.visualConcepts = visualConcepts;

      // Generate visual assets
      const assets = await openaiService?.generateVisualAssets(
          brandName,
          visualConcepts,
          storyOutline
      );

      this.currentProcess.data.assets = assets;

      this.stepCallbacks?.onStepComplete?.(3, 'visuals', {visualConcepts, assets});
    } catch (error) {
      console.error('Step 3 error:', error);
      throw new Error(`Visual generation failed: ${error.message}`);
    }
  }

  /**
   * Step 4: Final Treatment Document Generation
   */
  async executeStep4_FinalTreatment() {
    this.stepCallbacks?.onProgress?.('finalizing', 4, 4);
    this.currentProcess.currentStep = 4;

    try {
      const {
        brandName,
        companyAnalysis,
        storyOutline,
        visualConcepts,
        assets
      } = this.currentProcess?.data;

      const treatmentDocument = await openaiService?.generateTreatmentDocument(
          brandName,
          companyAnalysis,
          storyOutline,
          visualConcepts,
          assets
      );

      this.currentProcess.data.finalTreatment = treatmentDocument;

      this.stepCallbacks?.onStepComplete?.(4, 'final', treatmentDocument);
      this.stepCallbacks?.onComplete?.(this.currentProcess?.data);
    } catch (error) {
      console.error('Step 4 error:', error);
      throw new Error(`Final treatment generation failed: ${error.message}`);
    }
  }

  /**
   * Applies user feedback to refine the current step
   * @param {number} step - Current step number
   * @param {string} feedback - User feedback text
   */
  async applyFeedback(step, feedback) {
    // This would integrate feedback into the next generation cycle
    // For now, we'll store it for context in future API calls
    if (!this.currentProcess?.feedback) {
      this.currentProcess.feedback = {};
    }
    this.currentProcess.feedback[step] = feedback;
  }

  /**
   * Regenerates the current step with new creative direction
   * @param {number} step - Step to regenerate
   * @returns {Promise<object>} Regenerated step data
   */
  async regenerateStep(step) {
    this.currentProcess.currentStep = step;

    switch (step) {
      case 1:
        await this.executeStep1_CompanyResearch(this.currentProcess?.brandName);
        return this.currentProcess?.data?.companyAnalysis;
      case 2:
        await this.executeStep2_StoryGeneration();
        return this.currentProcess?.data?.storyOutline;
      case 3:
        await this.executeStep3_VisualGeneration();
        return {
          visualConcepts: this.currentProcess?.data?.visualConcepts,
          assets: this.currentProcess?.data?.assets
        };
      case 4:
        await this.executeStep4_FinalTreatment();
        return this.currentProcess?.data?.finalTreatment;
      default:
        throw new Error('Invalid step for regeneration');
    }
  }

  /**
   * Cancels the current generation process
   */
  cancelGeneration() {
    if (this.currentProcess) {
      this.currentProcess = null;
      this.stepCallbacks?.onError?.(new Error('Generation cancelled by user'), 0);
    }
  }

  /**
   * Gets the current process status
   * @returns {object} Current process information
   */
  getProcessStatus() {
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