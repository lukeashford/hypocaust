import React from 'react';
import {useNavigate} from 'react-router-dom';
import Icon from '../../../components/AppIcon';

const BreadcrumbNavigation = ({
  className = ""
}) => {
  const navigate = useNavigate();

  const breadcrumbItems = [
    {
      label: "Home",
      path: "/chat-interface-main-application-screen",
      icon: "Home",
      isClickable: true
    },
    {
      label: "Chat",
      path: "/chat-interface-main-application-screen",
      icon: "MessageSquare",
      isClickable: true
    },
    {
      label: "Treatment Complete",
      path: null,
      icon: "CheckCircle",
      isClickable: false
    },
    {
      label: "Preview",
      path: "/pdf-preview-and-download-screen",
      icon: "Eye",
      isClickable: false,
      isCurrent: true
    }
  ];

  const handleNavigation = (path) => {
    if (path) {
      navigate(path);
    }
  };

  return (
      <nav className={`bg-muted/20 border border-border rounded-lg p-3 ${className}`}
           aria-label="Breadcrumb">
        <div className="flex items-center space-x-2 overflow-x-auto">
          {breadcrumbItems?.map((item, index) => (
              <React.Fragment key={index}>
                <div className="flex items-center space-x-2 flex-shrink-0">
                  {item?.isClickable ? (
                      <button
                          onClick={() => handleNavigation(item?.path)}
                          className="flex items-center space-x-2 px-2 py-1 rounded-md text-sm font-medium transition-colors duration-200 hover:bg-muted/50 group"
                      >
                        <Icon
                            name={item?.icon}
                            size={14}
                            className="text-muted-foreground group-hover:text-accent"
                        />
                        <span className="text-muted-foreground group-hover:text-foreground">
                    {item?.label}
                  </span>
                      </button>
                  ) : (
                      <div
                          className={`flex items-center space-x-2 px-2 py-1 rounded-md text-sm font-medium ${
                              item?.isCurrent
                                  ? 'bg-accent/10 border border-accent/20' : ''
                          }`}>
                        <Icon
                            name={item?.icon}
                            size={14}
                            color={item?.isCurrent ? 'var(--color-accent)' : 'var(--color-success)'}
                        />
                        <span className={
                          item?.isCurrent
                              ? 'text-accent font-semibold' : 'text-success'
                        }>
                    {item?.label}
                  </span>
                      </div>
                  )}
                </div>

                {index < breadcrumbItems?.length - 1 && (
                    <Icon
                        name="ChevronRight"
                        size={14}
                        className="text-muted-foreground flex-shrink-0"
                    />
                )}
              </React.Fragment>
          ))}
        </div>
        {/* Progress Indicator */}
        <div className="mt-3 pt-3 border-t border-border">
          <div className="flex items-center justify-between text-xs text-muted-foreground mb-2">
            <span>Treatment Generation Progress</span>
            <span>100% Complete</span>
          </div>
          <div className="w-full bg-muted rounded-full h-1.5">
            <div className="bg-success h-1.5 rounded-full transition-all duration-500"
                 style={{width: '100%'}}/>
          </div>
        </div>
        {/* Status Message */}
        <div className="mt-3 flex items-center space-x-2">
          <Icon name="CheckCircle" size={16} color="var(--color-success)"/>
          <p className="text-sm font-medium text-success">
            Treatment generation completed successfully
          </p>
        </div>
      </nav>
  );
};

export default BreadcrumbNavigation;