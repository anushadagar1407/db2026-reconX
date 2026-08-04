import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { BrandMark } from './index';

describe('BrandMark', () => {
  it('renders the official local lockup with an accessible brand name', () => {
    render(<BrandMark size="lg" />);

    const mark = screen.getByRole('img', { name: 'Deutsche Bank' });
    expect(mark).toHaveAttribute('src', '/assets/deutsche-bank-logo.svg');
    expect(mark).toHaveClass('brand-mark--lockup', 'brand-mark--lg');
    expect(mark).toHaveAttribute('draggable', 'false');
  });

  it('uses the local square mark and an empty alternative when decorative', () => {
    const { container } = render(<BrandMark variant="symbol" decorative />);

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    const mark = container.querySelector('img');
    expect(mark).toHaveAttribute('src', '/assets/deutsche-bank-mark.svg');
    expect(mark).toHaveAttribute('alt', '');
    expect(mark).toHaveAttribute('aria-hidden', 'true');
  });
});
