import React from 'react';

interface ImageProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  src?: string;
  alt?: string;
  className?: string;
}

const Image: React.FC<ImageProps> = ({
  src,
  alt = "Image Name",
  className = "",
  ...props
}) => {

  return (
      <img
          src={src}
          alt={alt}
          className={className}
          onError={(e) => {
            const target = e.target as HTMLImageElement;
            target.src = "/assets/images/no_image.png";
          }}
          {...props}
      />
  );
};

export default Image;