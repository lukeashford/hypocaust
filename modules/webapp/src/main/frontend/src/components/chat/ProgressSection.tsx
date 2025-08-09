import React from 'react';
import ProgressIndicator from '../ui/ProgressIndicator';

interface ProgressSectionProps {
  isProcessing: boolean;
  agentStatus: string;
  currentStep: number;
  totalSteps: number;
  stepLabels: string[];
  aiAgentService?: any;
  setIsProcessing: (processing: boolean) => void;
  setAgentStatus: (status: string) => void;
  addMessage: (content: string, isUser: boolean) => void;
}

const ProgressSection: React.FC<ProgressSectionProps> = ({
  isProcessing,
  agentStatus,
  currentStep,
  totalSteps,
  stepLabels,
  aiAgentService,
  setIsProcessing,
  setAgentStatus,
  addMessage
}) => {
  const handleCancel = () => {
    aiAgentService?.cancelGeneration();
    setIsProcessing(false);
    setAgentStatus('idle');
    addMessage("Generation cancelled by user.", false);
  };

  // Only show progress section when processing or agent is active
  if (!isProcessing && agentStatus === 'idle') {
    return null;
  }

  return (
      <div className="mb-6">
        <ProgressIndicator
            currentStep={currentStep}
            totalSteps={totalSteps}
            agentStatus={agentStatus}
            onCancel={handleCancel}
            stepLabels={stepLabels}
        />
      </div>
  );
};

export default ProgressSection;