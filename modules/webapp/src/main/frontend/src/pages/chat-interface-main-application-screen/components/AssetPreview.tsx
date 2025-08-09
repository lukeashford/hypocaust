import React, {useState} from 'react';
import Icon from '../../../components/AppIcon';
import Image from '../../../components/AppImage';
import Button from '../../../components/ui/Button';

interface AssetType {
  type: 'image' | 'video' | 'document';
  url?: string;
  title?: string;
  description?: string;
  pages?: number;
  duration?: string;
}

interface AssetPreviewProps {
  assets?: AssetType[];
  onAssetClick?: ((asset: AssetType, index: number) => void) | null;
  onDownload?: ((assets: AssetType[]) => void) | null;
  className?: string;
}

const AssetPreview: React.FC<AssetPreviewProps> = ({
  assets = [],
  onAssetClick = null,
  onDownload = null,
  className = ""
}) => {
  const [selectedAsset, setSelectedAsset] = useState<AssetType | null>(null);
  const [currentIndex, setCurrentIndex] = useState<number>(0);

  const handleAssetClick = (asset: AssetType, index: number) => {
    setSelectedAsset(asset);
    setCurrentIndex(index);
    if (onAssetClick) {
      onAssetClick(asset, index);
    }
  };

  const handlePrevious = () => {
    const newIndex = currentIndex > 0 ? currentIndex - 1 : assets.length - 1;
    setCurrentIndex(newIndex);
    setSelectedAsset(assets[newIndex]);
  };

  const handleNext = () => {
    const newIndex = currentIndex < assets.length - 1 ? currentIndex + 1 : 0;
    setCurrentIndex(newIndex);
    setSelectedAsset(assets[newIndex]);
  };

  const closeModal = () => {
    setSelectedAsset(null);
  };

  if (!assets || assets.length === 0) {
    return null;
  }

  const renderAssetThumbnail = (asset: AssetType, index: number) => {
    const commonClasses = "relative group cursor-pointer rounded-lg overflow-hidden border border-border hover:border-accent/50 transition-all duration-200 hover:shadow-warm-md";

    switch (asset?.type) {
      case 'image':
        return (
            <div key={index} className={commonClasses}
                 onClick={() => handleAssetClick(asset, index)}>
              <div className="aspect-video overflow-hidden">
                <Image
                    src={asset?.url}
                    alt={asset?.title || `Asset ${index + 1}`}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                />
              </div>
              <div
                  className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                <div className="absolute bottom-0 left-0 right-0 p-3">
                  <p className="text-white text-sm font-medium truncate">{asset?.title}</p>
                  {asset?.description && (
                      <p className="text-white/80 text-xs mt-1 line-clamp-2">{asset?.description}</p>
                  )}
                </div>
              </div>
              <div
                  className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                <div className="bg-black/50 rounded-full p-1">
                  <Icon name="ZoomIn" size={14} color="white"/>
                </div>
              </div>
            </div>
        );

      case 'video':
        return (
            <div key={index} className={commonClasses}
                 onClick={() => handleAssetClick(asset, index)}>
              <div className="aspect-video bg-muted flex items-center justify-center">
                <div className="text-center">
                  <Icon name="Play" size={32} color="var(--color-accent)"/>
                  <p className="text-sm font-medium text-foreground mt-2">{asset?.title}</p>
                  <p className="text-xs text-muted-foreground">{asset?.duration || '0:30'}</p>
                </div>
              </div>
            </div>
        );

      case 'document':
        return (
            <div key={index} className={commonClasses}
                 onClick={() => handleAssetClick(asset, index)}>
              <div className="aspect-video bg-muted/30 flex items-center justify-center p-4">
                <div className="text-center">
                  <Icon name="FileText" size={32} color="var(--color-accent)"/>
                  <p className="text-sm font-medium text-foreground mt-2">{asset?.title}</p>
                  <p className="text-xs text-muted-foreground">{asset?.pages} pages</p>
                </div>
              </div>
            </div>
        );

      default:
        return (
            <div key={index} className={commonClasses}>
              <div className="aspect-video bg-muted flex items-center justify-center">
                <Icon name="File" size={32} color="var(--color-muted-foreground)"/>
              </div>
            </div>
        );
    }
  };

  return (
      <>
        <div className={`space-y-4 ${className}`}>
          {/* Header */}
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Icon name="Image" size={18} color="var(--color-accent)"/>
              <h3 className="text-sm font-heading font-semibold text-foreground">
                Generated Assets ({assets.length})
              </h3>
            </div>

            {onDownload && (
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onDownload(assets)}
                    iconName="Download"
                    iconPosition="left"
                    className="text-muted-foreground hover:text-foreground"
                >
                  Download All
                </Button>
            )}
          </div>

          {/* Asset Grid */}
          <div className={`grid gap-3 ${
              assets.length === 1
                  ? 'grid-cols-1'
                  : assets.length === 2
                      ? 'grid-cols-2' : 'grid-cols-2 md:grid-cols-3'
          }`}>
            {assets.map((asset, index) => renderAssetThumbnail(asset, index))}
          </div>

          {/* Asset Count */}
          {assets.length > 6 && (
              <div className="text-center">
                <Button
                    variant="ghost"
                    size="sm"
                    iconName="MoreHorizontal"
                    className="text-muted-foreground hover:text-foreground"
                >
                  View All {assets.length} Assets
                </Button>
              </div>
          )}
        </div>
        {/* Modal for Asset Preview */}
        {selectedAsset && (
            <div
                className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
              <div
                  className="relative max-w-4xl max-h-[90vh] w-full bg-card rounded-lg overflow-hidden shadow-warm-2xl">
                {/* Modal Header */}
                <div className="flex items-center justify-between p-4 border-b border-border">
                  <div className="flex items-center space-x-3">
                    <Icon name={selectedAsset?.type === 'image' ? 'Image' : 'File'} size={18}
                          color="var(--color-accent)"/>
                    <div>
                      <h3 className="text-lg font-heading font-semibold text-foreground">
                        {selectedAsset?.title}
                      </h3>
                      {selectedAsset?.description && (
                          <p className="text-sm text-muted-foreground">{selectedAsset?.description}</p>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center space-x-2">
                    {assets.length > 1 && (
                        <>
                          <Button
                              variant="ghost"
                              size="sm"
                              onClick={handlePrevious}
                              iconName="ChevronLeft"
                              className="w-8 h-8"
                          />
                          <span className="text-sm text-muted-foreground font-mono">
                      {currentIndex + 1} / {assets.length}
                    </span>
                          <Button
                              variant="ghost"
                              size="sm"
                              onClick={handleNext}
                              iconName="ChevronRight"
                              className="w-8 h-8"
                          />
                        </>
                    )}

                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={closeModal}
                        iconName="X"
                        className="w-8 h-8"
                    />
                  </div>
                </div>

                {/* Modal Content */}
                <div className="p-4">
                  {selectedAsset?.type === 'image' && (
                      <div className="flex justify-center">
                        <Image
                            src={selectedAsset?.url}
                            alt={selectedAsset?.title}
                            className="max-w-full max-h-[60vh] object-contain rounded-lg"
                        />
                      </div>
                  )}

                  {selectedAsset?.type === 'document' && (
                      <div className="text-center py-8">
                        <Icon name="FileText" size={64} color="var(--color-accent)"
                              className="mx-auto mb-4"/>
                        <p className="text-lg font-medium text-foreground mb-2">{selectedAsset?.title}</p>
                        <p className="text-muted-foreground mb-4">{selectedAsset?.pages} pages</p>
                        <Button
                            variant="default"
                            onClick={() => onDownload && onDownload([selectedAsset])}
                            iconName="Download"
                            iconPosition="left"
                            className="bg-accent hover:bg-accent/90"
                        >
                          Download Document
                        </Button>
                      </div>
                  )}
                </div>
              </div>
            </div>
        )}
      </>
  );
};

export default AssetPreview;