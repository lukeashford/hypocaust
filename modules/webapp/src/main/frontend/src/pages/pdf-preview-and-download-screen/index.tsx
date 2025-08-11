import React, {useEffect, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import Header from '../../components/ui/Header';
import PDFViewer from './components/PDFViewer';
import TreatmentMetadata from './components/TreatmentMetadata';
import ActionPanel from './components/ActionPanel';
import FeedbackSection from './components/FeedbackSection';
import BreadcrumbNavigation from './components/BreadcrumbNavigation';
import Icon from '../../components/AppIcon';

interface User {
  name: string;
  email: string;
}

interface TreatmentContent {
  title?: string;
  executiveSummary?: string;
  creativeStrategy?: string;
  storyBreakdown?: string;
  visualDirection?: string;
  productionNotes?: string;
  brandName?: string;
  generatedAt?: string;
  documentMetadata?: {
    size?: string;
    pages?: number;
  };
}

interface Asset {
  id?: string;
  name?: string;
  url?: string;
  type?: string;
}

interface PDFData {
  brandName?: string;
  generatedAt: string;
  mode: string;
  pdfUrl?: string | null;
  size: string;
  pages: number;
  fullContent: TreatmentContent | null;
  assets: Asset[];
}

interface LocationState {
  treatmentData?: TreatmentContent;
  assets?: Asset[];
}

interface ShareMethod {
  [key: string]: string;
}

interface FeedbackData {
  rating?: number;
  comments?: string;
  category?: string;
}

const PDFPreviewAndDownloadScreen: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [isDownloading, setIsDownloading] = useState<boolean>(false);
  const [pdfData, setPdfData] = useState<PDFData | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Mock user data
  const mockUser: User = {
    name: "Creative Professional",
    email: "user@example.com"
  };

  useEffect(() => {
    // Get treatment data from navigation state or use mock data
    const loadPdfData = async (): Promise<void> => {
      const state = location?.state as LocationState | undefined;
      const treatmentData = state?.treatmentData;
      const assets = state?.assets;

      if (treatmentData) {
        // Use real treatment data
        setPdfData({
          brandName: treatmentData?.brandName,
          generatedAt: treatmentData?.generatedAt || new Date().toISOString(),
          mode: "Interactive",
          pdfUrl: null, // PDF URL would be generated from treatment content
          size: treatmentData?.documentMetadata?.size || "2.4 MB",
          pages: treatmentData?.documentMetadata?.pages || 12,
          fullContent: treatmentData,
          assets: assets || []
        });
      } else {
        // Use mock data as fallback
        setPdfData({
          brandName: "TechFlow Solutions",
          generatedAt: "2025-01-07T20:36:26.637Z",
          mode: "Interactive",
          pdfUrl: null,
          size: "2.4 MB",
          pages: 12,
          fullContent: null,
          assets: []
        });
      }

      setIsLoading(false);
    };

    loadPdfData();
  }, [location?.state]);

  const handleDownload = async (format: string = 'pdf'): Promise<void> => {
    setIsDownloading(true);

    try {
      // Simulate download process
      await new Promise(resolve => setTimeout(resolve, 2000));

      if (pdfData?.fullContent) {
        // Generate download with real content
        if (format === 'pdf') {
          // In a real implementation, this would generate a proper PDF
          const treatmentText = `
Director's Treatment: ${pdfData?.fullContent?.title || pdfData?.brandName}

Executive Summary:
${pdfData?.fullContent?.executiveSummary || 'No content available'}

Creative Strategy:
${pdfData?.fullContent?.creativeStrategy || 'No content available'}

Story Breakdown:
${pdfData?.fullContent?.storyBreakdown || 'No content available'}

Visual Direction:
${pdfData?.fullContent?.visualDirection || 'No content available'}

Production Notes:
${pdfData?.fullContent?.productionNotes || 'No content available'}

Generated on: ${new Date(pdfData?.generatedAt)?.toLocaleDateString()}
          `;

          const blob = new Blob([treatmentText], {type: 'text/plain'});
          const link = document.createElement('a');
          link.href = URL.createObjectURL(blob);
          link.download = `${pdfData?.brandName?.replace(/\s+/g, '-')
          || 'Treatment'}-${Date.now()}.txt`;
          link?.click();
          URL.revokeObjectURL(link?.href);
        } else {
          // JSON format
          const blob = new Blob([JSON.stringify(pdfData?.fullContent, null, 2)],
              {type: 'application/json'});
          const link = document.createElement('a');
          link.href = URL.createObjectURL(blob);
          link.download = `${pdfData?.brandName?.replace(/\s+/g, '-')
          || 'Treatment'}-${Date.now()}.json`;
          link?.click();
          URL.revokeObjectURL(link?.href);
        }
      } else {
        // Create mock download as fallback
        const element = document.createElement('a');
        element.href = 'data:application/pdf;base64,JVBERi0xLjQKMSAwIG9iago8PAovVHlwZSAvQ2F0YWxvZwovUGFnZXMgMiAwIFIKPj4KZW5kb2JqCjIgMCBvYmoKPDwKL1R5cGUgL1BhZ2VzCi9LaWRzIFszIDAgUl0KL0NvdW50IDEKPD4KZW5kb2JqCjMgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCAyIDAgUgovTWVkaWFCb3ggWzAgMCA2MTIgNzkyXQovUmVzb3VyY2VzIDw8Ci9Gb250IDw8Ci9GMSANCj4+Cj4+Ci9Db250ZW50cyA1IDAgUgo+PgplbmRvYmoKNCAwIG9iago8PAovVHlwZSAvRm9udAovU3VidHlwZSAvVHlwZTEKL0Jhc2VGb250IC9IZWx2ZXRpY2EKPj4KZW5kb2JqCjUgMCBvYmoKPDwKL0xlbmd0aCA0NAo+PgpzdHJlYW0KQlQKL0YxIDEyIFRmCjEwMCA3MDAgVGQKKEhlbGxvIFdvcmxkKSBUagpFVAplbmRzdHJlYW0KZW5kb2JqCnhyZWYKMCA2CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDAwOSAwMDAwMCBuIAowMDAwMDAwMDU4IDAwMDAwIG4gCjAwMDAwMDAxMTUgMDAwMDAgbiAKMDAwMDAwMDI0NSAwMDAwMCBuIAowMDAwMDAwMzE0IDAwMDAwIG4gCnRyYWlsZXIKPDwKL1NpemUgNgovUm9vdCAxIDAgUgo+PgpzdGFydHhyZWYKNDA4CiUlRU9G';
        element.download = `${pdfData?.brandName?.replace(/\s+/g, '-') || 'Treatment'}.${format}`;
        document.body?.appendChild(element);
        element?.click();
        document.body?.removeChild(element);
      }

      setIsDownloading(false);
    } catch (error) {
      console.error('Download error:', error);
      setIsDownloading(false);
    }
  };

  const handleGenerateNew = (): void => {
    navigate('/chat-interface-main-application-screen');
  };

  const handleShare = (method: string): void => {
    // Mock share functionality
    console.log(`Sharing via ${method}`);

    // Show success message (in real app, would integrate with actual sharing services)
    const messages: ShareMethod = {
      email: 'Email sharing link copied to clipboard',
      link: 'Shareable link copied to clipboard',
      slack: 'Shared to Slack workspace',
      teams: 'Shared to Microsoft Teams'
    };

    // In a real app, you would show a toast notification
    alert(messages?.[method] || 'Shared successfully');
  };

  const handleSubmitFeedback = (feedbackData: FeedbackData): void => {
    console.log('Feedback submitted:', feedbackData);
    // In real app, would send to analytics/feedback service
  };

  const handleNavigation = (path: string): void => {
    navigate(path);
  };

  const handleFullscreen = (): void => {
    // Handle fullscreen PDF viewer
    console.log('Toggle fullscreen PDF viewer');
  };

  return (
      <div className="min-h-screen bg-background">
        <Header user={mockUser} onNavigate={handleNavigation}/>

        <main className="container mx-auto px-4 lg:px-6 py-6 space-y-6">
          {/* Breadcrumb Navigation */}
          <BreadcrumbNavigation/>

          {/* Page Header */}
          <div className="space-y-2">
            <div className="flex items-center space-x-2">
              <Icon name="Eye" size={24} color="var(--color-accent)"/>
              <h1 className="text-2xl lg:text-3xl font-heading font-bold text-foreground">
                Treatment Preview
              </h1>
            </div>
            <p className="text-muted-foreground">
              Review your completed director's treatment before download
            </p>
          </div>

          {/* Main Content */}
          <div className="grid grid-cols-1 xl:grid-cols-4 gap-6">
            {/* PDF Viewer - Left Panel (70% on desktop) */}
            <div className="xl:col-span-3 space-y-4">
              <PDFViewer
                  pdfUrl={pdfData?.pdfUrl}
                  treatmentContent={pdfData?.fullContent}
                  isLoading={isLoading}
                  onFullscreen={handleFullscreen}
                  className="w-full"
              />
            </div>

            {/* Right Panel (30% on desktop) */}
            <div className="xl:col-span-1 space-y-6">
              {/* Treatment Metadata */}
              <TreatmentMetadata
                  treatmentData={pdfData}
                  className="w-full"
              />

              {/* Action Panel */}
              <ActionPanel
                  onDownload={handleDownload}
                  onGenerateNew={handleGenerateNew}
                  onShare={handleShare}
                  isDownloading={isDownloading}
                  className="w-full"
              />

              {/* Feedback Section */}
              <FeedbackSection
                  onSubmitFeedback={handleSubmitFeedback}
                  className="w-full"
              />
            </div>
          </div>

          {/* Mobile Action Bar */}
          <div
              className="xl:hidden fixed bottom-0 left-0 right-0 bg-background/95 backdrop-blur-sm border-t border-border p-4 z-50">
            <div className="flex space-x-3">
              <button
                  onClick={() => handleDownload('pdf')}
                  disabled={isDownloading}
                  className="flex-1 bg-accent hover:bg-accent/90 text-accent-foreground px-4 py-3 rounded-lg font-medium transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
              >
                {isDownloading ? (
                    <>
                      <div
                          className="w-4 h-4 border-2 border-accent-foreground border-t-transparent rounded-full animate-spin"/>
                      <span>Downloading...</span>
                    </>
                ) : (
                    <>
                      <Icon name="Download" size={18}/>
                      <span>Download</span>
                    </>
                )}
              </button>

              <button
                  onClick={() => handleShare('link')}
                  className="px-4 py-3 border border-border rounded-lg hover:bg-muted/50 transition-colors duration-200 flex items-center justify-center"
              >
                <Icon name="Share" size={18}/>
              </button>
            </div>
          </div>

          {/* Mobile spacer */}
          <div className="xl:hidden h-20"/>
        </main>
      </div>
  );
};

export default PDFPreviewAndDownloadScreen;