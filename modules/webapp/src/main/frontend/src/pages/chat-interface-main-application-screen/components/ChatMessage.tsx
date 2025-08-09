import React from 'react';
import Icon from '../../../components/AppIcon';
import Image from '../../../components/AppImage';

interface Asset {
  type: 'image' | 'story' | 'pdf';
  url?: string;
  title?: string;
  description?: string;
  content?: string;
  size?: string;
  pages?: number;
}

interface ChatMessageProps {
  message: React.ReactNode;
  isUser?: boolean;
  timestamp?: string | null;
  isTyping?: boolean;
  assets?: Asset[] | null;
}

const ChatMessage: React.FC<ChatMessageProps> = ({
  message,
  isUser = false,
  timestamp = null,
  isTyping = false,
  assets = null
}) => {
  const formatTimestamp = (date: string | null): string => {
    if (!date) {
      return '';
    }
    return new Date(date)?.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const renderAssets = () => {
    if (!assets || assets?.length === 0) {
      return null;
    }

    return (
        <div className="mt-3 space-y-3">
          {assets?.map((asset, index) => (
              <div key={index}
                   className="border border-border rounded-lg overflow-hidden bg-muted/20">
                {asset?.type === 'image' && (
                    <div className="relative">
                      <Image
                          src={asset?.url}
                          alt={asset?.title || `Generated asset ${index + 1}`}
                          className="w-full h-48 object-cover"
                      />
                      <div
                          className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/60 to-transparent p-3">
                        <p className="text-white text-sm font-medium">{asset?.title}</p>
                        {asset?.description && (
                            <p className="text-white/80 text-xs mt-1">{asset?.description}</p>
                        )}
                      </div>
                    </div>
                )}

                {asset?.type === 'story' && (
                    <div className="p-4">
                      <div className="flex items-center space-x-2 mb-2">
                        <Icon name="FileText" size={16} color="var(--color-accent)"/>
                        <h4 className="text-sm font-semibold text-foreground">{asset?.title}</h4>
                      </div>
                      <div className="text-sm text-muted-foreground space-y-2">
                        {asset?.content?.split('\n')?.map((line, i) => (
                            <p key={i}>{line}</p>
                        ))}
                      </div>
                    </div>
                )}

                {asset?.type === 'pdf' && (
                    <div className="p-4">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-3">
                          <div
                              className="w-10 h-10 bg-accent/10 rounded-lg flex items-center justify-center">
                            <Icon name="FileText" size={20} color="var(--color-accent)"/>
                          </div>
                          <div>
                            <p className="text-sm font-medium text-foreground">{asset?.title}</p>
                            <p className="text-xs text-muted-foreground">{asset?.size} • {asset?.pages} pages</p>
                          </div>
                        </div>
                        <button
                            className="text-accent hover:text-accent/80 transition-colors duration-200">
                          <Icon name="Download" size={18}/>
                        </button>
                      </div>
                    </div>
                )}
              </div>
          ))}
        </div>
    );
  };

  return (
      <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-6`}>
        <div className={`flex max-w-[80%] ${isUser ? 'flex-row-reverse' : 'flex-row'} space-x-3`}>
          {/* Avatar */}
          <div className="flex-shrink-0">
            <div className={`w-8 h-8 rounded-full flex items-center justify-center ${
                isUser
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-accent text-accent-foreground'
            }`}>
              {isUser ? (
                  <Icon name="User" size={16}/>
              ) : (
                  <Icon name="Zap" size={16}/>
              )}
            </div>
          </div>

          {/* Message Content */}
          <div className={`flex-1 ${isUser ? 'mr-3' : 'ml-3'}`}>
            <div className={`rounded-lg p-4 ${
                isUser
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-card border border-border'
            }`}>
              {isTyping ? (
                  <div className="flex items-center space-x-2">
                    <div className="flex space-x-1">
                      <div className="w-2 h-2 bg-accent rounded-full animate-bounce"
                           style={{animationDelay: '0ms'}}></div>
                      <div className="w-2 h-2 bg-accent rounded-full animate-bounce"
                           style={{animationDelay: '150ms'}}></div>
                      <div className="w-2 h-2 bg-accent rounded-full animate-bounce"
                           style={{animationDelay: '300ms'}}></div>
                    </div>
                    <span className="text-sm text-muted-foreground">AI is thinking...</span>
                  </div>
              ) : (
                  <div className="space-y-2">
                    <p className={`text-sm leading-relaxed ${
                        isUser ? 'text-primary-foreground' : 'text-foreground'
                    }`}>
                      {message}
                    </p>
                    {renderAssets()}
                  </div>
              )}
            </div>

            {/* Timestamp */}
            {timestamp && !isTyping && (
                <p className={`text-xs text-muted-foreground mt-1 ${
                    isUser ? 'text-right' : 'text-left'
                }`}>
                  {formatTimestamp(timestamp)}
                </p>
            )}
          </div>
        </div>
      </div>
  );
};

export default ChatMessage;