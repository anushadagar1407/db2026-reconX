export const presentationGaps = [
  'none',
  '2xs',
  'xs',
  'sm',
  'md',
  'lg',
  'xl',
  '2xl',
] as const;

export type PresentationGap = (typeof presentationGaps)[number];
export type CrossAxisAlignment = 'start' | 'center' | 'end' | 'stretch';
export type MainAxisAlignment = 'start' | 'center' | 'end' | 'between';

export const technologies = [
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
] as const;

export type Technology = (typeof technologies)[number];
