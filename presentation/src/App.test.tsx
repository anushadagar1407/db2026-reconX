import type { ReactNode } from 'react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import App from './App';

vi.mock('@revealjs/react', () => ({
  Deck: ({ children }: { children: ReactNode }) => <main data-testid="deck">{children}</main>,
  Slide: ({ children }: { children: ReactNode }) => <section data-testid="reveal-slide">{children}</section>,
}));

vi.mock('reveal.js/plugin/notes', () => ({ default: {} }));

describe('App', () => {
  it('renders exactly ten slides in the required order', () => {
    window.__RECONX_PRESENTATION_CONFIG__ = { demoUrl: 'https://demo.example.test' };
    const { container } = render(<App />);
    const slides = Array.from(container.querySelectorAll('[data-testid="reveal-slide"]'));

    expect(screen.getByTestId('deck')).toBeInTheDocument();
    expect(slides).toHaveLength(10);
    expect(slides.map((slide) => slide.querySelector('.type-slide-label')?.textContent)).toEqual([
      'DAY 10 · 20-MINUTE DEMO',
      '02 / OPERATIONS PROBLEM',
      '03 / CURRENT RUNTIME BOUNDARY',
      '04 / STACK BY RESPONSIBILITY',
      '05 / LIVE TRADE JOURNEY',
      '06 / RECONCILIATION TRUTH LINE',
      '07 / CI/CD DELIVERY RAIL · 11:00–12:40',
      '08 / MONITORING & LOAD EVIDENCE · 12:40–14:25',
      '09 / AFTER-ACTION NOTES',
      '10 / OPEN FLOOR',
    ]);
    expect(slides.every((slide) => slide.querySelector('.slide-canvas'))).toBe(true);
  });

  it('preserves the brand, critical truth labels, and guarded demo boundary', () => {
    window.__RECONX_PRESENTATION_CONFIG__ = { demoUrl: 'https://demo.example.test' };
    render(<App />);

    expect(screen.getByRole('img', { name: 'Deutsche Bank' })).toHaveAttribute(
      'src',
      '/assets/deutsche-bank-logo.svg',
    );
    expect(screen.getByRole('heading', { name: 'ReconX — Enterprise Trade Reconciliation Platform' })).toBeInTheDocument();
    expect(screen.getByText('KAFKA APPLICATION ORCHESTRATION — GAP')).toBeInTheDocument();
    expect(screen.getByText('push: false')).toBeInTheDocument();
    expect(screen.getByText('runtime capture pending')).toBeInTheDocument();
    expect(screen.getByText('Presenter reflection pending')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Launch embedded demo' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open full demo' })).toHaveAttribute(
      'href',
      'https://demo.example.test/',
    );
  });
});
