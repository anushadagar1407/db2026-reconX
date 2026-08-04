import type { ReactNode } from 'react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { GroupASlides } from './GroupASlides';

vi.mock('@revealjs/react', () => ({
  Slide: ({ children }: { children: ReactNode }) => <section>{children}</section>,
}));

describe('GroupASlides', () => {
  it('renders exactly slides 1 through 4 in order with a canvas on each slide', () => {
    const { container } = render(<GroupASlides />);
    const slides = Array.from(container.querySelectorAll(':scope > section'));

    expect(slides).toHaveLength(4);
    expect(slides.map((slide) => slide.querySelector('.type-slide-label')?.textContent)).toEqual([
      'DAY 10 · 20-MINUTE DEMO',
      '02 / OPERATIONS PROBLEM',
      '03 / CURRENT RUNTIME BOUNDARY',
      '04 / STACK BY RESPONSIBILITY',
    ]);
    expect(slides.every((slide) => slide.querySelector('.slide-canvas'))).toBe(true);
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(4);
  });

  it('keeps the problem framing concise and avoids unsupported impact claims', () => {
    const { container } = render(<GroupASlides />);

    expect(container.querySelectorAll('.group-a-bullets > li')).toHaveLength(3);
    expect(screen.getByText('ILLUSTRATIVE DISCREPANCY · NOT PRODUCTION DATA')).toBeInTheDocument();
    expect(container.textContent).not.toMatch(/costs millions|production volume|€|\$/i);
  });

  it('labels the active architecture boundary and keeps Kafka orchestration as a gap', () => {
    const { container } = render(<GroupASlides />);
    const text = container.textContent ?? '';

    expect(screen.getByText('ACTIVE PATH · IMPLEMENTED')).toBeInTheDocument();
    expect(screen.getByText('SSE on trade create → browser')).toBeInTheDocument();
    expect(screen.getByText('KAFKA APPLICATION ORCHESTRATION — GAP')).toBeInTheDocument();
    expect(screen.getByText('Application event → reconciliation worker is not implemented; this is not a live arrow.')).toBeInTheDocument();
    expect(text).not.toContain('Kafka is active');
    expect(text).not.toContain('active Kafka');
    expect(text).not.toContain('Kafka →');
    expect(text).not.toContain('Kafka event path');
  });

  it('qualifies Kafka maturity on the responsibility-based stack index', () => {
    const { container } = render(<GroupASlides />);

    expect(screen.getByText('Kafka scaffold')).toBeInTheDocument();
    expect(screen.getByText('application path unfinished')).toBeInTheDocument();
    expect(screen.getByText(/Messaging is a configured dependency, not yet an integrated application path/)).toBeInTheDocument();
    expect(container.querySelectorAll('.group-a-stack__band')).toHaveLength(5);
    expect(container.querySelectorAll('.group-a-stack__technology')).not.toHaveLength(0);
  });
});
