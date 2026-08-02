import { StrictMode } from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ThemeProvider, useTheme } from '../ThemeContext.jsx';

const STORAGE_KEY = 'reconx-theme';
const initialLocalStorageDescriptor = Object.getOwnPropertyDescriptor(window, 'localStorage');
const initialMatchMediaDescriptor = Object.getOwnPropertyDescriptor(window, 'matchMedia');

function setMatchMedia(matches) {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: vi.fn(() => ({ matches })),
  });
}

function removeMatchMedia() {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: undefined,
  });
}

function removeLocalStorage() {
  Object.defineProperty(window, 'localStorage', {
    configurable: true,
    value: undefined,
  });
}

function restoreProperty(name, descriptor) {
  if (descriptor) {
    Object.defineProperty(window, name, descriptor);
  } else {
    delete window[name];
  }
}

function ThemeConsumer() {
  const { theme, toggle } = useTheme();

  return (
    <>
      <span data-testid="theme">{theme}</span>
      <button type="button" onClick={toggle}>Toggle theme</button>
    </>
  );
}

function ConsumerOutsideProvider() {
  useTheme();
  return null;
}

describe('ThemeContext', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    setMatchMedia(false);
  });

  afterEach(() => {
    cleanup();
    restoreProperty('localStorage', initialLocalStorageDescriptor);
    restoreProperty('matchMedia', initialMatchMediaDescriptor);
    vi.restoreAllMocks();
    window.localStorage?.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it.each(['light', 'dark'])('prefers valid stored theme %s over system preference', (storedTheme) => {
    window.localStorage.setItem(STORAGE_KEY, storedTheme);
    setMatchMedia(storedTheme === 'light');

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('theme')).toHaveTextContent(storedTheme);
    expect(document.documentElement.dataset.theme).toBe(storedTheme);
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe(storedTheme);
  });

  it('ignores an invalid stored theme and uses a dark system preference', () => {
    window.localStorage.setItem(STORAGE_KEY, 'sepia');
    setMatchMedia(true);

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('theme')).toHaveTextContent('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe('dark');
  });

  it.each([
    [true, 'dark'],
    [false, 'light'],
  ])('uses the system preference when storage is empty (%s)', (matches, expectedTheme) => {
    setMatchMedia(matches);

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('theme')).toHaveTextContent(expectedTheme);
    expect(document.documentElement.dataset.theme).toBe(expectedTheme);
  });

  it('falls back to light when matchMedia is unavailable or throws', () => {
    removeMatchMedia();

    const { unmount } = render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('theme')).toHaveTextContent('light');
    unmount();
    window.localStorage.clear();

    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: vi.fn(() => {
        throw new Error('matchMedia unavailable');
      }),
    });

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('theme')).toHaveTextContent('light');
  });

  it('falls back to the system preference when storage reads fail', () => {
    const getItem = vi.spyOn(window.Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('storage unavailable');
    });
    setMatchMedia(true);

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(getItem).toHaveBeenCalledWith(STORAGE_KEY);
    expect(screen.getByTestId('theme')).toHaveTextContent('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
  });

  it('works when localStorage is absent and when storage writes fail', () => {
    removeLocalStorage();
    setMatchMedia(true);

    const { unmount } = render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('theme')).toHaveTextContent('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');

    unmount();
    restoreProperty('localStorage', initialLocalStorageDescriptor);
    vi.spyOn(window.Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('storage unavailable');
    });

    expect(() => render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    )).not.toThrow();
    expect(screen.getByTestId('theme')).toHaveTextContent('dark');
  });

  it('passes through children and supports functional toggles in both directions', async () => {
    setMatchMedia(false);
    const user = userEvent.setup();

    render(
      <ThemeProvider>
        <p>Provider child</p>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByText('Provider child')).toBeInTheDocument();
    expect(screen.getByTestId('theme')).toHaveTextContent('light');

    const toggle = screen.getByRole('button', { name: 'Toggle theme' });
    await user.click(toggle);
    expect(screen.getByTestId('theme')).toHaveTextContent('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe('dark');

    await user.click(toggle);
    expect(screen.getByTestId('theme')).toHaveTextContent('light');
    expect(document.documentElement.dataset.theme).toBe('light');
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe('light');
  });

  it('remains consistent under StrictMode', () => {
    setMatchMedia(true);

    render(
      <StrictMode>
        <ThemeProvider>
          <ThemeConsumer />
        </ThemeProvider>
      </StrictMode>,
    );

    expect(screen.getByTestId('theme')).toHaveTextContent('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe('dark');
  });

  it('throws a clear error when used outside ThemeProvider', () => {
    expect(() => render(<ConsumerOutsideProvider />)).toThrowError(
      'useTheme must be used within a ThemeProvider',
    );
  });
});
