import React, {useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import Icon from '../AppIcon';
import Button from './Button';

interface User {
  name?: string;
  email?: string;
}

interface NavigationItem {
  label: string;
  path: string;
  icon: string;
}

interface HeaderProps {
  user?: User | null;
  onNavigate?: ((path: string) => void) | null;
}

const Header: React.FC<HeaderProps> = ({user = null, onNavigate = null}) => {
  const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);
  const location = useLocation();
  const navigate = useNavigate();

  const handleNavigation = (path: string): void => {
    if (onNavigate) {
      onNavigate(path);
    } else {
      navigate(path);
    }
    setIsMenuOpen(false);
  };

  const navigationItems: NavigationItem[] = [
    {
      label: 'Treatment Generator',
      path: '/chat-interface-main-application-screen',
      icon: 'MessageSquare'
    },
    {
      label: 'PDF Preview',
      path: '/pdf-preview-and-download-screen',
      icon: 'FileText'
    }
  ];

  const isCurrentPath = (path: string): boolean => location.pathname === path;

  const Logo: React.FC = () => (
      <div
          className="flex items-center cursor-pointer group transition-all duration-300"
          onClick={() => handleNavigation('/chat-interface-main-application-screen')}
      >
        <div className="flex items-center space-x-4">
          <div className="relative">
            <div
                className="w-10 h-10 bg-muted border golden-border flex items-center justify-center golden-hover transition-all duration-300">
              <Icon name="Film" size={20} color="var(--color-golden-primary)" strokeWidth={1.5}/>
            </div>
          </div>
          <div className="flex flex-col">
          <span
              className="text-lg font-light text-foreground group-hover:text-foreground/80 transition-colors duration-300 tracking-wide">
            CinematicBrand
          </span>
            <span className="text-xs font-light text-muted-foreground -mt-1 tracking-wider">
            DIRECTOR
          </span>
          </div>
        </div>
      </div>
  );

  const UserMenu: React.FC = () => (
      <div className="relative">
        <Button
            variant="ghost"
            size="sm"
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            className="flex items-center space-x-2 hover:bg-muted/30 font-light"
            iconName="User"
            iconPosition="left"
            iconSize={16}
        >
        <span className="hidden sm:inline text-sm font-light">
          {user?.name || 'Account'}
        </span>
          <Icon name="ChevronDown" size={14} strokeWidth={1.5}
                className={`transition-transform duration-300 ${isMenuOpen ? 'rotate-180' : ''}`}/>
        </Button>

        {isMenuOpen && (
            <div
                className="absolute right-0 top-full mt-2 w-48 bg-card border border-border shadow-warm-lg z-50 animate-scale-in">
              <div className="p-2">
                <div className="px-3 py-2 border-b border-border mb-2">
                  <p className="text-sm font-light text-foreground">
                    {user?.name || 'Creative Professional'}
                  </p>
                  <p className="text-xs text-muted-foreground font-light">
                    {user?.email || 'user@example.com'}
                  </p>
                </div>

                <button
                    className="w-full flex items-center space-x-2 px-3 py-2 text-sm text-foreground hover:bg-muted/30 transition-colors duration-300 font-light">
                  <Icon name="Settings" size={16} strokeWidth={1.5}/>
                  <span>Settings</span>
                </button>

                <button
                    className="w-full flex items-center space-x-2 px-3 py-2 text-sm text-foreground hover:bg-muted/30 transition-colors duration-300 font-light">
                  <Icon name="HelpCircle" size={16} strokeWidth={1.5}/>
                  <span>Help & Support</span>
                </button>

                <div className="border-t border-border mt-2 pt-2">
                  <button
                      className="w-full flex items-center space-x-2 px-3 py-2 text-sm text-destructive hover:bg-destructive/10 transition-colors duration-300 font-light">
                    <Icon name="LogOut" size={16} strokeWidth={1.5}/>
                    <span>Sign Out</span>
                  </button>
                </div>
              </div>
            </div>
        )}
      </div>
  );

  const NavigationItems: React.FC = () => (
      <div className="hidden md:flex items-center space-x-6">
        {navigationItems.map((item) => (
            <button
                key={item.path}
                onClick={() => handleNavigation(item.path)}
                className={`flex items-center space-x-2 px-3 py-2 text-sm font-light transition-all duration-300 ${
                    isCurrentPath(item.path)
                        ? 'text-accent bg-accent/10 border border-accent/20'
                        : 'text-foreground hover:text-accent hover:bg-muted/30 border border-transparent'
                }`}
            >
              <Icon name={item.icon} size={16} strokeWidth={1.5}/>
              <span>{item.label}</span>
            </button>
        ))}
      </div>
  );

  const MobileMenu: React.FC = () => (
      <div className="md:hidden">
        <Button
            variant="ghost"
            size="sm"
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            className="p-2"
            iconName="Menu"
            iconSize={20}
        />

        {isMenuOpen && (
            <div
                className="absolute top-full left-0 right-0 bg-card border-b border-border shadow-warm-lg z-50 animate-slide-down">
              <div className="px-4 py-4 space-y-2">
                {navigationItems.map((item) => (
                    <button
                        key={item.path}
                        onClick={() => handleNavigation(item.path)}
                        className={`w-full flex items-center space-x-3 px-3 py-3 text-sm font-light transition-colors duration-300 ${
                            isCurrentPath(item.path)
                                ? 'text-accent bg-accent/10'
                                : 'text-foreground hover:text-accent hover:bg-muted/30'
                        }`}
                    >
                      <Icon name={item.icon} size={18} strokeWidth={1.5}/>
                      <span>{item.label}</span>
                    </button>
                ))}

                <div className="border-t border-border pt-4 mt-4">
                  <div className="px-3 py-2">
                    <p className="text-sm font-light text-foreground">
                      {user?.name || 'Creative Professional'}
                    </p>
                    <p className="text-xs text-muted-foreground font-light">
                      {user?.email || 'user@example.com'}
                    </p>
                  </div>

                  <button
                      className="w-full flex items-center space-x-3 px-3 py-3 text-sm text-foreground hover:bg-muted/30 transition-colors duration-300 font-light">
                    <Icon name="Settings" size={18} strokeWidth={1.5}/>
                    <span>Settings</span>
                  </button>

                  <button
                      className="w-full flex items-center space-x-3 px-3 py-3 text-sm text-foreground hover:bg-muted/30 transition-colors duration-300 font-light">
                    <Icon name="HelpCircle" size={18} strokeWidth={1.5}/>
                    <span>Help & Support</span>
                  </button>

                  <button
                      className="w-full flex items-center space-x-3 px-3 py-3 text-sm text-destructive hover:bg-destructive/10 transition-colors duration-300 font-light">
                    <Icon name="LogOut" size={18} strokeWidth={1.5}/>
                    <span>Sign Out</span>
                  </button>
                </div>
              </div>
            </div>
        )}
      </div>
  );

  return (
      <header className="h-16 bg-card border-b border-border sticky top-0 z-40">
        <div className="flex items-center justify-between h-full px-4 lg:px-6">
          <div className="flex items-center space-x-8">
            <Logo/>
            <NavigationItems/>
          </div>

          <div className="flex items-center space-x-4">
            <div className="hidden md:block">
              <UserMenu/>
            </div>
            <MobileMenu/>
          </div>
        </div>
      </header>
  );
};

export default Header;