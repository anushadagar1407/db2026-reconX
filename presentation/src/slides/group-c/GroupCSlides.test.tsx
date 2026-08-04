import type { ReactNode } from 'react';
import { render, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { GroupCSlides } from './GroupCSlides';

vi.mock('@revealjs/react', () => ({
  Slide: ({ children, ...props }: { children: ReactNode }) => <section {...props}>{children}</section>,
}));

describe('GroupCSlides', () => {
  it('renders exactly slides 7 and 8 in order', () => {
    const { container } = render(<GroupCSlides />);
    const slides = container.querySelectorAll<HTMLElement>(':scope > section');

    expect(slides).toHaveLength(2);
    expect(within(slides[0]).getByRole('heading', { name: /CI verifies the build/i })).toBeInTheDocument();
    expect(within(slides[1]).getByRole('heading', { name: /Monitoring is provisioned/i })).toBeInTheDocument();
  });

  it('keeps the revalidated CI provenance and skipped-job truth visible', () => {
    const { container } = render(<GroupCSlides />);
    const delivery = container.querySelectorAll<HTMLElement>(':scope > section')[0];

    expect(within(delivery).getByText('PR #272 · run 30898100384')).toBeInTheDocument();
    expect(within(delivery).getByText('commit 4f1a939a')).toBeInTheDocument();
    expect(within(delivery).getByText(/Load job and native fallbacks: skipped/i)).toBeInTheDocument();
    expect(within(delivery).getAllByText(/passed/i).length).toBeGreaterThanOrEqual(3);
  });

  it('terminates delivery at push false without claiming publication or deployment', () => {
    const { container } = render(<GroupCSlides />);
    const delivery = container.querySelectorAll<HTMLElement>(':scope > section')[0];
    const visibleRail = delivery.querySelector('.slide-canvas');

    expect(within(visibleRail as HTMLElement).getByText('push: false')).toBeInTheDocument();
    expect(within(visibleRail as HTMLElement).getByText(/GHCR · deployment · release/i)).toBeInTheDocument();
    expect(within(visibleRail as HTMLElement).getByText(/no success arrow/i)).toBeInTheDocument();
    expect(visibleRail?.textContent).not.toMatch(/GHCR (push|publication) (passed|succeeded|complete)/i);
    expect(visibleRail?.textContent).not.toMatch(/deployed successfully/i);
  });

  it('shows the actual 10-VU / 100-create profile and pending 200-VU evidence', () => {
    const { container } = render(<GroupCSlides />);
    const monitoring = container.querySelectorAll<HTMLElement>(':scope > section')[1];
    const visibleStrip = monitoring.querySelector('.slide-canvas');

    expect(within(visibleStrip as HTMLElement).getByText(/10 VUs/i)).toBeInTheDocument();
    expect(within(visibleStrip as HTMLElement).getByText(/10 iterations/i)).toBeInTheDocument();
    expect(within(visibleStrip as HTMLElement).getByText(/100 trade creations/i)).toBeInTheDocument();
    expect(within(visibleStrip as HTMLElement).getByText(/200-VU baseline \/ load \/ recovery/i)).toBeInTheDocument();
    expect(within(visibleStrip as HTMLElement).getByText('pending', { exact: true })).toBeInTheDocument();
    expect(visibleStrip?.textContent).not.toMatch(/200 VUs? (passed|success|completed)/i);
  });
});
