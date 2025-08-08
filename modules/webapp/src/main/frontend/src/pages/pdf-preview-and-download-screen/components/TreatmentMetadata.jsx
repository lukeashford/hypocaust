import React from 'react';
import Icon from '../../../components/AppIcon';

const TreatmentMetadata = ({
  treatmentData = null,
  className = ""
}) => {
  // Mock treatment data
  const mockData = treatmentData || {
    brandName: "TechFlow Solutions",
    generatedAt: "2025-01-07T20:36:26.637Z",
    mode: "Interactive",
    duration: "3 minutes 45 seconds",
    pages: 12,
    fileSize: "2.4 MB",
    elements: {
      scenes: 8,
      characters: 3,
      locations: 4,
      products: 2
    },
    style: {
      tone: "Professional & Modern",
      aesthetic: "Corporate Cinematic",
      colorPalette: "Deep Blues & Warm Accents"
    },
    targetAudience: "Business Professionals, IT Decision Makers",
    keyMessages: [
      "Streamlined Workflow Solutions",
      "Enterprise-Grade Security",
      "Intuitive User Experience"
    ]
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date?.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const MetadataSection = ({title, children, icon}) => (
      <div className="space-y-3">
        <div className="flex items-center space-x-2">
          <Icon name={icon} size={16} color="var(--color-accent)"/>
          <h3 className="text-sm font-heading font-semibold text-foreground">
            {title}
          </h3>
        </div>
        <div className="pl-6 space-y-2">
          {children}
        </div>
      </div>
  );

  const InfoRow = ({label, value, icon = null}) => (
      <div className="flex items-center justify-between py-1">
        <div className="flex items-center space-x-2">
          {icon && <Icon name={icon} size={14} className="text-muted-foreground"/>}
          <span className="text-xs text-muted-foreground">{label}:</span>
        </div>
        <span className="text-xs font-medium text-foreground">{value}</span>
      </div>
  );

  return (
      <div className={`bg-card border border-border rounded-lg ${className}`}>
        {/* Header */}
        <div className="p-4 border-b border-border">
          <div className="flex items-center space-x-2 mb-3">
            <Icon name="Info" size={18} color="var(--color-accent)"/>
            <h2 className="text-lg font-heading font-semibold text-foreground">
              Treatment Details
            </h2>
          </div>

          {/* Brand Name */}
          <div className="bg-accent/10 border border-accent/20 rounded-lg p-3">
            <div className="flex items-center space-x-2">
              <Icon name="Building" size={16} color="var(--color-accent)"/>
              <div>
                <p className="text-sm font-medium text-accent">Brand</p>
                <p className="text-lg font-heading font-bold text-foreground">
                  {mockData?.brandName}
                </p>
              </div>
            </div>
          </div>
        </div>
        {/* Content */}
        <div className="p-4 space-y-6">
          {/* Generation Info */}
          <MetadataSection title="Generation Info" icon="Clock">
            <InfoRow
                label="Generated"
                value={formatDate(mockData?.generatedAt)}
                icon="Calendar"
            />
            <InfoRow
                label="Mode"
                value={mockData?.mode}
                icon="Settings"
            />
            <InfoRow
                label="Duration"
                value={mockData?.duration}
                icon="Play"
            />
          </MetadataSection>

          {/* Document Stats */}
          <MetadataSection title="Document Stats" icon="FileText">
            <InfoRow
                label="Pages"
                value={mockData?.pages}
                icon="File"
            />
            <InfoRow
                label="File Size"
                value={mockData?.fileSize}
                icon="HardDrive"
            />
            <InfoRow
                label="Format"
                value="PDF"
                icon="Download"
            />
          </MetadataSection>

          {/* Treatment Elements */}
          <MetadataSection title="Treatment Elements" icon="Film">
            <InfoRow
                label="Scenes"
                value={mockData?.elements?.scenes}
                icon="Camera"
            />
            <InfoRow
                label="Characters"
                value={mockData?.elements?.characters}
                icon="Users"
            />
            <InfoRow
                label="Locations"
                value={mockData?.elements?.locations}
                icon="MapPin"
            />
            <InfoRow
                label="Products"
                value={mockData?.elements?.products}
                icon="Package"
            />
          </MetadataSection>

          {/* Visual Style */}
          <MetadataSection title="Visual Style" icon="Palette">
            <InfoRow
                label="Tone"
                value={mockData?.style?.tone}
            />
            <InfoRow
                label="Aesthetic"
                value={mockData?.style?.aesthetic}
            />
            <InfoRow
                label="Color Palette"
                value={mockData?.style?.colorPalette}
            />
          </MetadataSection>

          {/* Target Audience */}
          <MetadataSection title="Target Audience" icon="Target">
            <p className="text-xs text-foreground leading-relaxed">
              {mockData?.targetAudience}
            </p>
          </MetadataSection>

          {/* Key Messages */}
          <MetadataSection title="Key Messages" icon="MessageSquare">
            <div className="space-y-1">
              {mockData?.keyMessages?.map((message, index) => (
                  <div key={index} className="flex items-start space-x-2">
                    <div className="w-1.5 h-1.5 bg-accent rounded-full mt-1.5 flex-shrink-0"/>
                    <p className="text-xs text-foreground leading-relaxed">
                      {message}
                    </p>
                  </div>
              ))}
            </div>
          </MetadataSection>

          {/* Quality Indicator */}
          <div className="bg-success/10 border border-success/20 rounded-lg p-3">
            <div className="flex items-center space-x-2">
              <Icon name="CheckCircle" size={16} color="var(--color-success)"/>
              <div>
                <p className="text-sm font-medium text-success">Production Ready</p>
                <p className="text-xs text-success/80">
                  Meets industry standards for client presentations
                </p>
              </div>
            </div>
          </div>

          {/* Export History */}
          <div className="border-t border-border pt-4">
            <div className="flex items-center space-x-2 mb-3">
              <Icon name="History" size={14} color="var(--color-muted-foreground)"/>
              <span className="text-xs font-medium text-muted-foreground">Export History</span>
            </div>
            <div className="space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="text-muted-foreground">Last downloaded:</span>
                <span className="text-foreground font-mono">Never</span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-muted-foreground">Share count:</span>
                <span className="text-foreground font-mono">0</span>
              </div>
            </div>
          </div>
        </div>
      </div>
  );
};

export default TreatmentMetadata;