import React from 'react';
import Icon from '../../AppIcon';

interface StoryMessageProps {
  content: string;
  storyContent: string;
}

const StoryMessage: React.FC<StoryMessageProps> = ({
  content,
  storyContent
}) => {
  return (
      <div className="space-y-4">
        <p className="text-sm leading-relaxed text-foreground">{content}</p>
        <div className="bg-card border border-border rounded-lg p-4">
          <div className="flex items-center space-x-2 mb-3">
            <Icon name="BookOpen" size={16} color="var(--color-accent)"/>
            <h4 className="text-sm font-semibold text-foreground">Cinematic Story Outline</h4>
          </div>
          <div
              className="text-sm text-muted-foreground font-mono whitespace-pre-line leading-relaxed max-h-96 overflow-y-auto">
            {storyContent}
          </div>
        </div>
      </div>
  );
};

export default StoryMessage;