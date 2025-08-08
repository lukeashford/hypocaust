import React, {useState} from 'react';
import Icon from '../../../components/AppIcon';
import Button from '../../../components/ui/Button';

const ActionPanel = ({
  onDownload = null,
  onGenerateNew = null,
  onShare = null,
  isDownloading = false,
  className = ""
}) => {
  const [shareMenuOpen, setShareMenuOpen] = useState(false);
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
    setShareMenuOpen(false);
  };

  const shareOptions = [
    {id: 'email', label: 'Email', icon: 'Mail', description: 'Send via email'},
    {id: 'link', label: 'Copy Link', icon: 'Link', description: 'Copy shareable link'},
    {id: 'slack', label: 'Slack', icon: 'MessageSquare', description: 'Share to Slack'},
    {id: 'teams', label: 'Teams', icon: 'Users', description: 'Share to Microsoft Teams'}
  ];

  const downloadFormats = [
    {
      id: 'pdf',
      label: 'PDF Document',
      icon: 'FileText',
      description: 'High-quality PDF for presentations'
    },
    {id: 'docx', label: 'Word Document', icon: 'FileText', description: 'Editable Word format'},
    {id: 'png', label: 'Image Export', icon: 'Image', description: 'High-resolution images'}
  ];

  return (
      <div className={`space-y-4 ${className}`}>
        {/* Primary Actions */}
        <div className="space-y-3">
          <Button
              variant="default"
              size="lg"
              fullWidth
              loading={isDownloading}
              onClick={() => handleDownload('pdf')}
              iconName="Download"
              iconPosition="left"
              className="bg-accent hover:bg-accent/90 text-accent-foreground shadow-warm-md hover:shadow-warm-lg transition-all duration-200"
          >
            {isDownloading ? 'Preparing Download...' : 'Download PDF Treatment'}
          </Button>

          <div className="grid grid-cols-2 gap-2">
            <Button
                variant="outline"
                size="default"
                onClick={onGenerateNew}
                iconName="Plus"
                iconPosition="left"
                className="text-sm"
            >
              New Treatment
            </Button>

            <div className="relative">
              <Button
                  variant="outline"
                  size="default"
                  onClick={() => setShareMenuOpen(!shareMenuOpen)}
                  iconName="Share"
                  iconPosition="left"
                  className="w-full text-sm"
              >
                Share
              </Button>

              {shareMenuOpen && (
                  <div
                      className="absolute top-full left-0 right-0 mt-2 bg-popover border border-border rounded-lg shadow-warm-lg z-50 animate-scale-in">
                    <div className="p-2">
                      {shareOptions?.map((option) => (
                          <button
                              key={option?.id}
                              onClick={() => handleShare(option?.id)}
                              className="w-full flex items-center space-x-3 px-3 py-2 text-left hover:bg-muted/50 rounded-md transition-colors duration-200 group"
                          >
                            <Icon name={option?.icon} size={16}
                                  className="text-muted-foreground group-hover:text-foreground"/>
                            <div className="flex-1 min-w-0">
                              <p className="text-sm font-medium text-foreground">{option?.label}</p>
                              <p className="text-xs text-muted-foreground">{option?.description}</p>
                            </div>
                          </button>
                      ))}
                    </div>
                  </div>
              )}
            </div>
          </div>
        </div>
        {/* Download Format Options */}
        <div className="bg-muted/20 border border-border rounded-lg p-3">
          <div className="flex items-center space-x-2 mb-3">
            <Icon name="Download" size={16} color="var(--color-muted-foreground)"/>
            <span className="text-sm font-medium text-foreground">Download Formats</span>
          </div>

          <div className="space-y-2">
            {downloadFormats?.map((format) => (
                <button
                    key={format?.id}
                    onClick={() => handleDownload(format?.id)}
                    disabled={isDownloading}
                    className="w-full flex items-center space-x-3 p-2 text-left hover:bg-muted/50 rounded-md transition-colors duration-200 group disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Icon name={format?.icon} size={16}
                        className="text-muted-foreground group-hover:text-foreground"/>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground">{format?.label}</p>
                    <p className="text-xs text-muted-foreground">{format?.description}</p>
                  </div>
                  <Icon name="ChevronRight" size={14}
                        className="text-muted-foreground group-hover:text-accent"/>
                </button>
            ))}
          </div>
        </div>
        {/* Secondary Actions */}
        <div className="space-y-2">
          <Button
              variant="ghost"
              size="default"
              fullWidth
              iconName="Edit"
              iconPosition="left"
              className="text-muted-foreground hover:text-foreground justify-start"
          >
            Request Modifications
          </Button>

          <Button
              variant="ghost"
              size="default"
              fullWidth
              iconName="RefreshCw"
              iconPosition="left"
              className="text-muted-foreground hover:text-foreground justify-start"
          >
            Regenerate Treatment
          </Button>

          <Button
              variant="ghost"
              size="default"
              fullWidth
              iconName="Save"
              iconPosition="left"
              className="text-muted-foreground hover:text-foreground justify-start"
          >
            Save to Library
          </Button>
        </div>
        {/* Quick Stats */}
        <div className="bg-card border border-border rounded-lg p-3">
          <div className="flex items-center space-x-2 mb-2">
            <Icon name="BarChart3" size={16} color="var(--color-muted-foreground)"/>
            <span className="text-sm font-medium text-foreground">Quick Stats</span>
          </div>

          <div className="grid grid-cols-2 gap-3 text-xs">
            <div className="text-center">
              <p className="text-muted-foreground">Generation Time</p>
              <p className="font-mono font-medium text-foreground">2m 34s</p>
            </div>
            <div className="text-center">
              <p className="text-muted-foreground">Quality Score</p>
              <p className="font-mono font-medium text-success">95%</p>
            </div>
            <div className="text-center">
              <p className="text-muted-foreground">Word Count</p>
              <p className="font-mono font-medium text-foreground">2,847</p>
            </div>
            <div className="text-center">
              <p className="text-muted-foreground">Scenes</p>
              <p className="font-mono font-medium text-foreground">8</p>
            </div>
          </div>
        </div>
        {/* Help Section */}
        <div className="bg-muted/10 border border-border rounded-lg p-3">
          <div className="flex items-center space-x-2 mb-2">
            <Icon name="HelpCircle" size={16} color="var(--color-muted-foreground)"/>
            <span className="text-sm font-medium text-foreground">Need Help?</span>
          </div>

          <div className="space-y-2">
            <button
                className="w-full flex items-center space-x-2 text-left text-xs text-muted-foreground hover:text-foreground transition-colors duration-200">
              <Icon name="Book" size={14}/>
              <span>Treatment Guidelines</span>
            </button>
            <button
                className="w-full flex items-center space-x-2 text-left text-xs text-muted-foreground hover:text-foreground transition-colors duration-200">
              <Icon name="MessageCircle" size={14}/>
              <span>Contact Support</span>
            </button>
            <button
                className="w-full flex items-center space-x-2 text-left text-xs text-muted-foreground hover:text-foreground transition-colors duration-200">
              <Icon name="Video" size={14}/>
              <span>Video Tutorial</span>
            </button>
          </div>
        </div>
      </div>
  );
};

export default ActionPanel;