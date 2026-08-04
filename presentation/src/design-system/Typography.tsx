import type { HTMLAttributes, ReactNode } from 'react';

interface TextProps {
  children: ReactNode;
  className?: string;
}

export type SlideTitleProps = TextProps & Omit<HTMLAttributes<HTMLHeadingElement>, keyof TextProps>;

export function SlideTitle({ children, className, ...props }: SlideTitleProps) {
  const classes = ['type-slide-title', className].filter(Boolean).join(' ');
  return <h1 className={classes} {...props}>{children}</h1>;
}

export type SectionHeadingProps = TextProps & Omit<HTMLAttributes<HTMLHeadingElement>, keyof TextProps>;

export function SectionHeading({ children, className, ...props }: SectionHeadingProps) {
  const classes = ['type-section-heading', className].filter(Boolean).join(' ');
  return <h2 className={classes} {...props}>{children}</h2>;
}

export type BodyTextVariant = 'body' | 'lede';

export type BodyTextProps = TextProps
  & Omit<HTMLAttributes<HTMLParagraphElement>, keyof TextProps>
  & { variant?: BodyTextVariant };

export function BodyText({
  children,
  variant = 'body',
  className,
  ...props
}: BodyTextProps) {
  const classes = ['type-body', `type-body--${variant}`, className].filter(Boolean).join(' ');
  return <p className={classes} {...props}>{children}</p>;
}

export type SlideLabelProps = TextProps & Omit<HTMLAttributes<HTMLParagraphElement>, keyof TextProps>;

export function SlideLabel({ children, className, ...props }: SlideLabelProps) {
  const classes = ['type-slide-label', className].filter(Boolean).join(' ');
  return <p className={classes} {...props}>{children}</p>;
}
