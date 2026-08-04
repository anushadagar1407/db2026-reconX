import type { ReactNode } from 'react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { GroupBSlides } from './GroupBSlides';

vi.mock('@revealjs/react', () => ({
  Slide: ({ children }: { children: ReactNode }) => (
    <section data-testid="reveal-slide">{children}</section>
  ),
}));

describe('GroupBSlides', () => {
  it('renders exactly slides 5 and 6 in order', () => {
    const { container } = render(<GroupBSlides demoUrl="https://demo.example.test" />);

    expect(container.querySelectorAll('[data-testid="reveal-slide"]')).toHaveLength(2);
    expect(screen.getByRole('heading', { name: 'Live demo runway' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Reconciliation truth line' })).toBeInTheDocument();
    expect(screen.getByText('05 / Live trade journey')).toBeInTheDocument();
    expect(screen.getByText('06 / Reconciliation truth line')).toBeInTheDocument();
  });

  it('keeps the four demo checkpoint labels visible and uses the idle guarded frame', () => {
    render(<GroupBSlides demoUrl="https://demo.example.test" />);

    expect(screen.getByText('JWT + role check')).toBeInTheDocument();
    expect(screen.getByText('validated POST')).toBeInTheDocument();
    expect(screen.getByText('HTTP 201 → PostgreSQL')).toBeInTheDocument();
    expect(screen.getByText('create-time SSE → trade_created_total')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Demo: idle');
    expect(screen.getByRole('button', { name: 'Launch embedded demo' })).toBeInTheDocument();
    expect(screen.queryByTitle('ReconX live demo')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open full demo' })).toHaveAttribute(
      'href',
      'https://demo.example.test',
    );
  });

  it('states the Kafka and worker boundary without claiming an automatic result', () => {
    render(<GroupBSlides demoUrl="https://demo.example.test" />);

    expect(screen.getByText('POST /api/v1/recon/run')).toBeInTheDocument();
    expect(screen.getByText('202 · QUEUED')).toBeInTheDocument();
    expect(screen.getByText('Apache Kafka')).toBeInTheDocument();
    expect(screen.getByText('Queue has no worker; no application listener result is observed.'))
      .toBeInTheDocument();
    expect(screen.getByText(/Kafka is a muted configured node, not an observed application event route/)).toBeInTheDocument();
    expect(screen.queryByText(/automatically reconciled|event was consumed|worker processed/i)).not.toBeInTheDocument();
  });
});
