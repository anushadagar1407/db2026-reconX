import type { ReactNode } from 'react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ArchitectureSlide } from './ArchitectureSlide';
import { DeliverySlide } from './DeliverySlide';
import { LearningsSlide } from './LearningsSlide';
import { LiveDemoSlide } from './LiveDemoSlide';
import { MonitoringSlide } from './MonitoringSlide';
import { ProblemSlide } from './ProblemSlide';
import { QuestionsSlide } from './QuestionsSlide';
import { ReconciliationSlide } from './ReconciliationSlide';
import { TechStackSlide } from './TechStackSlide';
import { TitleSlide } from './TitleSlide';
import { contributors } from './contributors';

vi.mock('@revealjs/react', () => ({
  Slide: ({ children }: { children: ReactNode }) => <section data-testid="slide">{children}</section>,
}));

const slides = [
  <TitleSlide key="title" />,
  <ProblemSlide key="problem" />,
  <ArchitectureSlide key="architecture" />,
  <TechStackSlide key="stack" />,
  <LiveDemoSlide key="demo" demoUrl="https://demo.example.test" />,
  <ReconciliationSlide key="reconciliation" />,
  <DeliverySlide key="delivery" />,
  <MonitoringSlide key="monitoring" />,
  <LearningsSlide key="learnings" />,
  <QuestionsSlide key="questions" />,
];

describe('flat slide components', () => {
  it('renders one Reveal slide for each of the ten individual components in order', () => {
    const { container } = render(<>{slides}</>);
    const renderedSlides = Array.from(container.querySelectorAll('[data-testid="slide"]'));

    expect(renderedSlides).toHaveLength(10);
    expect(renderedSlides[10]).toBeUndefined();
    expect(renderedSlides.map((slide) => slide.querySelector('.type-slide-label')?.textContent)).toEqual([
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
    expect(renderedSlides.every((slide) => slide.querySelector('.slide-canvas'))).toBe(true);
  });

  it('renders the complete local public contributor roster on the title slide', () => {
    render(<TitleSlide />);

    expect(screen.getByRole('heading', { name: 'Contributors' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Contributors' }).tagName).toBe('DIV');
    expect(screen.getByText('PUBLIC GITHUB RECORD · 7')).toBeInTheDocument();
    expect(contributors).toHaveLength(7);

    contributors.forEach(({ displayName, login, avatarSrc }) => {
      expect(screen.getByText(displayName, { exact: true })).toBeInTheDocument();
      expect(screen.getByText(`@${login}`, { exact: true })).toBeInTheDocument();

      const avatar = screen.getByRole('img', {
        name: `Portrait of ${displayName}; GitHub login @${login}`,
      });

      expect(avatar).toHaveAttribute('src', avatarSrc);
      expect(avatar.getAttribute('src')).toMatch(/^\/assets\/contributors\/[a-z0-9-]+\.jpg$/);
      expect(avatar.getAttribute('src')).not.toMatch(/^https?:/);
      expect(avatar).toHaveAttribute('alt', `Portrait of ${displayName}; GitHub login @${login}`);
    });

    expect(screen.queryByText(/^@?sidoncode$/i)).not.toBeInTheDocument();
  });

  it('keeps evidence boundaries visible in the source-backed narrative', () => {
    render(<>{slides}</>);

    expect(screen.getByText('KAFKA APPLICATION ORCHESTRATION — GAP')).toBeInTheDocument();
    expect(screen.getByText('Queue has no worker; no application listener result is observed.')).toBeInTheDocument();
    expect(screen.getByText('push: false')).toBeInTheDocument();
    expect(screen.getByText('build completed · not pushed, loaded, or exported')).toBeInTheDocument();
    expect(screen.getByText('JUnit / Failsafe / Vitest / JaCoCo reports')).toBeInTheDocument();
    expect(screen.queryByText(/local image only/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/image build outputs/i)).not.toBeInTheDocument();
    expect(screen.getByText('runtime capture pending')).toBeInTheDocument();
    expect(screen.getByText('If unavailable: full-demo link or source/API walkthrough')).toBeInTheDocument();
    expect(screen.queryByText(/prepared fallback/i)).not.toBeInTheDocument();
    expect(screen.getByText('Presenter reflection pending')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'https://github.com/anushadagar1407/db2026-reconX' })).toBeInTheDocument();
  });

  it('keeps the live demo idle and retains the recovery link', () => {
    render(<LiveDemoSlide demoUrl="https://demo.example.test" />);

    expect(screen.getByRole('status')).toHaveTextContent('Demo: idle');
    expect(screen.getByLabelText('Embedded demo idle state')).toHaveTextContent(
      'No runtime capture is claimed here.',
    );
    expect(screen.getByRole('button', { name: 'Launch embedded demo' })).toBeInTheDocument();
    expect(screen.queryByTitle('ReconX live demo')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open full demo' })).toHaveAttribute(
      'href',
      'https://demo.example.test',
    );
  });
});
