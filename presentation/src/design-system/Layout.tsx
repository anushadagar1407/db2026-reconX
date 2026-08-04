import type { HTMLAttributes, ReactNode } from 'react';
import type {
  CrossAxisAlignment,
  MainAxisAlignment,
  PresentationGap,
} from './contracts';

interface LayoutProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  gap?: PresentationGap;
  align?: CrossAxisAlignment;
}

export interface StackProps extends LayoutProps {
  justify?: MainAxisAlignment;
}

export function Stack({
  children,
  gap = 'md',
  align = 'stretch',
  justify = 'start',
  className,
  ...props
}: StackProps) {
  const classes = [
    'ds-stack',
    `ds-gap--${gap}`,
    `ds-align--${align}`,
    `ds-justify--${justify}`,
    className,
  ].filter(Boolean).join(' ');

  return <div className={classes} {...props}>{children}</div>;
}

export interface ClusterProps extends LayoutProps {
  justify?: MainAxisAlignment;
  wrap?: boolean;
}

export function Cluster({
  children,
  gap = 'sm',
  align = 'center',
  justify = 'start',
  wrap = true,
  className,
  ...props
}: ClusterProps) {
  const classes = [
    'ds-cluster',
    `ds-gap--${gap}`,
    `ds-align--${align}`,
    `ds-justify--${justify}`,
    wrap ? 'ds-cluster--wrap' : 'ds-cluster--nowrap',
    className,
  ].filter(Boolean).join(' ');

  return <div className={classes} {...props}>{children}</div>;
}

export type GridColumns = 1 | 2 | 3 | 4 | 6 | 12 | 'auto';
export type GridMinimum = 'compact' | 'standard' | 'wide';

export interface GridProps extends LayoutProps {
  columns?: GridColumns;
  minimumColumnWidth?: GridMinimum;
}

export function Grid({
  children,
  gap = 'md',
  align = 'stretch',
  columns = 'auto',
  minimumColumnWidth = 'standard',
  className,
  ...props
}: GridProps) {
  const classes = [
    'ds-grid',
    `ds-gap--${gap}`,
    `ds-align--${align}`,
    `ds-grid--columns-${columns}`,
    `ds-grid--minimum-${minimumColumnWidth}`,
    className,
  ].filter(Boolean).join(' ');

  return <div className={classes} {...props}>{children}</div>;
}

export type SplitRatio = '1:1' | '2:1' | '3:2' | '2:3';

export interface SplitProps extends LayoutProps {
  ratio?: SplitRatio;
}

export function Split({
  children,
  gap = 'lg',
  align = 'stretch',
  ratio = '1:1',
  className,
  ...props
}: SplitProps) {
  const classes = [
    'ds-split',
    `ds-gap--${gap}`,
    `ds-align--${align}`,
    `ds-split--ratio-${ratio.replace(':', '-')}`,
    className,
  ].filter(Boolean).join(' ');

  return <div className={classes} {...props}>{children}</div>;
}
