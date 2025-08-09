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
    inline = 'nearest'
  } = options;

  // Auto-scroll to bottom when dependencies change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({
      behavior,
      block,
      inline
    });
  }, dependencies);

  return {
    messagesEndRef
  };
};