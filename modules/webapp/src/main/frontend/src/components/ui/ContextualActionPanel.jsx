import React, {useState} from 'react';
import Icon from '../AppIcon';
import Button from './Button';

const ContextualActionPanel = ({
  pdfData = null,
  onDownload = null,
  onShare = null,
  onRegenerate = null,
  isVisible = true,
  className = ""
}) => {
  const [isShareMenuOpen, setIsShareMenuOpen] = useState(false);
  const [_downloadFormat, setDownloadFormat] = useState('pdf');

  const handleDownload = (format = 'pdf') => {
    if (onDownload) {
      onDownload(format);
    }
    setDownloadFormat(format);
  };

  const handleShare = (method) => {
    if (onShare) {
      onShare(method);
    }
    setIsShareMenuOpen(false);
  };

  const shareOptions = [
    {id: 'email', label: 'Email', icon: 'Mail'},
    {id: 'link', label: 'Copy Link', icon: 'Link'},
    {id: 'slack', label: 'Slack', icon: 'MessageSquare'},
    {id: 'teams', label: 'Teams', icon: 'Users'}
  ];

  const downloadOptions = [
    {
      id: 'pdf',
      label: 'PDF Document',
      icon: 'FileText',
      description: 'High-quality PDF for presentations'
    },
    {id: 'docx', label: 'Word Document', icon: 'FileText', description: 'Editable Word format'},
    {id: 'png', label: 'Image Export', icon: 'Image', description: 'High-resolution image'}
  ];

  if (!isVisible) {
    return null;
  }

  return (
      <div className={`bg-card border border-border rounded-lg shadow-warm-lg ${className}`}>
        {/* Header */}
        <div className="p-4 border-b border-border">
          <div className="flex items-center space-x-2">
            <Icon name="Download" size={18} color="var(--color-accent)"/>
            <h3 className="text-sm font-heading font-semibold text-foreground">
              Export & Share
            </h3>
          </div>
          <p className="text-xs text-muted-foreground mt-1">
            Download or share your cinematic treatment
          </p>
        </div>
        {/* Actions */}
        <div className="p-4 space-y-4">
          {/* Primary Download Action */}
          <div className="space-y-2">
            <Button
                variant="default"
                size="lg"
                fullWidth
                onClick={() => handleDownload('pdf')}
                iconName="Download"
                iconPosition="left"
                className="bg-accent hover:bg-accent/90 text-accent-foreground shadow-warm-md hover:shadow-warm-lg transition-all duration-200"
            >
              Download PDF Treatment
            </Button>

            {/* Download Options */}
            <div className="grid grid-cols-1 gap-2">
              {downloadOptions?.slice(1)?.map((option) => (
                  <button
                      key={option?.id}
                      onClick={() => handleDownload(option?.id)}
                      className="flex items-center space-x-3 p-3 text-left hover:bg-muted/50 rounded-lg transition-colors duration-200 group"
                  >
                    <Icon name={option?.icon} size={16}
                          className="text-muted-foreground group-hover:text-foreground"/>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-foreground">{option?.label}</p>
                      <p className="text-xs text-muted-foreground">{option?.description}</p>
                    </div>
                    <Icon name="ChevronRight" size={14}
                          className="text-muted-foreground group-hover:text-accent"/>
                  </button>
              ))}
            </div>
          </div>

          {/* Share Actions */}
          <div className="space-y-2">
            <div className="relative">
              <Button
                  variant="outline"
                  size="default"
                  fullWidth
                  onClick={() => setIsShareMenuOpen(!isShareMenuOpen)}
                  iconName="Share"
                  iconPosition="left"
                  className="justify-between"
              >
                Share Treatment
                <Icon name="ChevronDown" size={14}
                      className={`transition-transform duration-200 ${isShareMenuOpen ? 'rotate-180'
                          : ''}`}/>
              </Button>

              {isShareMenuOpen && (
                  <div
                      className="absolute top-full left-0 right-0 mt-2 bg-popover border border-border rounded-lg shadow-warm-lg z-50 animate-scale-in">
                    <div className="p-2">
                      {shareOptions?.map((option) => (
                          <button
                              key={option?.id}
                              onClick={() => handleShare(option?.id)}
                              className="w-full flex items-center space-x-3 px-3 py-2 text-sm text-foreground hover:bg-muted/50 rounded-md transition-colors duration-200"
                          >
                            <Icon name={option?.icon} size={16}/>
                            <span>{option?.label}</span>
                          </button>
                      ))}
                    </div>
                  </div>
              )}
            </div>
          </div>

          {/* Secondary Actions */}
          <div className="border-t border-border pt-4 space-y-2">
            <Button
                variant="ghost"
                size="default"
                fullWidth
                onClick={onRegenerate}
                iconName="RefreshCw"
                iconPosition="left"
                className="text-muted-foreground hover:text-foreground"
            >
              Generate New Version
            </Button>

            <Button
                variant="ghost"
                size="default"
                fullWidth
                iconName="Edit"
                iconPosition="left"
                className="text-muted-foreground hover:text-foreground"
            >
              Request Modifications
            </Button>
          </div>

          {/* Status Information */}
          {pdfData && (
              <div className="border-t border-border pt-4">
                <div className="space-y-2 text-xs text-muted-foreground font-mono">
                  <div className="flex justify-between">
                    <span>Document Size:</span>
                    <span>{pdfData?.size || '2.4 MB'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Pages:</span>
                    <span>{pdfData?.pages || '12'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Generated:</span>
                    <span>{pdfData?.timestamp || 'Just now'}</span>
                  </div>
                </div>
              </div>
          )}

          {/* Quality Indicator */}
          <div className="bg-success/10 border border-success/20 rounded-lg p-3">
            <div className="flex items-center space-x-2">
              <Icon name="CheckCircle" size={16} color="var(--color-success)"/>
              <div>
                <p className="text-sm font-medium text-success">Production Ready</p>
                <p className="text-xs text-success/80">
                  This treatment meets industry standards for client presentations
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
  );
};

export default ContextualActionPanel;