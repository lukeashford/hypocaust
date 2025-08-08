import tailwindcssAnimate from "tailwindcss-animate";

/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: [
    './pages/**/*.{js,jsx}',
    './components/**/*.{js,jsx}',
    './app/**/*.{js,jsx}',
    './src/**/*.{js,jsx}',
  ],
  prefix: "",
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {
      colors: {
        border: "var(--color-border)",
        input: "var(--color-input)",
        ring: "var(--color-ring)",
        background: "var(--color-background)",
        foreground: "var(--color-foreground)",
        primary: {
          DEFAULT: "var(--color-primary)",
          foreground: "var(--color-primary-foreground)",
        },
        secondary: {
          DEFAULT: "var(--color-secondary)",
          foreground: "var(--color-secondary-foreground)",
        },
        destructive: {
          DEFAULT: "var(--color-destructive)",
          foreground: "var(--color-destructive-foreground)",
        },
        muted: {
          DEFAULT: "var(--color-muted)",
          foreground: "var(--color-muted-foreground)",
        },
        accent: {
          DEFAULT: "var(--color-accent)",
          foreground: "var(--color-accent-foreground)",
        },
        popover: {
          DEFAULT: "var(--color-popover)",
          foreground: "var(--color-popover-foreground)",
        },
        card: {
          DEFAULT: "var(--color-card)",
          foreground: "var(--color-card-foreground)",
        },
        success: {
          DEFAULT: "var(--color-success)",
          foreground: "var(--color-success-foreground)",
        },
        warning: {
          DEFAULT: "var(--color-warning)",
          foreground: "var(--color-warning-foreground)",
        },
        error: {
          DEFAULT: "var(--color-error)",
          foreground: "var(--color-error-foreground)",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "var(--radius)",
        sm: "var(--radius)",
        none: "0px",
      },
      fontFamily: {
        heading: ['Inter', 'sans-serif'],
        body: ['Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      fontSize: {
        'xs': ['0.75rem', {lineHeight: '1.1', fontWeight: '200'}],
        'sm': ['0.875rem', {lineHeight: '1.2', fontWeight: '200'}],
        'base': ['1rem', {lineHeight: '1.4', fontWeight: '200'}],
        'lg': ['1.125rem', {lineHeight: '1.4', fontWeight: '300'}],
        'xl': ['1.25rem', {lineHeight: '1.4', fontWeight: '300'}],
        '2xl': ['1.5rem', {lineHeight: '1.3', fontWeight: '300'}],
        '3xl': ['1.875rem', {lineHeight: '1.2', fontWeight: '300'}],
        '4xl': ['2.25rem', {lineHeight: '1.1', fontWeight: '300'}],
        '5xl': ['3rem', {lineHeight: '1', fontWeight: '300'}],
        '6xl': ['3.75rem', {lineHeight: '1', fontWeight: '300'}],
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '128': '32rem',
      },
      animation: {
        "scale-in": "scale-in 0.4s cubic-bezier(0.16, 1, 0.3, 1)",
        "fade-in": "fade-in 0.6s cubic-bezier(0.16, 1, 0.3, 1)",
        "slide-up": "slide-up 0.5s cubic-bezier(0.16, 1, 0.3, 1)",
        "pulse-slow": "pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite",
      },
      keyframes: {
        "scale-in": {
          "0%": {
            opacity: "0",
            transform: "scale(0.98)",
          },
          "100%": {
            opacity: "1",
            transform: "scale(1)",
          },
        },
        "fade-in": {
          "0%": {
            opacity: "0",
          },
          "100%": {
            opacity: "1",
          },
        },
        "slide-up": {
          "0%": {
            opacity: "0",
            transform: "translateY(8px)",
          },
          "100%": {
            opacity: "1",
            transform: "translateY(0)",
          },
        },
      },
      backdropBlur: {
        xs: '2px',
      },
      boxShadow: {
        'warm-sm': '0 1px 3px 0 rgba(0, 0, 0, 0.2)',
        'warm': '0 2px 4px 0 rgba(0, 0, 0, 0.2)',
        'warm-md': '0 4px 8px -1px rgba(0, 0, 0, 0.2)',
        'warm-lg': '0 8px 16px -3px rgba(0, 0, 0, 0.2)',
        'warm-xl': '0 16px 32px -5px rgba(0, 0, 0, 0.2)',
        'warm-2xl': '0 24px 48px -12px rgba(0, 0, 0, 0.2)',
        'none': 'none',
      },
    },
  },
  plugins: [
    tailwindcssAnimate,
  ],
}