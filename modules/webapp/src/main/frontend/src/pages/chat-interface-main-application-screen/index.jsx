import React, {useEffect, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import Header from '../../components/ui/Header';
import ProgressIndicator from '../../components/ui/ProgressIndicator';
import ContextualActionPanel from '../../components/ui/ContextualActionPanel';
import ChatMessage from './components/ChatMessage';
import MessageInput from './components/MessageInput';
import InteractivePrompt from './components/InteractivePrompt';
import AssetPreview from './components/AssetPreview';
import WelcomeScreen from './components/WelcomeScreen';
import Icon from '../../components/AppIcon';
import aiAgentService from '../../services/aiAgentService';

const ChatInterface = () => {
  const navigate = useNavigate();
  const messagesEndRef = useRef(null);

  // Core state
  const [messages, setMessages] = useState([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [mode, setMode] = useState('interactive'); // 'interactive' or 'oneshot'
  const [showWelcome, setShowWelcome] = useState(true);

  // AI Agent state
  const [currentStep, setCurrentStep] = useState(1);
  const [agentStatus, setAgentStatus] = useState('idle');
  const [showInteractivePrompt, setShowInteractivePrompt] = useState(false);
  const [currentStepData, setCurrentStepData] = useState(null);
  const [error, setError] = useState(null);

  // Generated content state
  const [generatedAssets, setGeneratedAssets] = useState([]);
  const [finalTreatment, setFinalTreatment] = useState(null);
  const [processData, setProcessData] = useState(null);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    messagesEndRef?.current?.scrollIntoView({behavior: 'smooth'});
  }, [messages]);

  // Setup AI Agent callbacks
  useEffect(() => {
    aiAgentService?.setCallbacks({
      onProgress: (status, step, total) => {
        setAgentStatus(status);
        setCurrentStep(step);
        setIsProcessing(true);
        setError(null);
      },
      onStepComplete: (step, type, data) => {
        handleStepComplete(step, type, data);
      },
      onError: (error, step) => {
        console.error('AI Agent Error:', error);
        setError(error?.message || 'An error occurred during generation');
        setIsProcessing(false);
        setAgentStatus('idle');
        addMessage(`Error in step ${step}: ${error?.message || 'Unknown error occurred'}`, false);
      },
      onComplete: (completeData) => {
        setProcessData(completeData);
        setIsProcessing(false);
        setAgentStatus('completed');
      }
    });
  }, []);

  const handleStepComplete = (step, type, data) => {
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
  };

  const addMessage = (content, isUser = false, timestamp = null, isTyping = false, data = null,
      storyContent = null, treatmentData = null) => {
    const newMessage = {
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

  const handleSendMessage = async (message) => {
    if (showWelcome) {
      setShowWelcome(false);
    }

    // Add user message
    addMessage(message, true);

    // Reset error state
    setError(null);

    try {
      // Start AI processing
      await aiAgentService?.generateTreatment(message, mode);
    } catch (error) {
      console.error('Generation error:', error);
      setError(error?.message || 'Failed to generate treatment');
      addMessage(`Sorry, I encountered an error: ${error?.message || 'Unknown error'}`, false);
      setIsProcessing(false);
      setAgentStatus('idle');
    }
  };

  const handleModeChange = (newMode) => {
    setMode(newMode);
  };

  const handleGetStarted = (brandName = null) => {
    setShowWelcome(false);
    if (brandName) {
      handleSendMessage(brandName);
    }
  };

  const handleModeSelect = (selectedMode) => {
    setMode(selectedMode);
    setShowWelcome(false);
  };

  const handleInteractiveContinue = async () => {
    try {
      setShowInteractivePrompt(false);
      const result = await aiAgentService?.continueToNextStep();

      if (result?.complete) {
        // Final step completed
        setProcessData(result?.data);
      }
    } catch (error) {
      console.error('Continue error:', error);
      setError(error?.message || 'Failed to continue to next step');
      addMessage(`Error continuing: ${error?.message || 'Unknown error'}`, false);
      setIsProcessing(false);
    }
  };

  const handleInteractiveFeedback = async (feedback) => {
    addMessage(feedback, true);
    addMessage(
        "Thank you for the feedback! I'll incorporate your suggestions and continue with the next step.",
        false);

    try {
      const result = await aiAgentService?.continueToNextStep(feedback);

      if (result?.complete) {
        setProcessData(result?.data);
      }
    } catch (error) {
      console.error('Feedback error:', error);
      setError(error?.message || 'Failed to apply feedback');
      addMessage(`Error applying feedback: ${error?.message || 'Unknown error'}`, false);
      setIsProcessing(false);
    }
  };

  const handleInteractiveRegenerate = async () => {
    addMessage("Regenerating this step with fresh creative direction...", false);
    setShowInteractivePrompt(false);

    try {
      await aiAgentService?.regenerateStep(currentStep);
    } catch (error) {
      console.error('Regenerate error:', error);
      setError(error?.message || 'Failed to regenerate step');
      addMessage(`Error regenerating: ${error?.message || 'Unknown error'}`, false);
      setIsProcessing(false);
    }
  };

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

  const handleShareTreatment = (method) => {
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

  const renderMessageContent = (message) => {
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
                    onClick={handleViewPDF}
                    className="flex items-center space-x-2 px-3 py-2 bg-success text-success-foreground rounded-md text-sm font-medium hover:bg-success/90 transition-colors duration-200"
                >
                  <Icon name="Eye" size={16}/>
                  <span>Preview PDF</span>
                </button>

                <button
                    onClick={() => handleDownloadTreatment('pdf')}
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
      <div className="min-h-screen bg-background">
        <Header/>
        <div className="flex flex-col lg:flex-row min-h-[calc(100vh-4rem)]">
          {/* Main Chat Area */}
          <div className="flex-1 flex flex-col">
            {showWelcome ? (
                <WelcomeScreen
                    onGetStarted={handleGetStarted}
                    onModeSelect={handleModeSelect}
                />
            ) : (
                <>
                  {/* Messages Area */}
                  <div className="flex-1 overflow-y-auto">
                    <div className="max-w-4xl mx-auto px-4 py-6">
                      {/* Error Display */}
                      {error && (
                          <div
                              className="mb-6 p-4 bg-destructive/10 border border-destructive/20 rounded-lg">
                            <div className="flex items-center space-x-2">
                              <Icon name="AlertCircle" size={16} color="var(--color-destructive)"/>
                              <h4 className="text-sm font-medium text-destructive">Generation
                                Error</h4>
                            </div>
                            <p className="text-sm text-destructive/80 mt-1">{error}</p>
                          </div>
                      )}

                      {/* Progress Indicator */}
                      {(isProcessing || agentStatus !== 'idle') && (
                          <div className="mb-6">
                            <ProgressIndicator
                                currentStep={currentStep}
                                totalSteps={4}
                                agentStatus={agentStatus}
                                onCancel={() => {
                                  aiAgentService?.cancelGeneration();
                                  setIsProcessing(false);
                                  setAgentStatus('idle');
                                  addMessage("Generation cancelled by user.", false);
                                }}
                                stepLabels={['Research', 'Story Creation', 'Visual Assets',
                                  'Final Treatment']}
                            />
                          </div>
                      )}

                      {/* Chat Messages */}
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
                                  onContinue={handleInteractiveContinue}
                                  onFeedback={handleInteractiveFeedback}
                                  onRegenerate={handleInteractiveRegenerate}
                              />
                            </div>
                        )}
                      </div>

                      <div ref={messagesEndRef}/>
                    </div>
                  </div>

                  {/* Message Input */}
                  <MessageInput
                      onSendMessage={handleSendMessage}
                      isProcessing={isProcessing}
                      mode={mode}
                      onModeChange={handleModeChange}
                      placeholder={messages?.length === 0
                          ? "Enter your brand name to get started..."
                          : "Ask questions or provide feedback..."}
                  />
                </>
            )}
          </div>

          {/* Sidebar - Action Panel */}
          {finalTreatment && (
              <div className="lg:w-80 border-l border-border bg-background/50">
                <div className="p-4">
                  <ContextualActionPanel
                      pdfData={finalTreatment}
                      onDownload={handleDownloadTreatment}
                      onShare={handleShareTreatment}
                      onRegenerate={() => {
                        setFinalTreatment(null);
                        setMessages([]);
                        setCurrentStep(1);
                        setAgentStatus('idle');
                        setGeneratedAssets([]);
                        setProcessData(null);
                        addMessage("Starting fresh treatment generation...", false);
                      }}
                      isVisible={true}
                  />
                </div>
              </div>
          )}
        </div>
      </div>
  );
};

export default ChatInterface;