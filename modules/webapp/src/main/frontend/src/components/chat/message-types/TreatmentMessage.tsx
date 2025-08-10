import React from 'react';
import Icon from '../../AppIcon';
import {TreatmentData} from 'hooks/useChatState.ts';

interface TreatmentMessageProps {
  content: string;
  treatmentData: TreatmentData;
  onViewPDF: () => void;
  onDownloadTreatment: (format?: string) => void;
}

const TreatmentMessage: React.FC<TreatmentMessageProps> = ({
  content,
  treatmentData,
  onViewPDF,
  onDownloadTreatment
}) => {
  return (
      <div className="space-y-4">
        <p className="text-sm leading-relaxed text-foreground">{content}</p>
        <div className="bg-success/10 border border-success/20 rounded-lg p-4">
          <div className="flex items-center space-x-3 mb-3">
            <Icon name="FileText" size={20} color="var(--color-success)"/>
            <div>
              <h4 className="text-sm font-semibold text-success">{treatmentData.title}</h4>
              <p className="text-xs text-success/80">{treatmentData.pages} pages
                • {treatmentData.size}</p>
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
};

export default TreatmentMessage;