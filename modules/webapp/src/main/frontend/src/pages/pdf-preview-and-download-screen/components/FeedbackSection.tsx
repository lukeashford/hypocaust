import React, {useState} from 'react';
import Icon from '../../../components/AppIcon';
import Button from '../../../components/ui/Button';
import Input from '../../../components/ui/Input';

interface FeedbackCategory {
  id: string;
  label: string;
  icon: string;
}

interface FeedbackData {
  rating: number;
  feedback: string;
  categories: string[];
  timestamp: string;
}

interface FeedbackSectionProps {
  onSubmitFeedback?: ((feedbackData: FeedbackData) => void) | null;
  className?: string;
}

interface RatingLabels {
  [key: number]: string;
}

const FeedbackSection: React.FC<FeedbackSectionProps> = ({
  onSubmitFeedback = null,
  className = ""
}) => {
  const [rating, setRating] = useState<number>(0);
  const [hoveredRating, setHoveredRating] = useState<number>(0);
  const [feedback, setFeedback] = useState<string>('');
  const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [isSubmitted, setIsSubmitted] = useState<boolean>(false);

  const feedbackCategories: FeedbackCategory[] = [
    {id: 'accuracy', label: 'Brand Accuracy', icon: 'Target'},
    {id: 'creativity', label: 'Creative Quality', icon: 'Lightbulb'},
    {id: 'formatting', label: 'Document Format', icon: 'FileText'},
    {id: 'completeness', label: 'Completeness', icon: 'CheckCircle'},
    {id: 'usability', label: 'Ease of Use', icon: 'Smile'}
  ];

  const handleRatingClick = (value: number): void => {
    setRating(value);
  };

  const handleCategoryToggle = (categoryId: string): void => {
    setSelectedCategories(prev =>
        prev?.includes(categoryId)
            ? prev?.filter(id => id !== categoryId)
            : [...prev, categoryId]
    );
  };

  const handleSubmit = async (): Promise<void> => {
    if (rating === 0) {
      return;
    }

    setIsSubmitting(true);

    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1500));

    if (onSubmitFeedback) {
      onSubmitFeedback({
        rating,
        feedback,
        categories: selectedCategories,
        timestamp: new Date()?.toISOString()
      });
    }

    setIsSubmitting(false);
    setIsSubmitted(true);

    // Reset form after 3 seconds
    setTimeout(() => {
      setIsSubmitted(false);
      setRating(0);
      setFeedback('');
      setSelectedCategories([]);
    }, 3000);
  };

  const getRatingLabel = (value: number): string => {
    const labels: RatingLabels = {
      1: 'Poor',
      2: 'Fair',
      3: 'Good',
      4: 'Very Good',
      5: 'Excellent'
    };
    return labels?.[value] || '';
  };

  if (isSubmitted) {
    return (
        <div className={`bg-success/10 border border-success/20 rounded-lg p-4 ${className}`}>
          <div className="text-center space-y-3">
            <Icon name="CheckCircle" size={32} color="var(--color-success)" className="mx-auto"/>
            <div>
              <h3 className="text-lg font-heading font-semibold text-success mb-1">
                Thank You!
              </h3>
              <p className="text-sm text-success/80">
                Your feedback helps us improve the treatment generation process
              </p>
            </div>
          </div>
        </div>
    );
  }

  return (
      <div className={`bg-card border border-border rounded-lg ${className}`}>
        {/* Header */}
        <div className="p-4 border-b border-border">
          <div className="flex items-center space-x-2">
            <Icon name="MessageSquare" size={18} color="var(--color-accent)"/>
            <h3 className="text-lg font-heading font-semibold text-foreground">
              Rate This Treatment
            </h3>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Help us improve by sharing your experience
          </p>
        </div>
        {/* Content */}
        <div className="p-4 space-y-6">
          {/* Star Rating */}
          <div className="space-y-3">
            <label className="text-sm font-medium text-foreground">
              Overall Rating
            </label>
            <div className="flex items-center space-x-1">
              {[1, 2, 3, 4, 5]?.map((star) => (
                  <button
                      key={star}
                      onClick={() => handleRatingClick(star)}
                      onMouseEnter={() => setHoveredRating(star)}
                      onMouseLeave={() => setHoveredRating(0)}
                      className="p-1 transition-colors duration-200"
                  >
                    <Icon
                        name="Star"
                        size={24}
                        color={
                          star <= (hoveredRating || rating)
                              ? 'var(--color-warning)'
                              : 'var(--color-muted-foreground)'
                        }
                        className={
                          star <= (hoveredRating || rating)
                              ? 'fill-current' : ''
                        }
                    />
                  </button>
              ))}
              {(hoveredRating || rating) > 0 && (
                  <span className="ml-2 text-sm font-medium text-foreground">
                {getRatingLabel(hoveredRating || rating)}
              </span>
              )}
            </div>
          </div>

          {/* Feedback Categories */}
          <div className="space-y-3">
            <label className="text-sm font-medium text-foreground">
              What aspects worked well? (Optional)
            </label>
            <div className="grid grid-cols-1 gap-2">
              {feedbackCategories?.map((category) => (
                  <button
                      key={category?.id}
                      onClick={() => handleCategoryToggle(category?.id)}
                      className={`flex items-center space-x-3 p-3 rounded-lg border transition-all duration-200 ${
                          selectedCategories?.includes(category?.id)
                              ? 'bg-accent/10 border-accent/20 text-accent'
                              : 'bg-muted/20 border-border text-foreground hover:bg-muted/30'
                      }`}
                  >
                    <Icon
                        name={category?.icon}
                        size={16}
                        color={
                          selectedCategories?.includes(category?.id)
                              ? 'var(--color-accent)'
                              : 'var(--color-muted-foreground)'
                        }
                    />
                    <span className="text-sm font-medium">{category?.label}</span>
                    {selectedCategories?.includes(category?.id) && (
                        <Icon name="Check" size={16} color="var(--color-accent)"
                              className="ml-auto"/>
                    )}
                  </button>
              ))}
            </div>
          </div>

          {/* Written Feedback */}
          <div className="space-y-3">
            <Input
                label="Additional Comments (Optional)"
                type="text"
                placeholder="Share any specific feedback or suggestions..."
                value={feedback}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFeedback(e?.target?.value)}
                description="Your feedback helps us improve the AI treatment generation"
                className="min-h-[80px] resize-none"
            />
          </div>

          {/* Submit Button */}
          <Button
              variant="default"
              size="default"
              fullWidth
              loading={isSubmitting}
              onClick={handleSubmit}
              disabled={rating === 0}
              iconName="Send"
              iconPosition="left"
              className="bg-accent hover:bg-accent/90 text-accent-foreground"
          >
            {isSubmitting ? 'Submitting Feedback...' : 'Submit Feedback'}
          </Button>

          {/* Privacy Note */}
          <div className="bg-muted/10 border border-border rounded-lg p-3">
            <div className="flex items-start space-x-2">
              <Icon name="Shield" size={16} color="var(--color-muted-foreground)"
                    className="mt-0.5"/>
              <div>
                <p className="text-xs text-muted-foreground leading-relaxed">
                  Your feedback is anonymous and helps improve our AI models. We don't store
                  personal information with your responses.
                </p>
              </div>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="border-t border-border pt-4">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Found an issue?</span>
              <button className="text-accent hover:text-accent/80 transition-colors duration-200">
                Report Bug
              </button>
            </div>
          </div>
        </div>
      </div>
  );
};

export default FeedbackSection;