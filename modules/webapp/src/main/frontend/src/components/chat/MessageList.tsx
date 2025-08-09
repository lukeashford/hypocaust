import React from 'react';
import {useNavigate} from 'react-router-dom';
import Icon from '../AppIcon';
import ChatMessage from '../../pages/chat-interface-main-application-screen/components/ChatMessage';
import AssetPreview
  from '../../pages/chat-interface-main-application-screen/components/AssetPreview';
import InteractivePrompt
  from '../../pages/chat-interface-main-application-screen/components/InteractivePrompt';

interface MessageData {
  id: string;
  content: string;
  isUser: boolean;
  timestamp: string;
  isTyping?: boolean;
  treatmentData?: {
    title: string;
    pages: number;
    size: string;
  };
  storyContent?: string;
  assets?: any[];
  data?: {
    summary?: string;
    keyPoints?: string[];
    brandPersonality?: string;
    targetAudience?: string;
    visualStyle?: string;
  };
}

interface MessageListProps {
  messages: MessageData[];
  showInteractivePrompt: boolean;
  currentStep: number;
  currentStepData: any;
  processData?: any;
  finalTreatment?: any;
  generatedAssets?: any[];
  onDownloadTreatment: (format?: string) => void;
  onShareTreatment: (method: string) => void;
  onViewPDF: () => void;
  onInteractiveContinue: () => void;
  onInteractiveFeedback: (feedback: any) => void;
  onInteractiveRegenerate: () => void;
  addMessage: (content: string, isUser: boolean) => void;
  messagesEndRef: React.RefObject<HTMLDivElement>;
}

