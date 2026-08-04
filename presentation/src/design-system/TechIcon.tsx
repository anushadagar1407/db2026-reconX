import ApachekafkaOriginal from 'devicons-react/icons/ApachekafkaOriginal';
import DockerOriginal from 'devicons-react/icons/DockerOriginal';
import GithubOriginal from 'devicons-react/icons/GithubOriginal';
import GithubactionsOriginal from 'devicons-react/icons/GithubactionsOriginal';
import GrafanaOriginal from 'devicons-react/icons/GrafanaOriginal';
import JavaOriginal from 'devicons-react/icons/JavaOriginal';
import JavascriptOriginal from 'devicons-react/icons/JavascriptOriginal';
import NginxOriginal from 'devicons-react/icons/NginxOriginal';
import PostgresqlOriginal from 'devicons-react/icons/PostgresqlOriginal';
import PrometheusOriginal from 'devicons-react/icons/PrometheusOriginal';
import ReactOriginal from 'devicons-react/icons/ReactOriginal';
import SpringOriginal from 'devicons-react/icons/SpringOriginal';
import TypescriptOriginal from 'devicons-react/icons/TypescriptOriginal';
import VitejsOriginal from 'devicons-react/icons/VitejsOriginal';
import type { ComponentType, SVGProps } from 'react';
import type { Technology } from './contracts';

interface DeviconProps extends SVGProps<SVGElement> {
  size?: number | string;
}

interface TechIconDefinition {
  label: string;
  icon: ComponentType<DeviconProps>;
}

// Technology artwork is bundled through devicons-react (MIT):
// https://github.com/MKAbuMattar/devicons-react, sourced from Devicon (MIT).
const techIconRegistry: Record<Technology, TechIconDefinition> = {
  react: { label: 'React', icon: ReactOriginal },
  typescript: { label: 'TypeScript', icon: TypescriptOriginal },
  javascript: { label: 'JavaScript', icon: JavascriptOriginal },
  java: { label: 'Java', icon: JavaOriginal },
  spring: { label: 'Spring', icon: SpringOriginal },
  postgresql: { label: 'PostgreSQL', icon: PostgresqlOriginal },
  kafka: { label: 'Apache Kafka', icon: ApachekafkaOriginal },
  docker: { label: 'Docker', icon: DockerOriginal },
  github: { label: 'GitHub', icon: GithubOriginal },
  'github-actions': { label: 'GitHub Actions', icon: GithubactionsOriginal },
  vite: { label: 'Vite', icon: VitejsOriginal },
  nginx: { label: 'nginx', icon: NginxOriginal },
  prometheus: { label: 'Prometheus', icon: PrometheusOriginal },
  grafana: { label: 'Grafana', icon: GrafanaOriginal },
};
export type TechIconSize = 'sm' | 'md' | 'lg' | 'xl';

export interface TechIconProps {
  technology: Technology;
  size?: TechIconSize;
  decorative?: boolean;
}

export function TechIcon({
  technology,
  size = 'md',
  decorative = false,
}: TechIconProps) {
  const definition = techIconRegistry[technology];
  const Icon = definition.icon;

  return (
    <span
      className={`tech-icon tech-icon--${size}`}
      role={decorative ? undefined : 'img'}
      aria-label={decorative ? undefined : definition.label}
      aria-hidden={decorative ? true : undefined}
      data-technology={technology}
    >
      <Icon size="1em" aria-hidden="true" focusable="false" />
    </span>
  );
}
