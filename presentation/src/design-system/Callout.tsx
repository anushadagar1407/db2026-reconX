import type { HTMLAttributes, ReactNode } from 'react';

export type CalloutTone = 'neutral' | 'information' | 'success' | 'warning' | 'critical';

export interface CalloutProps extends Omit<HTMLAttributes<HTMLElement>, 'children' | 'title'> {
  children: ReactNode;
  label: string;
  title?: ReactNode;
  tone?: CalloutTone;
}

export function Callout({
  children,
  label,
  title,
  tone = 'neutral',
  className,
  ...props
}: CalloutProps) {
  const classes = ['ds-callout', `ds-callout--${tone}`, className].filter(Boolean).join(' ');

  return (
    <aside className={classes} aria-label={label} {...props}>
      {title ? <p className="ds-callout__title">{title}</p> : null}
      <div className="ds-callout__body">{children}</div>
    </aside>
  );
}
