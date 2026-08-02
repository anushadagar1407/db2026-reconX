// TICKET-ADV124 — ThemeProvider: context flips data-theme; CSS owns colours.
import { createContext, useContext, useEffect, useState } from 'react';

const STORAGE_KEY = 'reconx-theme';
const DARK_MODE_QUERY = '(prefers-color-scheme: dark)';
const ThemeContext = createContext(null);

function isTheme(value) {
  return value === 'light' || value === 'dark';
}

function getInitialTheme() {
  if (typeof window === 'undefined') return 'light';

  try {
    const storedTheme = window.localStorage?.getItem(STORAGE_KEY);
    if (isTheme(storedTheme)) return storedTheme;
  } catch {
    // Continue with the system preference when storage is unavailable.
  }

  try {
    return typeof window.matchMedia === 'function' && window.matchMedia(DARK_MODE_QUERY).matches
      ? 'dark'
      : 'light';
  } catch {
    return 'light';
  }
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(getInitialTheme);

  useEffect(() => {
    if (typeof document !== 'undefined' && document.documentElement) {
      document.documentElement.dataset.theme = theme;
    }

    if (typeof window !== 'undefined') {
      try {
        window.localStorage?.setItem(STORAGE_KEY, theme);
      } catch {
        // A blocked store must not prevent the theme from applying.
      }
    }
  }, [theme]);

  const toggle = () => setTheme((currentTheme) => (currentTheme === 'light' ? 'dark' : 'light'));

  return (
    <ThemeContext.Provider value={{ theme, toggle }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);

  if (context === null) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }

  return context;
}
