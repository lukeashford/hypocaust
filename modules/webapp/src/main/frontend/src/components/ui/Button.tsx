import React from 'react';
import {Slot} from "@radix-ui/react-slot";
import {cva, VariantProps} from "class-variance-authority";
import {cn} from "../../utils/cn";
import Icon from '../AppIcon';

const buttonVariants = cva(
    "inline-flex items-center justify-center whitespace-nowrap text-sm font-light ring-offset-background transition-all duration-300 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-1 disabled:pointer-events-none disabled:opacity-50 border border-border hover:border-foreground/20",
    {
      variants: {
        variant: {
          default: "bg-primary text-primary-foreground hover:bg-primary/80 border-primary/20",
          destructive: "bg-destructive text-destructive-foreground hover:bg-destructive/80 border-destructive/20",
          outline: "border-border bg-transparent hover:bg-muted/30 hover:border-foreground/20",
          secondary: "bg-secondary text-secondary-foreground hover:bg-secondary/80 border-secondary/20",
          ghost: "border-transparent hover:bg-muted/30 hover:border-foreground/10",
          link: "text-primary underline-offset-4 hover:underline border-transparent",
          success: "bg-success text-success-foreground hover:bg-success/80 border-success/20",
          warning: "bg-warning text-warning-foreground hover:bg-warning/80 border-warning/20",
          danger: "bg-error text-error-foreground hover:bg-error/80 border-error/20",
        },
        size: {
          default: "h-10 px-6 py-2",
          sm: "h-9 px-4",
          lg: "h-12 px-8",
          icon: "h-10 w-10",
          xs: "h-8 px-3 text-xs",
          xl: "h-14 px-12 text-base",
        },
      },
      defaultVariants: {
        variant: "default",
        size: "default",
      },
    }
);

export interface ButtonProps
    extends React.ButtonHTMLAttributes<HTMLButtonElement>,
        VariantProps<typeof buttonVariants> {
  asChild?: boolean;
  loading?: boolean;
  iconName?: string | null;
  iconPosition?: 'left' | 'right';
  iconSize?: number | null;
  fullWidth?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(({
  className,
  variant,
  size,
  asChild = false,
  children,
  loading = false,
  iconName = null,
  iconPosition = 'left',
  iconSize = null,
  fullWidth = false,
  disabled = false,
  ...props
}, ref) => {
  const Comp = asChild ? Slot : "button";

  // Icon size mapping based on button size
  const iconSizeMap: Record<string, number> = {
    xs: 12,
    sm: 14,
    default: 16,
    lg: 18,
    xl: 20,
    icon: 16,
  };

  const calculatedIconSize = iconSize || iconSizeMap?.[size || 'default'] || 16;

  // Loading spinner with minimal aesthetic
  const LoadingSpinner = () => (
      <svg className="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                strokeWidth="1"/>
        <path className="opacity-75" fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"/>
      </svg>
  );

  // Icon rendering
  const renderIcon = () => {
    if (!iconName) {
      return null;
    }

    return (
        <Icon
            name={iconName}
            size={calculatedIconSize}
            strokeWidth={1.5}
            className={cn(
                children && iconPosition === 'left' && "mr-2",
                children && iconPosition === 'right' && "ml-2"
            )}
        />
    );
  };

  return (
      <Comp
          className={cn(
              buttonVariants({variant, size, className}),
              fullWidth && "w-full",
              "font-light tracking-wide"
          )}
          ref={ref}
          disabled={disabled || loading}
          {...props}
      >
        {loading && <LoadingSpinner/>}
        {iconName && iconPosition === 'left' && renderIcon()}
        {children}
        {iconName && iconPosition === 'right' && renderIcon()}
      </Comp>
  );
});

Button.displayName = "Button";

export default Button;