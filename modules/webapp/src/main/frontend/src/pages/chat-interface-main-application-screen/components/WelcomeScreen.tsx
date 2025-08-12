import React from 'react';
import Icon from '../../../components/AppIcon';
import Button from '../../../components/ui/Button';

interface Feature {
  icon: string;
  title: string;
  description: string;
}

type GenerationMode = 'interactive' | 'oneshot';

interface WelcomeScreenProps {
  onGetStarted?: ((brandName?: string | null) => void) | null;
  onModeSelect?: ((mode: GenerationMode) => void) | null;
}

const WelcomeScreen: React.FC<WelcomeScreenProps> = ({
  onGetStarted = null,
  onModeSelect = null
}) => {
  const features: Feature[] = [
    {
      icon: 'Search',
      title: 'AI Company Research',
      description: 'Automatically analyze brand identity, website content, and social media presence'
    },
    {
      icon: 'BookOpen',
      title: 'Cinematic Story Generation',
      description: 'Create compelling narrative outlines tailored to your brand and target audience'
    },
    {
      icon: 'Users',
      title: 'Character & Costume Design',
      description: 'Generate detailed character concepts with professional costume and styling notes'
    },
    {
      icon: 'Image',
      title: 'Visual Asset Creation',
      description: 'Produce high-quality video stills and visual concepts for your treatment'
    },
    {
      icon: 'FileText',
      title: 'Professional PDF Export',
      description: 'Download industry-standard director\'s treatments ready for client presentations'
    },
    {
      icon: 'Zap',
      title: 'Interactive or One-Shot',
      description: 'Choose between guided step-by-step creation or fully automated generation'
    }
  ];

  const exampleBrands: string[] = [
    "TechFlow Solutions",
    "Artisan Coffee Co.",
    "EcoGreen Innovations",
    "Urban Fitness Studio",
    "Luxury Travel Concierge"
  ];

  return (
      <div className="max-w-5xl mx-auto px-6 py-12">
        {/* Hero Section */}
        <div className="text-center mb-16">
          <div className="flex items-center justify-center mb-8">
            <div className="relative">
              <div
                  className="w-20 h-20 bg-muted border golden-border flex items-center justify-center shadow-warm-lg">
                <Icon name="Film" size={40} color="var(--color-golden-primary)" strokeWidth={1.5}/>
              </div>
            </div>
          </div>

          <h1 className="text-5xl md:text-6xl font-light text-foreground mb-6 tracking-wide">
            CinematicBrand Director
          </h1>

          <p className="text-xl text-muted-foreground mb-8 max-w-3xl mx-auto leading-relaxed font-light">
            Transform any brand name into a comprehensive director's treatment for cinematic
            marketing videos.
            Powered by AI agents that research, analyze, and create professional-grade treatments.
          </p>

          <div className="flex items-center justify-center space-x-8 mb-10">
            <div className="flex items-center space-x-2 text-sm text-muted-foreground font-light">
              <div className="w-2 h-2 golden-bg"></div>
              <span>Industry Standard</span>
            </div>
            <div className="flex items-center space-x-2 text-sm text-muted-foreground font-light">
              <div className="w-2 h-2 golden-bg"></div>
              <span>AI-Powered</span>
            </div>
            <div className="flex items-center space-x-2 text-sm text-muted-foreground font-light">
              <div className="w-2 h-2 golden-bg"></div>
              <span>Instant Download</span>
            </div>
          </div>
        </div>

        {/* Mode Selection */}
        <div className="grid md:grid-cols-2 gap-8 mb-16">
          <div
              className="bg-card border border-border p-8 golden-hover transition-all duration-300 hover:shadow-warm-md group cursor-pointer"
              onClick={() => onModeSelect && onModeSelect('interactive')}>
            <div className="flex items-center space-x-4 mb-6">
              <div
                  className="w-12 h-12 bg-muted border golden-border flex items-center justify-center transition-colors duration-300">
                <Icon name="MessageCircle" size={24} color="var(--color-golden-primary)"
                      strokeWidth={1.5}/>
              </div>
              <h3 className="text-xl font-light text-foreground">Interactive Mode</h3>
            </div>
            <p className="text-muted-foreground mb-6 font-light leading-relaxed">
              Get feedback prompts at each step. Review and refine company research, story concepts,
              character designs, and visual assets before moving to the next phase.
            </p>
            <div className="flex items-center text-sm text-foreground font-light">
              <span>Choose Interactive</span>
              <Icon name="ArrowRight" size={16} strokeWidth={1.5}
                    className="ml-2 group-hover:translate-x-1 transition-transform duration-300"/>
            </div>
          </div>

          <div
              className="bg-card border border-border p-8 golden-hover transition-all duration-300 hover:shadow-warm-md group cursor-pointer"
              onClick={() => onModeSelect && onModeSelect('oneshot')}>
            <div className="flex items-center space-x-4 mb-6">
              <div
                  className="w-12 h-12 bg-muted border golden-border flex items-center justify-center transition-colors duration-300">
                <Icon name="Zap" size={24} color="var(--color-golden-primary)" strokeWidth={1.5}/>
              </div>
              <h3 className="text-xl font-light text-foreground">One-Shot Mode</h3>
            </div>
            <p className="text-muted-foreground mb-6 font-light leading-relaxed">
              Fully automated generation. AI agents work through all steps autonomously to deliver
              a complete director's treatment without interruption.
            </p>
            <div className="flex items-center text-sm text-foreground font-light">
              <span>Choose One-Shot</span>
              <Icon name="ArrowRight" size={16} strokeWidth={1.5}
                    className="ml-2 group-hover:translate-x-1 transition-transform duration-300"/>
            </div>
          </div>
        </div>

        {/* Features Grid */}
        <div className="mb-16">
          <h2 className="text-3xl font-light text-foreground text-center mb-12 tracking-wide">
            Comprehensive Treatment Generation
          </h2>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features?.map((feature, index) => (
                <div key={index}
                     className="bg-card border border-border p-6 hover:shadow-warm-md transition-all duration-300 hover:border-foreground/20">
                  <div className="flex items-center space-x-3 mb-4">
                    <div
                        className="w-10 h-10 bg-muted border border-border flex items-center justify-center">
                      <Icon name={feature?.icon} size={20} color="var(--color-foreground)"
                            strokeWidth={1.5}/>
                    </div>
                    <h3 className="font-light text-foreground text-lg">{feature?.title}</h3>
                  </div>
                  <p className="text-sm text-muted-foreground leading-relaxed font-light">{feature?.description}</p>
                </div>
            ))}
          </div>
        </div>

        {/* Example Brands */}
        <div className="bg-muted/20 border border-border p-8 mb-12">
          <h3 className="text-lg font-light text-foreground mb-6 text-center tracking-wide">
            Try These Example Brands
          </h3>
          <div className="flex flex-wrap justify-center gap-3">
            {exampleBrands?.map((brand, index) => (
                <button
                    key={index}
                    onClick={() => onGetStarted && onGetStarted(brand)}
                    className="px-6 py-3 bg-card border border-border text-sm text-foreground hover:border-foreground/30 hover:text-foreground transition-all duration-300 hover:shadow-warm-sm font-light"
                >
                  {brand}
                </button>
            ))}
          </div>
        </div>

        {/* CTA */}
        <div className="text-center">
          <Button
              variant="outline"
              size="xl"
              onClick={() => onGetStarted && onGetStarted()}
              iconName="Sparkles"
              iconPosition="left"
              className="border-foreground/30 hover:border-foreground hover:bg-muted/30 text-foreground shadow-warm-lg hover:shadow-warm-xl transition-all duration-300 px-12 py-4 text-lg font-light"
          >
            Start Creating Your Treatment
          </Button>

          <p className="text-sm text-muted-foreground mt-6 font-light">
            Enter any brand name to begin • No signup required • Instant results
          </p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-8 mt-16 pt-12 border-t border-border">
          <div className="text-center">
            <div className="text-3xl font-light text-foreground mb-2">2.4s</div>
            <div className="text-sm text-muted-foreground font-light">Average Generation Time</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-light text-foreground mb-2">12</div>
            <div className="text-sm text-muted-foreground font-light">Pages Per Treatment</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-light text-foreground mb-2">100%</div>
            <div className="text-sm text-muted-foreground font-light">Industry Standard</div>
          </div>
        </div>
      </div>
  );
};

export default WelcomeScreen;