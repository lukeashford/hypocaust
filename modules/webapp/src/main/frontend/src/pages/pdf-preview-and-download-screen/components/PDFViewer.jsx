import React from 'react';
import Icon from '../../../components/AppIcon';

const PDFViewer = ({
  treatmentContent = null,
  isLoading = false,
  onFullscreen = null,
  className = ""
}) => {

  const renderTreatmentContent = () => {
    if (!treatmentContent) {
      return (
          <div className="space-y-6">
            <div className="text-center py-12">
              <Icon name="FileText" size={64} color="var(--color-muted-foreground)"
                    className="mx-auto mb-4"/>
              <h3 className="text-lg font-semibold text-foreground mb-2">Director's Treatment
                Preview</h3>
              <p className="text-muted-foreground">Treatment content will appear here</p>
            </div>
          </div>
      );
    }

    return (
        <div className="space-y-8 text-foreground">
          {/* Title */}
          <div className="text-center pb-6 border-b border-border">
            <h1 className="text-3xl font-heading font-bold mb-2">
              {treatmentContent?.title || `Director's Treatment: ${treatmentContent?.brandName}`}
            </h1>
            <p className="text-muted-foreground">
              Generated on {new Date(
                treatmentContent?.generatedAt || Date.now())?.toLocaleDateString()}
            </p>
          </div>
          {/* Executive Summary */}
          {treatmentContent?.executiveSummary && (
              <section>
                <h2 className="text-xl font-heading font-bold mb-4 text-accent">Executive
                  Summary</h2>
                <p className="text-sm leading-relaxed text-muted-foreground">
                  {treatmentContent?.executiveSummary}
                </p>
              </section>
          )}
          {/* Creative Strategy */}
          {treatmentContent?.creativeStrategy && (
              <section>
                <h2 className="text-xl font-heading font-bold mb-4 text-accent">Creative
                  Strategy</h2>
                <p className="text-sm leading-relaxed text-muted-foreground">
                  {treatmentContent?.creativeStrategy}
                </p>
              </section>
          )}
          {/* Story Breakdown */}
          {treatmentContent?.storyBreakdown && (
              <section>
                <h2 className="text-xl font-heading font-bold mb-4 text-accent">Story Breakdown</h2>
                <div className="text-sm leading-relaxed text-muted-foreground whitespace-pre-line">
                  {treatmentContent?.storyBreakdown}
                </div>
              </section>
          )}
          {/* Visual Direction */}
          {treatmentContent?.visualDirection && (
              <section>
                <h2 className="text-xl font-heading font-bold mb-4 text-accent">Visual
                  Direction</h2>
                <p className="text-sm leading-relaxed text-muted-foreground">
                  {treatmentContent?.visualDirection}
                </p>
              </section>
          )}
          {/* Production Notes */}
          {treatmentContent?.productionNotes && (
              <section>
                <h2 className="text-xl font-heading font-bold mb-4 text-accent">Production
                  Notes</h2>
                <p className="text-sm leading-relaxed text-muted-foreground">
                  {treatmentContent?.productionNotes}
                </p>
              </section>
          )}
          {/* Casting Notes */}
          {treatmentContent?.castingNotes && (
              <section>
                <h2 className="text-xl font-heading font-bold mb-4 text-accent">Casting Notes</h2>
                <p className="text-sm leading-relaxed text-muted-foreground">
                  {treatmentContent?.castingNotes}
                </p>
              </section>
          )}
          {/* Location Requirements */}
          {treatmentContent?.locationRequirements && (
              <section>
                <h2 className="text-xl font-heading font-bold mb-4 text-accent">Location
                  Requirements</h2>
                <p className="text-sm leading-relaxed text-muted-foreground">
                  {treatmentContent?.locationRequirements}
                </p>
              </section>
          )}
          {/* Technical Specifications */}
          {treatmentContent?.technicalSpecifications && (
              <section>
                <h2 className="text-xl font-heading font-bold mb-4 text-accent">Technical
                  Specifications</h2>
                <p className="text-sm leading-relaxed text-muted-foreground">
                  {treatmentContent?.technicalSpecifications}
                </p>
              </section>
          )}
          {/* Budget Considerations */}
          {treatmentContent?.budgetConsiderations && (
              <section>
                <h2 className="text-xl font-heading font-bold mb-4 text-accent">Budget
                  Considerations</h2>
                <p className="text-sm leading-relaxed text-muted-foreground">
                  {treatmentContent?.budgetConsiderations}
                </p>
              </section>
          )}
          {/* Timeline */}
          {treatmentContent?.timeline && (
              <section>
                <h2 className="text-xl font-heading font-bold mb-4 text-accent">Production
                  Timeline</h2>
                <p className="text-sm leading-relaxed text-muted-foreground">
                  {treatmentContent?.timeline}
                </p>
              </section>
          )}
          {/* Post Production Notes */}
          {treatmentContent?.postProductionNotes && (
              <section>
                <h2 className="text-xl font-heading font-bold mb-4 text-accent">Post-Production
                  Notes</h2>
                <p className="text-sm leading-relaxed text-muted-foreground">
                  {treatmentContent?.postProductionNotes}
                </p>
              </section>
          )}
        </div>
    );
  };

  if (isLoading) {
    return (
        <div className={`bg-card border border-border rounded-lg overflow-hidden ${className}`}>
          <div className="flex items-center justify-between p-4 border-b border-border">
            <div className="flex items-center space-x-2">
              <Icon name="FileText" size={20} color="var(--color-muted-foreground)"/>
              <h3 className="font-heading font-semibold text-foreground">Treatment Document</h3>
            </div>
            <div className="flex items-center space-x-2">
              <div
                  className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin"/>
              <span className="text-sm text-muted-foreground">Loading...</span>
            </div>
          </div>
          <div className="h-96 flex items-center justify-center">
            <div className="text-center">
              <div
                  className="w-12 h-12 border-4 border-accent border-t-transparent rounded-full animate-spin mx-auto mb-4"/>
              <p className="text-sm text-muted-foreground">Generating treatment preview...</p>
            </div>
          </div>
        </div>
    );
  }

  return (
      <div className={`bg-card border border-border rounded-lg overflow-hidden ${className}`}>
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-border bg-muted/30">
          <div className="flex items-center space-x-2">
            <Icon name="FileText" size={20} color="var(--color-accent)"/>
            <h3 className="font-heading font-semibold text-foreground">Treatment Document</h3>
          </div>

          <div className="flex items-center space-x-2">
            <button
                onClick={onFullscreen}
                className="p-2 text-muted-foreground hover:text-foreground transition-colors duration-200 rounded-md hover:bg-muted/50"
                title="Fullscreen"
            >
              <Icon name="Maximize" size={18}/>
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="p-6 max-h-[600px] overflow-y-auto">
          {renderTreatmentContent()}
        </div>
      </div>
  );
};

export default PDFViewer;