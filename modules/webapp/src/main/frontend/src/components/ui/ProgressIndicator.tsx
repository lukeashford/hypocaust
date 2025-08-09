import React from 'react';
import Icon from '../AppIcon';

interface ProgressIndicatorProps {
  currentStep?: number;
  totalSteps?: number;
  agentStatus?: string;
  onCancel?: (() => void) | null;
  stepLabels?: string[];
}

const ProgressIndicator: React.FC<ProgressIndicatorProps> = ({
  currentStep = 1,
  totalSteps = 4,
  agentStatus = 'idle',
  onCancel = null,
  stepLabels = ['Research', 'Analysis', 'Generation', 'Finalization']
}) => {
  const getStepStatus = (stepIndex: number): 'completed' | 'active' | 'pending' => {
    if (stepIndex < currentStep - 1) {
      return 'completed';
    }
    if (stepIndex === currentStep - 1) {
      return 'active';
    }
    return 'pending';
  };

  const getStatusIcon = (status: 'completed' | 'active' | 'pending') => {
    switch (status) {
      case 'completed':
        return <Icon name="Check" size={16} color="var(--color-success)" strokeWidth={2.5}/>;
      case 'active':
        return (
            <div
                className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin"/>
        );
      default:
        return <div className="w-4 h-4 rounded-full border-2 border-muted"/>;
    }
  };

  const getStatusMessage = (): string => {
    switch (agentStatus) {
      case 'researching':
        return 'Analyzing company and market data...';
      case 'analyzing':
        return 'Processing brand insights and positioning...';
      case 'generating':
        return 'Creating cinematic treatment concepts...';
      case 'finalizing':
        return 'Formatting and preparing final output...';
      case 'completed':
        return 'Treatment generation completed successfully';
      case 'error':
        return 'An error occurred during generation';
      default:
        return 'Ready to begin treatment generation';
    }
  };

  const isActive = agentStatus !== 'idle' && agentStatus !== 'completed' && agentStatus !== 'error';

  if (!isActive && agentStatus === 'idle') {
    return null;
  }

  return (
      <div className="bg-card border border-border rounded-lg p-4 shadow-warm-md animate-fade-in">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            <Icon name="Zap" size={18} color="var(--color-accent)"/>
            <h3 className="text-sm font-heading font-semibold text-foreground">
              AI Treatment Generation
            </h3>
          </div>

          {onCancel && isActive && (
              <button
                  onClick={onCancel}
                  className="text-muted-foreground hover:text-destructive transition-colors duration-200 p-1 rounded-md hover:bg-destructive/10"
                  title="Cancel generation"
              >
                <Icon name="X" size={16}/>
              </button>
          )}
        </div>
        {/* Progress Steps */}
        <div className="space-y-3 mb-4">
          {stepLabels?.map((label, index) => {
            const status = getStepStatus(index);
            return (
                <div key={index} className="flex items-center space-x-3">
                  <div className="flex-shrink-0">
                    {getStatusIcon(status)}
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                  <span className={`text-sm font-medium ${
                      status === 'completed'
                          ? 'text-success'
                          : status === 'active' ? 'text-accent' : 'text-muted-foreground'
                  }`}>
                    {label}
                  </span>

                      {status === 'completed' && (
                          <span className="text-xs text-success font-mono">
                      ✓ Complete
                    </span>
                      )}

                      {status === 'active' && (
                          <span className="text-xs text-accent font-mono animate-pulse">
                      Processing...
                    </span>
                      )}
                    </div>

                    {/* Progress bar for active step */}
                    {status === 'active' && (
                        <div className="mt-1 w-full bg-muted rounded-full h-1">
                          <div className="bg-accent h-1 rounded-full animate-pulse"
                               style={{width: '60%'}}/>
                        </div>
                    )}
                  </div>
                </div>
            );
          })}
        </div>
        {/* Status Message */}
        <div className="border-t border-border pt-3">
          <div className="flex items-center space-x-2">
            {agentStatus === 'error' ? (
                <Icon name="AlertCircle" size={16} color="var(--color-error)"/>
            ) : agentStatus === 'completed' ? (
                <Icon name="CheckCircle" size={16} color="var(--color-success)"/>
            ) : (
                <div
                    className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin"/>
            )}

            <p className={`text-sm font-mono ${
                agentStatus === 'error' ? 'text-error'
                    : agentStatus === 'completed' ? 'text-success' : 'text-muted-foreground'
            }`}>
              {getStatusMessage()}
            </p>
          </div>

          {/* Overall Progress */}
          <div className="mt-3">
            <div className="flex items-center justify-between text-xs text-muted-foreground mb-1">
              <span>Overall Progress</span>
              <span>{Math.round((currentStep / totalSteps) * 100)}%</span>
            </div>
            <div className="w-full bg-muted rounded-full h-2">
              <div
                  className="bg-gradient-to-r from-accent to-accent/80 h-2 rounded-full transition-all duration-500 ease-out"
                  style={{width: `${(currentStep / totalSteps) * 100}%`}}
              />
            </div>
          </div>
        </div>
        {/* Estimated time remaining */}
        {isActive && (
            <div className="mt-3 text-center">
              <p className="text-xs text-muted-foreground font-mono">
                Estimated time remaining: {Math.max(1, totalSteps - currentStep)} minutes
              </p>
            </div>
        )}
      </div>
  );
};

export default ProgressIndicator;