import type { ReactNode } from 'react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import App from './App';

vi.mock('@revealjs/react', () => ({
  Deck: ({ children }: { children: ReactNode }) => <main data-testid="deck">{children}</main>,
  Slide: ({ children }: { children: ReactNode }) => <section>{children}</section>,
}));

vi.mock('reveal.js/plugin/notes', () => ({ default: {} }));

describe('App', () => {
  it('mounts the reveal deck and configured live-demo boundary', () => {
    window.__RECONX_PRESENTATION_CONFIG__ = { demoUrl: 'https://demo.example.test' };
    render(<App />);

    expect(screen.getByTestId('deck')).toBeInTheDocument();
    expect(screen.getByTestId('deck').querySelector('.slide-canvas--paper')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Deutsche Bank' })).toHaveAttribute(
      'src',
      '/assets/deutsche-bank-logo.svg',
    );
    expect(screen.getByRole('heading', { name: 'ReconX presentation scaffold' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open full demo' })).toHaveAttribute(
      'href',
      'https://demo.example.test/',
    );
  });
});
