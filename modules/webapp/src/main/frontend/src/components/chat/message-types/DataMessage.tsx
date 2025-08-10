import React from 'react';
import Icon from '../../AppIcon';

interface MessageData {
  summary?: string;
  keyPoints?: string[];
  brandPersonality?: string;
  targetAudience?: string;
  visualStyle?: string;
}

interface DataMessageProps {
  content: string;
  data: MessageData;
}

const DataMessage: React.FC<DataMessageProps> = ({
  content,
  data
}) => {
  return (
      <div className="space-y-4">
        <p className="text-sm leading-relaxed text-foreground">{content}</p>
        <div className="bg-muted/30 rounded-lg p-4 border border-border/50">
          <div className="space-y-3">
            {data.summary && (
                <div>
                  <h4 className="text-sm font-medium text-foreground mb-1">Summary:</h4>
                  <p className="text-sm text-muted-foreground">{data.summary}</p>
                </div>
            )}
            {data.keyPoints && (
                <div>
                  <h4 className="text-sm font-medium text-foreground mb-2">Key Points:</h4>
                  <ul className="space-y-1">
                    {data.keyPoints.map((point, index) => (
                        <li key={index}
                            className="flex items-start space-x-2 text-sm text-muted-foreground">
                          <Icon name="ChevronRight" size={14} className="mt-0.5 flex-shrink-0"/>
                          <span>{point}</span>
                        </li>
                    ))}
                  </ul>
                </div>
            )}
            {data.brandPersonality && (
                <div>
                  <h4 className="text-sm font-medium text-foreground mb-1">Brand Personality:</h4>
                  <p className="text-sm text-muted-foreground">{data.brandPersonality}</p>
                </div>
            )}
            {data.targetAudience && (
                <div>
                  <h4 className="text-sm font-medium text-foreground mb-1">Target Audience:</h4>
                  <p className="text-sm text-muted-foreground">{data.targetAudience}</p>
                </div>
            )}
            {data.visualStyle && (
                <div>
                  <h4 className="text-sm font-medium text-foreground mb-1">Visual Style:</h4>
                  <p className="text-sm text-muted-foreground">{data.visualStyle}</p>
                </div>
            )}
          </div>
        </div>
      </div>
  );
};

export default DataMessage;