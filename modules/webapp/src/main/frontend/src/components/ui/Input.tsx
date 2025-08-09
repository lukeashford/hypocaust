import React from "react";
import {cn} from "utils/cn";

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  className?: string;
  type?: string;
  label?: string;
  description?: string;
  error?: string;
  required?: boolean;
  id?: string;
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(({
  className,
  type = "text",
  label,
  description,
  error,
  required = false,
  id,
  ...props
}, ref) => {
  // Generate unique ID if not provided
  const inputId = id || `input-${Math.random()?.toString(36)?.substr(2, 9)}`;

  // Base input classes with Nolan aesthetic
  const baseInputClasses = "flex h-12 w-full border border-border bg-background px-4 py-3 text-sm font-light ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-light placeholder:text-muted-foreground placeholder:font-light focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-1 focus-visible:border-foreground/30 disabled:cursor-not-allowed disabled:opacity-50 transition-all duration-300";

  // Checkbox-specific styles
  if (type === "checkbox") {
    return (
        <input
            type="checkbox"
            className={cn(
                "h-4 w-4 border border-border bg-background text-primary focus:ring-1 focus:ring-ring focus:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-50 transition-all duration-300",
                className
            )}
            ref={ref}
            id={inputId}
            {...props}
        />
    );
  }

  // Radio button-specific styles
  if (type === "radio") {
    return (
        <input
            type="radio"
            className={cn(
                "h-4 w-4 border border-border bg-background text-primary focus:ring-1 focus:ring-ring focus:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-50 transition-all duration-300",
                className
            )}
            ref={ref}
            id={inputId}
            {...props}
        />
    );
  }

  // For regular inputs with wrapper structure
  return (
      <div className="space-y-3">
        {label && (
            <label
                htmlFor={inputId}
                className={cn(
                    "text-sm font-light leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 tracking-wide",
                    error ? "text-destructive" : "text-foreground"
                )}
            >
              {label}
              {required && <span className="text-destructive ml-1">*</span>}
            </label>
        )}

        <input
            type={type}
            className={cn(
                baseInputClasses,
                error
                && "border-destructive focus-visible:ring-destructive focus-visible:border-destructive",
                className
            )}
            ref={ref}
            id={inputId}
            {...props}
        />

        {description && !error && (
            <p className="text-xs text-muted-foreground font-light">
              {description}
            </p>
        )}

        {error && (
            <p className="text-xs text-destructive font-light">
              {error}
            </p>
        )}
      </div>
  );
});

Input.displayName = "Input";

export default Input;