const MessageList: React.FC<MessageListProps> = ({
  messages,
  showInteractivePrompt,
  currentStep,
  currentStepData,
  processData,
  finalTreatment,
  generatedAssets,
  onDownloadTreatment,
  onShareTreatment,
  onViewPDF,
  onInteractiveContinue,
  onInteractiveFeedback,
  onInteractiveRegenerate,
  addMessage,
  messagesEndRef
}) => {
  const navigate = useNavigate();

  const handleDownloadTreatment = (format = 'pdf') => {
    // Simulate download with real treatment data
    const treatmentContent = processData?.finalTreatment || finalTreatment?.fullData;

    if (treatmentContent) {
      // In a real implementation, this would generate a PDF from the treatment data
      const link = document.createElement('a');
      const blob = new Blob([JSON.stringify(treatmentContent, null, 2)],
          {type: 'application/json'});
      link.href = URL.createObjectURL(blob);
      link.download = `${treatmentContent?.brandName || 'Treatment'}-${Date.now()}.${format
      === 'pdf' ? 'json' : format}`;
      link?.click();
      URL.revokeObjectURL(link?.href);

      addMessage(`Treatment downloaded as ${format?.toUpperCase()} successfully!`, false);
    } else {
      addMessage("No treatment data available for download.", false);
    }
  };

  const handleShareTreatment = (method: string) => {
    addMessage(`Treatment shared via ${method} successfully!`, false);
  };

  const handleViewPDF = () => {
    // Pass treatment data to PDF preview screen
    navigate('/pdf-preview-and-download-screen', {
      state: {
        treatmentData: processData?.finalTreatment || finalTreatment?.fullData,
        assets: generatedAssets
      }
    });
  };

  const renderMessageContent = (message: MessageData) => {
    if (message?.treatmentData) {
      return (
          <div className="space-y-4">
            <p className="text-sm leading-relaxed text-foreground">{message?.content}</p>
            <div className="bg-success/10 border border-success/20 rounded-lg p-4">
              <div className="flex items-center space-x-3 mb-3">
                <Icon name="FileText" size={20} color="var(--color-success)"/>
                <div>
                  <h4 className="text-sm font-semibold text-success">{message?.treatmentData?.title}</h4>
                  <p className="text-xs text-success/80">{message?.treatmentData?.pages} pages
                    • {message?.treatmentData?.size}</p>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                <button
                    onClick={onViewPDF}
                    className="flex items-center space-x-2 px-3 py-2 bg-success text-success-foreground rounded-md text-sm font-medium hover:bg-success/90 transition-colors duration-200"
                >
                  <Icon name="Eye" size={16}/>
                  <span>Preview PDF</span>
                </button>

                <button
                    onClick={() => onDownloadTreatment('pdf')}
                    className="flex items-center space-x-2 px-3 py-2 bg-accent text-accent-foreground rounded-md text-sm font-medium hover:bg-accent/90 transition-colors duration-200"
                >
                  <Icon name="Download" size={16}/>
                  <span>Download</span>
                </button>
              </div>
            </div>
          </div>
      );
    }

    if (message?.storyContent) {
      return (
          <div className="space-y-4">
            <p className="text-sm leading-relaxed text-foreground">{message?.content}</p>
            <div className="bg-card border border-border rounded-lg p-4">
              <div className="flex items-center space-x-2 mb-3">
                <Icon name="BookOpen" size={16} color="var(--color-accent)"/>
                <h4 className="text-sm font-semibold text-foreground">Cinematic Story Outline</h4>
              </div>
              <div
                  className="text-sm text-muted-foreground font-mono whitespace-pre-line leading-relaxed max-h-96 overflow-y-auto">
                {message?.storyContent}
              </div>
            </div>
          </div>
      );
    }

    if (message?.assets) {
      return (
          <div className="space-y-4">
            <p className="text-sm leading-relaxed text-foreground">{message?.content}</p>
            <AssetPreview
                assets={message?.assets}
                onAssetClick={(asset) => console.log('Asset clicked:', asset)}
                onDownload={(assets) => console.log('Download assets:', assets)}
            />
          </div>
      );
    }

    if (message?.data && typeof message?.data === 'object') {
      return (
          <div className="space-y-4">
            <p className="text-sm leading-relaxed text-foreground">{message?.content}</p>
            <div className="bg-muted/30 rounded-lg p-4 border border-border/50">
              <div className="space-y-3">
                {message?.data?.summary && (
                    <div>
                      <h4 className="text-sm font-medium text-foreground mb-1">Summary:</h4>
                      <p className="text-sm text-muted-foreground">{message?.data?.summary}</p>
                    </div>
                )}
                {message?.data?.keyPoints && (
                    <div>
                      <h4 className="text-sm font-medium text-foreground mb-2">Key Points:</h4>
                      <ul className="space-y-1">
                        {message?.data?.keyPoints?.map((point, index) => (
                            <li key={index}
                                className="flex items-start space-x-2 text-sm text-muted-foreground">
                              <Icon name="ChevronRight" size={14} className="mt-0.5 flex-shrink-0"/>
                              <span>{point}</span>
                            </li>
                        ))}
                      </ul>
                    </div>
                )}
                {message?.data?.brandPersonality && (
                    <div>
                      <h4 className="text-sm font-medium text-foreground mb-1">Brand
                        Personality:</h4>
                      <p className="text-sm text-muted-foreground">{message?.data?.brandPersonality}</p>
                    </div>
                )}
                {message?.data?.targetAudience && (
                    <div>
                      <h4 className="text-sm font-medium text-foreground mb-1">Target Audience:</h4>
                      <p className="text-sm text-muted-foreground">{message?.data?.targetAudience}</p>
                    </div>
                )}
                {message?.data?.visualStyle && (
                    <div>
                      <h4 className="text-sm font-medium text-foreground mb-1">Visual Style:</h4>
                      <p className="text-sm text-muted-foreground">{message?.data?.visualStyle}</p>
                    </div>
                )}
              </div>
            </div>
          </div>
      );
    }

    return message?.content;
  };

  return (
      <div className="space-y-6">
        {messages?.map((message) => (
            <ChatMessage
                key={message?.id}
                message={renderMessageContent(message)}
                isUser={message?.isUser}
                timestamp={message?.timestamp}
                isTyping={message?.isTyping}
            />
        ))}

        {/* Interactive Prompt */}
        {showInteractivePrompt && (
            <div className="mt-6">
              <InteractivePrompt
                  step={currentStep === 1 ? 'research' : currentStep === 2 ? 'story'
                      : currentStep === 3 ? 'visuals' : 'final'}
                  data={currentStepData}
                  onContinue={onInteractiveContinue}
                  onFeedback={onInteractiveFeedback}
                  onRegenerate={onInteractiveRegenerate}
              />
            </div>
        )}

        <div ref={messagesEndRef}/>
      </div>
  );
};

export default MessageList;