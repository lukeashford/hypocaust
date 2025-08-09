import React from 'react';
import Icon from '../AppIcon';

/**
 * Props interface for ChatHeader component
 */
interface ChatErrorDisplayProps {
  /** Error message to display, null/undefined if no error */
  error: string | null;
}

/**
 * ChatHeader component that displays error messages in the chat interface
 * Extracted from ChatInterface to handle header section and error display
 */
const ChatErrorDisplay: React.FC<ChatErrorDisplayProps> = ({error}) => {
  return (
      <>
        {/* Error Display */}
        {error && (
            <div className="mb-6 p-4 bg-destructive/10 border border-destructive/20 rounded-lg">
              <div className="flex items-center space-x-2">
                <Icon name="AlertCircle" size={16} color="var(--color-destructive)"/>
                <h4 className="text-sm font-medium text-destructive">Generation Error</h4>
              </div>
              <p className="text-sm text-destructive/80 mt-1">{error}</p>
            </div>
        )}
      </>
  );
};

export default ChatErrorDisplay;