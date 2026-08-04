import type { ReactNode } from 'react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { GroupDSlides } from './GroupDSlides';

vi.mock('@revealjs/react', () => ({
  Slide: ({ children }: { children: ReactNode }) => (
    <section data-testid="reveal-slide">{children}</section>
  ),
}));

describe('GroupDSlides', () => {
  it('renders exactly slides 9 and 10 in order', () => {
    render(<GroupDSlides />);

    expect(screen.getAllByTestId('reveal-slide')).toHaveLength(2);
    expect(screen.getAllByTestId('reveal-slide').map((slide) => (
      slide.querySelector('[data-group-d-slide]')?.getAttribute('data-group-d-slide')
    ))).toEqual(['9', '10']);
  });

  it('keeps learnings neutral and visibly pending human reflection', () => {
    render(<GroupDSlides />);

    expect(screen.getByRole('heading', { name: 'Learnings we can defend' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Configuration is not integration.' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Contract drift breaks rehearsals.' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Evidence needs provenance.' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Presenter reflection pending' })).toBeInTheDocument();
    expect(document.body.textContent).not.toMatch(/we passed|hardest bug|biggest win/i);
  });

  it('keeps the exact repository URL and truthful source anchors on the close', () => {
    render(<GroupDSlides />);

    const repositoryUrl = 'https://github.com/anushadagar1407/db2026-reconX';
    expect(screen.getByRole('link', { name: repositoryUrl })).toHaveAttribute('href', repositoryUrl);
    expect(screen.getByText('TradeController')).toBeInTheDocument();
    expect(screen.getByText('ReconciliationEngine')).toBeInTheDocument();
    expect(screen.getByText('docker-compose.yml')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'API' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Recon' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Runtime' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Delivery' })).toBeInTheDocument();
  });

  it('does not include unsupported release or production claims', () => {
    render(<GroupDSlides />);

    expect(document.body.textContent).not.toMatch(/v1\.0\.0|GHCR|deployed|production use|production deployment/i);
  });
});
