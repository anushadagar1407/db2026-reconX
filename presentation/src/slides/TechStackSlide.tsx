import { Slide } from '@revealjs/react';
import {
  BodyText,
  Cluster,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
  TechIcon,
} from '../design-system';

function StackTechnology({
  children,
  technology,
}: {
  children: string;
  technology?:
    | 'java'
    | 'spring'
    | 'postgresql'
    | 'react'
    | 'vite'
    | 'nginx'
    | 'kafka'
    | 'prometheus'
    | 'grafana'
    | 'docker'
    | 'github-actions';
}) {
  return (
    <span className="tech-stack-slide__technology">
      {technology ? <TechIcon technology={technology} size="sm" decorative /> : null}
      <span>{children}</span>
    </span>
  );
}

export function TechStackSlide() {
  return (
    <Slide aria-label="Slide 4: Technology stack">
      <SlideCanvas background="paper" className="tech-stack-slide-canvas">
        <Stack className="tech-stack-slide" gap="lg">
          <Cluster justify="between" align="end" wrap={false}>
            <Stack gap="sm">
              <SlideLabel>04 / STACK BY RESPONSIBILITY</SlideLabel>
              <SlideTitle>Stack by responsibility, not by shopping list</SlideTitle>
            </Stack>
            <span className="tech-stack-slide__aside">A foundation with explicit maturity boundaries</span>
          </Cluster>

          <div className="tech-stack-slide__bands" aria-label="ReconX technology stack by responsibility">
            <div className="tech-stack-slide__band">
              <div className="tech-stack-slide__band-label"><span>01</span> DOMAIN / API</div>
              <Cluster className="tech-stack-slide__band-content" gap="lg" align="center" wrap>
                <span className="tech-stack-slide__state tech-stack-slide__state--implemented">IMPLEMENTED</span>
                <StackTechnology technology="java">Java 25</StackTechnology>
                <StackTechnology technology="spring">Spring Boot 3.5.0</StackTechnology>
                <StackTechnology>sealed types · DTO boundaries</StackTechnology>
              </Cluster>
            </div>

            <div className="tech-stack-slide__band">
              <div className="tech-stack-slide__band-label"><span>02</span> DATA / AUDIT</div>
              <Cluster className="tech-stack-slide__band-content" gap="lg" align="center" wrap>
                <span className="tech-stack-slide__state tech-stack-slide__state--implemented">IMPLEMENTED</span>
                <StackTechnology technology="postgresql">PostgreSQL 16</StackTechnology>
                <StackTechnology>Liquibase · JPA / Envers</StackTechnology>
                <StackTechnology>JSONB · partitions</StackTechnology>
              </Cluster>
            </div>

            <div className="tech-stack-slide__band">
              <div className="tech-stack-slide__band-label"><span>03</span> UI / LIVE UPDATES</div>
              <Cluster className="tech-stack-slide__band-content" gap="lg" align="center" wrap>
                <span className="tech-stack-slide__state tech-stack-slide__state--implemented">IMPLEMENTED</span>
                <StackTechnology technology="react">React 19</StackTechnology>
                <StackTechnology technology="vite">Vite 6</StackTechnology>
                <StackTechnology technology="nginx">nginx</StackTechnology>
                <StackTechnology>SSE</StackTechnology>
              </Cluster>
            </div>

            <div className="tech-stack-slide__band tech-stack-slide__band--qualified">
              <div className="tech-stack-slide__band-label"><span>04</span> MESSAGING / OPS</div>
              <Cluster className="tech-stack-slide__band-content" gap="lg" align="center" wrap>
                <span className="tech-stack-slide__state tech-stack-slide__state--configured">CONFIGURED</span>
                <StackTechnology technology="kafka">Kafka scaffold</StackTechnology>
                <StackTechnology technology="prometheus">Prometheus 2.54.1</StackTechnology>
                <StackTechnology technology="grafana">Grafana 11.2.0</StackTechnology>
                <span className="tech-stack-slide__qualifier">application path unfinished</span>
              </Cluster>
            </div>

            <div className="tech-stack-slide__band">
              <div className="tech-stack-slide__band-label"><span>05</span> DELIVERY / PRESENTATION</div>
              <Cluster className="tech-stack-slide__band-content" gap="lg" align="center" wrap>
                <span className="tech-stack-slide__state tech-stack-slide__state--configured">CONFIGURED</span>
                <StackTechnology technology="docker">Docker</StackTechnology>
                <StackTechnology>Compose</StackTechnology>
                <StackTechnology technology="github-actions">GitHub Actions verification</StackTechnology>
                <StackTechnology>Reveal.js 6 · Playwright</StackTechnology>
              </Cluster>
            </div>
          </div>

          <BodyText className="tech-stack-slide__footer">
            Messaging is a configured dependency, not yet an integrated application path. Release status requires
            separate evidence; version strings alone are not release proof.
          </BodyText>
        </Stack>
      </SlideCanvas>
      <aside className="notes">
        Evidence status: implemented for Java/Spring, persistence, UI, live updates, observability configuration,
        and presentation tooling; configured for Kafka broker/dependency and Docker/Compose; gap for treating Kafka
        as integrated application messaging or the backend snapshot as a release. Refresh manifests, Compose,
        application configuration, and runtime integrations before changing maturity labels.
      </aside>
    </Slide>
  );
}

export default TechStackSlide;
