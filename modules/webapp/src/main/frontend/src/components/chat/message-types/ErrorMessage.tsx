import React from 'react';
import Icon from '../../AppIcon';

interface ErrorData {
  message: string;
  timestamp: string;
}

interface ErrorMessageProps {
  content: string;
  errorData: ErrorData;
}

const ErrorMessage: React.FC<ErrorMessageProps> = ({
  content,
  errorData
}) => {
  return (
      <div className="space-y-4">
        <p className="text-sm leading-relaxed text-foreground">{content}</p>
        <div className="bg-destructive/10 border border-destructive/20 rounded-lg p-4">
          <div className="flex items-center space-x-3 mb-3">
            <Icon name="AlertCircle" size={20} color="var(--color-destructive)"/>
            <div>
              <h4 className="text-sm font-semibold text-destructive">Error</h4>
              <p className="text-xs text-destructive/80">{errorData.timestamp}</p>
            </div>
          </div>

          <div className="text-sm text-destructive/90 leading-relaxed">
            {errorData.message}
          </div>
        </div>
      </div>
  );
};

export default ErrorMessage;