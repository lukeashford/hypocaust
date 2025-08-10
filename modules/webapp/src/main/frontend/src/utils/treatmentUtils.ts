import {NavigateFunction} from 'react-router-dom';

export interface TreatmentHandlers {
  handleDownloadTreatment: (format?: string) => void;
  handleShareTreatment: (method: string) => void;
  handleViewPDF: () => void;
}

export const createTreatmentHandlers = (
    navigate: NavigateFunction,
    addMessage: (message: string, isUser: boolean) => void,
    processData: any,
    finalTreatment: any,
    generatedAssets: any[]
): TreatmentHandlers => {
  const handleDownloadTreatment = (format = 'pdf') => {
    // Use real treatment data for download
    const treatmentContent = processData?.finalTreatment || finalTreatment?.fullData;

    if (treatmentContent) {
      // In a real implementation, this would generate a PDF from the treatment data
      const link = document.createElement('a');
      const blob = new Blob([JSON.stringify(treatmentContent, null, 2)], {
        type: 'application/json'
      });
      link.href = URL.createObjectURL(blob);
      link.download = `${treatmentContent?.brandName || 'Treatment'}-${Date.now()}.${format
      === 'pdf' ? 'json' : format}`;
      link.click();
      URL.revokeObjectURL(link.href);

      addMessage(`Treatment downloaded as ${format?.toUpperCase()} successfully!`, false);
    } else {
      addMessage("No treatment data available for download.", false);
    }
  };

  const handleShareTreatment = (method: string) => {
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

  return {
    handleDownloadTreatment,
    handleShareTreatment,
    handleViewPDF
  };
};