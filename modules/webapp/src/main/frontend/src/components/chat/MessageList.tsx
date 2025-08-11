import React from 'react';
import ChatMessage from '../../pages/chat-interface-main-application-screen/components/ChatMessage';
import InteractivePrompt
  from '../../pages/chat-interface-main-application-screen/components/InteractivePrompt';
import {ChatMessage as ChatMessageType} from '../../hooks/useChatState';
import TreatmentMessage from './message-types/TreatmentMessage';
import StoryMessage from './message-types/StoryMessage';
import AssetsMessage from './message-types/AssetsMessage';
import DataMessage from './message-types/DataMessage';
import ErrorMessage from './message-types/ErrorMessage';

interface MessageListProps {
  messages: ChatMessageType[];
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
  onDownloadTreatment,
  onViewPDF,
  onInteractiveContinue,
  onInteractiveFeedback,
  onInteractiveRegenerate,
  messagesEndRef
}) => {

  const renderMessageContent = (message: ChatMessageType) => {
    if (message.errorData) {
      return (
          <ErrorMessage
              content={message.content}
              errorData={message.errorData}
          />
      );
    }

    if (message.treatmentData) {
      return (
          <TreatmentMessage
              content={message.content}
              treatmentData={message.treatmentData}
              onViewPDF={onViewPDF}
              onDownloadTreatment={onDownloadTreatment}
          />
      );
    }

    if (message.storyContent) {
      return (
          <StoryMessage
              content={message.content}
              storyContent={message.storyContent}
          />
      );
    }

    if (message.assets) {
      return (
          <AssetsMessage
              content={message.content}
              assets={message.assets}
          />
      );
    }

    if (message.data && typeof message.data === 'object') {
      return (
          <DataMessage
              content={message.content}
              data={message.data}
          />
      );
    }

    return message.content;
  };

  return (
      <div className="space-y-6">
        {messages?.map((message) => (
            <ChatMessage
                key={message?.id}
                message={renderMessageContent(message)}
                isUser={message?.isUser}
                timestamp={message?.timestamp ? message.timestamp.toISOString() : null}
                isTyping={message?.isTyping}
            />
        ))}

        {/* Interactive Prompt */}
        {showInteractivePrompt && (
            <div className="mt-6">
              <InteractivePrompt
                  step={currentStep === 1 ? 'research' : currentStep === 2 ? 'story'
                      : currentStep === 3 ? 'characters' : 'visuals'}
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