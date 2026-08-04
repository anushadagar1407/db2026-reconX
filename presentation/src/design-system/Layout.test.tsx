import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Cluster, Grid, SlideCanvas, Split, Stack } from './index';

describe('presentation layout primitives', () => {
  it('maps bounded slide variants to stable classes without dropping caller attributes', () => {
    render(
      <SlideCanvas
        background="ink"
        inset="compact"
        align="center"
        className="caller-canvas"
        contentClassName="caller-content"
        data-testid="canvas"
      >
        Slide content
      </SlideCanvas>,
    );

    const canvas = screen.getByTestId('canvas');
    expect(canvas).toHaveClass('slide-canvas', 'slide-canvas--ink', 'caller-canvas');
    expect(canvas).toHaveAttribute('data-slide-background', 'ink');
    const safeArea = canvas.firstElementChild;
    expect(safeArea).toHaveClass(
      'slide-canvas__safe-area--compact',
    );
    expect(safeArea).toHaveAttribute('data-slide-inset', 'compact');
    expect(safeArea?.firstElementChild).toHaveClass(
      'slide-canvas__safe-content',
      'slide-canvas__safe-content--align-center',
      'caller-content',
    );
  });

  it('applies typed gap, axis, wrapping, grid, and split contracts', () => {
    render(
      <div>
        <Stack data-testid="stack" gap="xl" align="end" justify="between"><span /></Stack>
        <Cluster data-testid="cluster" gap="2xs" wrap={false}><span /></Cluster>
        <Grid
          data-testid="grid"
          columns={12}
          minimumColumnWidth="compact"
          gap="none"
        >
          <span />
        </Grid>
        <Split data-testid="split" ratio="2:3" align="start"><span /><span /></Split>
      </div>,
    );

    expect(screen.getByTestId('stack')).toHaveClass(
      'ds-stack',
      'ds-gap--xl',
      'ds-align--end',
      'ds-justify--between',
    );
    expect(screen.getByTestId('cluster')).toHaveClass('ds-cluster--nowrap', 'ds-gap--2xs');
    expect(screen.getByTestId('grid')).toHaveClass(
      'ds-grid--columns-12',
      'ds-grid--minimum-compact',
      'ds-gap--none',
    );
    expect(screen.getByTestId('split')).toHaveClass('ds-split--ratio-2-3', 'ds-align--start');
  });

  it('uses overflow-safe defaults for authoring layouts', () => {
    render(
      <div>
        <Stack data-testid="stack"><span /></Stack>
        <Cluster data-testid="cluster"><span /></Cluster>
        <Grid data-testid="grid"><span /></Grid>
        <Split data-testid="split"><span /><span /></Split>
      </div>,
    );

    expect(screen.getByTestId('stack')).toHaveClass('ds-gap--md', 'ds-align--stretch');
    expect(screen.getByTestId('cluster')).toHaveClass('ds-cluster--wrap', 'ds-align--center');
    expect(screen.getByTestId('grid')).toHaveClass(
      'ds-grid--columns-auto',
      'ds-grid--minimum-standard',
    );
    expect(screen.getByTestId('split')).toHaveClass('ds-split--ratio-1-1');
  });
});
