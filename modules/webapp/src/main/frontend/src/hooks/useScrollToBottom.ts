import {useEffect, useRef} from 'react';

// Return type for the useScrollToBottom hook
export interface UseScrollToBottomReturn {
  messagesEndRef: React.RefObject<HTMLDivElement>;
}

// Options interface for customizing scroll behavior
export interface UseScrollToBottomOptions {
  behavior?: ScrollBehavior;
  block?: ScrollLogicalPosition;
  inline?: ScrollLogicalPosition;
  bottomOffset?: number; // Offset from bottom to account for fixed elements
}

/**
 * Custom hook for auto-scrolling to the bottom of a container when dependencies change
 *
 * @param dependencies - Array of dependencies that trigger the scroll behavior (e.g., messages)
 * @param options - Optional scroll behavior configuration
 * @returns Object containing the ref to attach to the scroll target element
 *
 * @example
 * ```tsx
 * const { messagesEndRef } = useScrollToBottom([messages]);
 *
 * return (
 *   <div>
 *     {messages.map(message => <Message key={message.id} {...message} />)}
 *     <div ref={messagesEndRef} />
 *   </div>
 * );
 * ```
 */
export const useScrollToBottom = (
    dependencies: React.DependencyList,
    options: UseScrollToBottomOptions = {}
): UseScrollToBottomReturn => {
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const {
    behavior = 'smooth',
    block = 'nearest',
    inline = 'nearest',
    bottomOffset = 0
  } = options;

  // Auto-scroll to bottom when dependencies change
  useEffect(() => {
    if (messagesEndRef.current) {
      // If bottomOffset is provided, use custom scroll positioning
      if (bottomOffset > 0) {
        const element = messagesEndRef.current;
        const elementRect = element.getBoundingClientRect();
        const targetPosition = window.pageYOffset + elementRect.top - window.innerHeight
            + bottomOffset;

        window.scrollTo({
          top: Math.max(0, targetPosition),
          behavior: behavior as ScrollBehavior
        });
      } else {
        // Fallback to standard scrollIntoView for backward compatibility
        messagesEndRef.current.scrollIntoView({
          behavior,
          block,
          inline
        });
      }
    }
  }, dependencies);

  return {
    messagesEndRef
  };
};