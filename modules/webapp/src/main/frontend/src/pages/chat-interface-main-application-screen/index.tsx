import React, {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import Header from 'components/ui/Header';
import ContextualActionPanel from 'components/ui/ContextualActionPanel';
import MessageInput from './components/MessageInput';
import WelcomeScreen from './components/WelcomeScreen.tsx';
import ChatErrorDisplay from 'components/chat/ChatErrorDisplay';
import MessageList from 'components/chat/MessageList';
import ProgressSection from 'components/chat/ProgressSection';
import {useChatState} from 'hooks/useChatState';
import {useAIAgent} from 'hooks/useAIAgent';
import {useScrollToBottom} from 'hooks/useScrollToBottom';
import {createTreatmentHandlers} from 'utils/treatmentUtils';

type GenerationMode = 'interactive' | 'oneshot';

const ChatInterface: React.FC = () => {
  const navigate = useNavigate();

  // Chat state from custom hook
  const {
    messages,
    isProcessing,
    error,
    showWelcome,
    generatedAssets,
    finalTreatment,
    processData,
    setMessages,
    setIsProcessing,
    setError,
    setShowWelcome,
    setGeneratedAssets,
    setFinalTreatment,
    setProcessData,
    addMessage
  } = useChatState();

  // Other component state (not extracted)
  const [mode, setMode] = useState<GenerationMode>('interactive');

  // AI Agent state and callbacks from custom hook
  const {
    currentStep,
    agentStatus,
    currentStepData,
    showInteractivePrompt,
    setCurrentStep,
    setAgentStatus,
    handleSendMessage,
    handleInteractiveContinue,
    handleInteractiveFeedback,
    handleInteractiveRegenerate
  } = useAIAgent({
    mode,
    addMessage,
    setIsProcessing,
    setError,
    setShowWelcome,
    setGeneratedAssets,
    setFinalTreatment,
    setProcessData
  });

  // Auto-scroll to bottom when new messages arrive
  const {messagesEndRef} = useScrollToBottom([messages]);

  const handleModeChange = (newMode: GenerationMode) => {
    setMode(newMode);
  };

  const handleGetStarted = (brandName: string | null = null) => {
    setShowWelcome(false);
    if (brandName) {
      handleSendMessage(brandName);
    }
  };

  const handleModeSelect = (selectedMode: GenerationMode) => {
    setMode(selectedMode);
    setShowWelcome(false);
  };

  // Create treatment handlers using the utility function
  const {handleDownloadTreatment, handleShareTreatment, handleViewPDF} = createTreatmentHandlers(
      navigate,
      addMessage,
      processData,
      finalTreatment,
      generatedAssets
  );

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
                  {/* Fixed Progress Section at Top */}
                  <div className="flex-shrink-0 bg-background border-b border-border">
                    <div className="max-w-4xl mx-auto px-4 py-4">
                      <ChatErrorDisplay error={error}/>
                      <ProgressSection
                          isProcessing={isProcessing}
                          agentStatus={agentStatus}
                          currentStep={currentStep}
                          totalSteps={4}
                          stepLabels={['Research', 'Story Creation', 'Visual Assets',
                            'Final Treatment']}
                          setIsProcessing={setIsProcessing}
                          setAgentStatus={setAgentStatus}
                          addMessage={addMessage}
                      />
                    </div>
                  </div>

                  {/* Scrollable Messages Area */}
                  <div className="flex-1 overflow-y-auto">
                    <div className="max-w-4xl mx-auto px-4 py-6">
                      <MessageList
                          messages={messages}
                          showInteractivePrompt={showInteractivePrompt}
                          currentStep={currentStep}
                          currentStepData={currentStepData}
                          processData={processData}
                          finalTreatment={finalTreatment}
                          generatedAssets={generatedAssets}
                          onDownloadTreatment={handleDownloadTreatment}
                          onShareTreatment={handleShareTreatment}
                          onViewPDF={handleViewPDF}
                          onInteractiveContinue={handleInteractiveContinue}
                          onInteractiveFeedback={handleInteractiveFeedback}
                          onInteractiveRegenerate={handleInteractiveRegenerate}
                          addMessage={addMessage}
                          messagesEndRef={messagesEndRef}
                      />
                    </div>
                  </div>

                  {/* Fixed Message Input at Bottom */}
                  <div className="flex-shrink-0">
                    <MessageInput
                        onSendMessage={handleSendMessage}
                        isProcessing={isProcessing}
                        mode={mode}
                        onModeChange={handleModeChange}
                        placeholder={messages?.length === 0
                            ? "Enter your brand name to get started..."
                            : "Ask questions or provide feedback..."}
                    />
                  </div>
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