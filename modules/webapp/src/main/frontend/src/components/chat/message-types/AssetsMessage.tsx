import React from 'react';
import AssetPreview
  from '../../../pages/chat-interface-main-application-screen/components/AssetPreview';

interface AssetsMessageProps {
  content: string;
  assets: any[];
}

const AssetsMessage: React.FC<AssetsMessageProps> = ({
  content,
  assets
}) => {
  return (
      <div className="space-y-4">
        <p className="text-sm leading-relaxed text-foreground">{content}</p>
        <AssetPreview
            assets={assets}
            onAssetClick={(asset) => console.log('Asset clicked:', asset)}
            onDownload={(assets) => console.log('Download assets:', assets)}
        />
      </div>
  );
};

export default AssetsMessage;