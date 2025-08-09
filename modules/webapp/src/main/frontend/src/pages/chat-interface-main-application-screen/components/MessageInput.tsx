import React, {useEffect, useRef, useState} from 'react';
import Icon from '../../../components/AppIcon';
import Button from '../../../components/ui/Button';
import Input from '../../../components/ui/Input';

type GenerationMode = 'interactive' | 'oneshot';

interface MessageInputProps {
  onSendMessage: (message: string) => void;
  isProcessing?: boolean;
  mode?: GenerationMode;
  onModeChange?: ((mode: GenerationMode) => void) | null;
  placeholder?: string;
}

const MessageInput: React.FC<MessageInputProps> = ({
  onSendMessage,
  isProcessing = false,
  mode = 'interactive',
  onModeChange = null,
  placeholder = "Enter your brand name to get started..."
}) => {
  const [message, setMessage] = useState<string>('');
  const [isExpanded, setIsExpanded] = useState<boolean>(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (isExpanded && textareaRef?.current) {
      textareaRef?.current?.focus();
    } else if (!isExpanded && inputRef?.current) {
      inputRef?.current?.focus();
    }
  }, [isExpanded]);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>): void => {
    e?.preventDefault();
    if (message?.trim() && !isProcessing) {
      onSendMessage(message?.trim());
      setMessage('');
      setIsExpanded(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>): void => {
    if (e?.key === 'Enter' && !e?.shiftKey) {
      e?.preventDefault();
      handleSubmit(e as any);
    }
  };

  const toggleExpanded = (): void => {
    setIsExpanded(!isExpanded);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>): void => {
    setMessage(e?.target?.value);
  };

  const handleTextareaChange = (e: React.ChangeEvent<HTMLTextAreaElement>): void => {
    setMessage(e?.target?.value);
  };

  return (
      <div className="border-t border-border bg-background/95 backdrop-blur-sm">
        <div className="max-w-4xl mx-auto p-4">
          {/* Mode Selector */}
          {onModeChange && (
              <div className="flex items-center justify-center mb-4">
                <div className="bg-muted rounded-lg p-1 flex items-center space-x-1">
                  <button
                      onClick={() => onModeChange('interactive')}
                      className={`px-3 py-2 text-sm font-medium rounded-md transition-all duration-200 ${
                          mode === 'interactive' ? 'bg-accent text-accent-foreground shadow-warm-sm'
                              : 'text-muted-foreground hover:text-foreground'
                      }`}
                  >
                    <Icon name="MessageCircle" size={16} className="inline mr-2"/>
                    Interactive
                  </button>
                  <button
                      onClick={() => onModeChange('oneshot')}
                      className={`px-3 py-2 text-sm font-medium rounded-md transition-all duration-200 ${
                          mode === 'oneshot' ? 'bg-accent text-accent-foreground shadow-warm-sm'
                              : 'text-muted-foreground hover:text-foreground'
                      }`}
                  >
                    <Icon name="Zap" size={16} className="inline mr-2"/>
                    One-shot
                  </button>
                </div>
              </div>
          )}

          {/* Input Form */}
          <form onSubmit={handleSubmit} className="space-y-3">
            <div className="relative">
              {!isExpanded ? (
                  <div className="flex items-center space-x-2">
                    <div className="flex-1">
                      <Input
                          ref={inputRef}
                          type="text"
                          placeholder={placeholder}
                          value={message}
                          onChange={handleInputChange}
                          disabled={isProcessing}
                          className="pr-20"
                      />
                    </div>

                    <div className="flex items-center space-x-2">
                      <button
                          type="button"
                          onClick={toggleExpanded}
                          className="p-2 text-muted-foreground hover:text-foreground transition-colors duration-200 rounded-md hover:bg-muted/50"
                          title="Expand for detailed input"
                      >
                        <Icon name="Maximize2" size={18}/>
                      </button>

                      <Button
                          type="submit"
                          variant="default"
                          size="default"
                          disabled={!message?.trim() || isProcessing}
                          loading={isProcessing}
                          iconName="Send"
                          iconPosition="right"
                          className="bg-accent hover:bg-accent/90"
                      >
                        {isProcessing ? 'Processing...' : 'Generate'}
                      </Button>
                    </div>
                  </div>
              ) : (
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <h3 className="text-sm font-medium text-foreground">Detailed Brand Input</h3>
                      <button
                          type="button"
                          onClick={toggleExpanded}
                          className="p-1 text-muted-foreground hover:text-foreground transition-colors duration-200"
                      >
                        <Icon name="Minimize2" size={16}/>
                      </button>
                    </div>

                    <textarea
                        ref={textareaRef}
                        placeholder={`Enter your brand name and any additional context:\n\nExample:\n- Brand: "TechFlow Solutions"\n- Industry: Software Development\n- Target Audience: Enterprise clients\n- Key Message: Innovation and reliability`}
                        value={message}
                        onChange={handleTextareaChange}
                        onKeyDown={handleKeyDown}
                        disabled={isProcessing}
                        rows={6}
                        className="w-full px-3 py-2 bg-input border border-border rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent resize-none"
                    />

                    <div className="flex items-center justify-between">
                      <p className="text-xs text-muted-foreground">
                        Press Shift+Enter for new line, Enter to send
                      </p>

                      <Button
                          type="submit"
                          variant="default"
                          size="default"
                          disabled={!message?.trim() || isProcessing}
                          loading={isProcessing}
                          iconName="Send"
                          iconPosition="right"
                          className="bg-accent hover:bg-accent/90"
                      >
                        {isProcessing ? 'Processing...' : 'Generate Treatment'}
                      </Button>
                    </div>
                  </div>
              )}
            </div>
          </form>

          {/* Mode Description */}
          <div className="mt-3 text-center">
            <p className="text-xs text-muted-foreground">
              {mode === 'interactive' ? (
                  <>
                    <Icon name="MessageCircle" size={12} className="inline mr-1"/>
                    Interactive mode: Get feedback prompts at each step for customization
                  </>
              ) : (
                  <>
                    <Icon name="Zap" size={12} className="inline mr-1"/>
                    One-shot mode: Generate complete treatment automatically without interruption
                  </>
              )}
            </p>
          </div>

          {/* Processing Indicator */}
          {isProcessing && (
              <div className="mt-4 flex items-center justify-center space-x-2 text-accent">
                <div
                    className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin"></div>
                <span
                    className="text-sm font-medium">AI agents are working on your treatment...</span>
              </div>
          )}
        </div>
      </div>
  );
};

export default MessageInput;