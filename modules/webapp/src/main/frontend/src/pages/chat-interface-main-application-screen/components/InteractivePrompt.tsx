import React, {useState} from 'react';
import Icon from '../../../components/AppIcon';
import Button from '../../../components/ui/Button';

type StepType = 'research' | 'story' | 'characters' | 'visuals';

interface StepData {
  summary?: string;
  keyPoints?: string[];
}

interface StepInfo {
  title: string;
  icon: string;
  description: string;
  color: string;
}

interface InteractivePromptProps {
  step?: StepType;
  data?: StepData | string | null;
  onContinue?: (() => void) | null;
  onFeedback?: ((feedback: string) => void) | null;
  onRegenerate?: (() => void) | null;
}

const InteractivePrompt: React.FC<InteractivePromptProps> = ({
  step = 'research',
  data = null,
  onContinue = null,
  onFeedback = null,
  onRegenerate = null
}) => {
  const [feedbackText, setFeedbackText] = useState<string>('');
  const [showFeedbackInput, setShowFeedbackInput] = useState<boolean>(false);

  const getStepInfo = (): StepInfo => {
    switch (step) {
      case 'research':
        return {
          title: 'Company Research Complete',
          icon: 'Search',
          description: 'I\'ve analyzed the company information and brand identity.',
          color: 'var(--color-success)'
        };
      case 'story':
        return {
          title: 'Story Outline Generated',
          icon: 'BookOpen',
          description: 'Here\'s the cinematic story concept for your brand.',
          color: 'var(--color-accent)'
        };
      case 'characters':
        return {
          title: 'Character Designs Ready',
          icon: 'Users',
          description: 'Character concepts and costume designs have been created.',
          color: 'var(--color-warning)'
        };
      case 'visuals':
        return {
          title: 'Visual Assets Generated',
          icon: 'Image',
          description: 'Video stills and visual concepts are ready for review.',
          color: 'var(--color-success)'
        };
      default:
        return {
          title: 'Step Complete',
          icon: 'CheckCircle',
          description: 'This step has been completed.',
          color: 'var(--color-success)'
        };
    }
  };

  const handleFeedbackSubmit = () => {
    if (feedbackText?.trim() && onFeedback) {
      onFeedback(feedbackText?.trim());
      setFeedbackText('');
      setShowFeedbackInput(false);
    }
  };

  const stepInfo = getStepInfo();

  return (
      <div className="bg-card border border-border rounded-lg p-6 shadow-warm-md animate-scale-in">
        {/* Header */}
        <div className="flex items-center space-x-3 mb-4">
          <div className="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center">
            <Icon name={stepInfo?.icon} size={20} color={stepInfo?.color}/>
          </div>
          <div>
            <h3 className="text-lg font-heading font-semibold text-foreground">
              {stepInfo?.title}
            </h3>
            <p className="text-sm text-muted-foreground">
              {stepInfo?.description}
            </p>
          </div>
        </div>
        {/* Data Preview */}
        {data && (
            <div className="mb-6 p-4 bg-muted/30 rounded-lg border border-border/50">
              <div className="space-y-2">
                {typeof data === 'string' ? (
                    <p className="text-sm text-foreground leading-relaxed">{data}</p>
                ) : (
                    <div className="space-y-3">
                      {data?.summary && (
                          <div>
                            <h4 className="text-sm font-medium text-foreground mb-1">Summary:</h4>
                            <p className="text-sm text-muted-foreground">{data?.summary}</p>
                          </div>
                      )}
                      {data?.keyPoints && (
                          <div>
                            <h4 className="text-sm font-medium text-foreground mb-2">Key
                              Points:</h4>
                            <ul className="space-y-1">
                              {data?.keyPoints?.map((point, index) => (
                                  <li key={index}
                                      className="flex items-start space-x-2 text-sm text-muted-foreground">
                                    <Icon name="ChevronRight" size={14}
                                          className="mt-0.5 flex-shrink-0"/>
                                    <span>{point}</span>
                                  </li>
                              ))}
                            </ul>
                          </div>
                      )}
                    </div>
                )}
              </div>
            </div>
        )}
        {/* Feedback Input */}
        {showFeedbackInput && (
            <div className="mb-6 space-y-3 animate-fade-in">
              <div>
                <label className="block text-sm font-medium text-foreground mb-2">
                  Your Feedback
                </label>
                <textarea
                    value={feedbackText}
                    onChange={(e) => setFeedbackText(e?.target?.value)}
                    placeholder="Share your thoughts, suggestions, or specific changes you'd like to see..."
                    rows={4}
                    className="w-full px-3 py-2 bg-input border border-border rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent resize-none"
                />
              </div>

              <div className="flex items-center space-x-2">
                <Button
                    variant="default"
                    size="sm"
                    onClick={handleFeedbackSubmit}
                    disabled={!feedbackText?.trim()}
                    iconName="Send"
                    iconPosition="right"
                    className="bg-accent hover:bg-accent/90"
                >
                  Submit Feedback
                </Button>

                <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setShowFeedbackInput(false);
                      setFeedbackText('');
                    }}
                >
                  Cancel
                </Button>
              </div>
            </div>
        )}
        {/* Action Buttons */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            {!showFeedbackInput && (
                <Button
                    variant="outline"
                    size="default"
                    onClick={() => setShowFeedbackInput(true)}
                    iconName="MessageSquare"
                    iconPosition="left"
                >
                  Provide Feedback
                </Button>
            )}

            {onRegenerate && (
                <Button
                    variant="ghost"
                    size="default"
                    onClick={onRegenerate}
                    iconName="RefreshCw"
                    iconPosition="left"
                    className="text-muted-foreground hover:text-foreground"
                >
                  Regenerate
                </Button>
            )}
          </div>

          <div className="flex items-center space-x-2">
            <Button
                variant="default"
                size="default"
                onClick={onContinue ?? undefined}
                iconName="ArrowRight"
                iconPosition="right"
                className="bg-accent hover:bg-accent/90"
            >
              Continue to Next Step
            </Button>
          </div>
        </div>
        {/* Step Indicator */}
        <div className="mt-4 pt-4 border-t border-border">
          <div className="flex items-center justify-center space-x-2 text-xs text-muted-foreground">
            <Icon name="Clock" size={12}/>
            <span>Interactive mode • Step-by-step generation</span>
          </div>
        </div>
      </div>
  );
};

export default InteractivePrompt;