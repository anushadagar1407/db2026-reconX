import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { technologies, TechIcon } from './index';

describe('TechIcon', () => {
  it('exposes the curated repository technology registry', () => {
    expect(technologies).toEqual([
      'react',
      'typescript',
      'javascript',
      'java',
      'spring',
      'postgresql',
      'kafka',
      'docker',
      'github',
      'github-actions',
      'vite',
      'nginx',
      'prometheus',
      'grafana',
    ]);
  });

  it('provides a centralized accessible label and hides nested artwork', () => {
    const { container } = render(<TechIcon technology="kafka" size="lg" />);

    const icon = screen.getByRole('img', { name: 'Apache Kafka' });
    expect(icon).toHaveClass('tech-icon--lg');
    expect(icon).toHaveAttribute('data-technology', 'kafka');
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    expect(container.querySelector('svg')).toHaveAttribute('focusable', 'false');
  });

  it('removes decorative technology artwork from the accessibility tree', () => {
    const { container } = render(<TechIcon technology="react" decorative />);

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(container.firstElementChild).toHaveAttribute('aria-hidden', 'true');
    expect(container.firstElementChild).not.toHaveAttribute('aria-label');
  });
});
