import type { HTMLAttributes, ReactNode } from 'react';

export type SlideBackground = 'paper' | 'soft' | 'ink' | 'blue';
export type SlideInset = 'safe' | 'compact' | 'edge';
export type SlideAlignment = 'start' | 'center' | 'end';

export interface SlideCanvasProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  background?: SlideBackground;
  inset?: SlideInset;
  align?: SlideAlignment;
  contentClassName?: string;
}

export function SlideCanvas({
  children,
  background = 'paper',
  inset = 'safe',
  align = 'start',
  className,
  contentClassName,
  ...props
}: SlideCanvasProps) {
  const canvasClassName = [
    'slide-canvas',
    `slide-canvas--${background}`,
    className,
  ].filter(Boolean).join(' ');
  const safeAreaClassName = [
    'slide-canvas__safe-area',
    `slide-canvas__safe-area--${inset}`,
  ].join(' ');
  const safeContentClassName = [
    'slide-canvas__safe-content',
    `slide-canvas__safe-content--align-${align}`,
    contentClassName,
  ].filter(Boolean).join(' ');

  return (
    <div className={canvasClassName} data-slide-background={background} {...props}>
      <div className={safeAreaClassName} data-slide-inset={inset}>
        <div className={safeContentClassName}>{children}</div>
      </div>
    </div>
  );
}
