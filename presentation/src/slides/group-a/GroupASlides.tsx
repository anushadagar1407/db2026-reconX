import { Slide } from '@revealjs/react';
import {
  BodyText,
  BrandMark,
  Callout,
  Cluster,
  SectionHeading,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
  TechIcon,
} from '../../design-system';
import './GroupASlides.css';

function ArchitectureNode({
  children,
  technology,
  state,
  detail,
}: {
  children: string;
  technology?: 'nginx' | 'spring' | 'postgresql' | 'prometheus' | 'grafana' | 'kafka';
  state: 'active' | 'configured' | 'gap';
  detail: string;
}) {
  return (
    <div className={`group-a-architecture__node group-a-architecture__node--${state}`}>
      <Cluster gap="xs" align="center" wrap={false}>
        {technology ? <TechIcon technology={technology} size="sm" decorative /> : null}
        <span className="group-a-architecture__node-name">{children}</span>
      </Cluster>
      <span className="group-a-architecture__node-detail">{detail}</span>
    </div>
  );
}

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
    <span className="group-a-stack__technology">
      {technology ? <TechIcon technology={technology} size="sm" decorative /> : null}
      <span>{children}</span>
    </span>
  );
}

export function GroupASlides() {
  return (
    <>
      <Slide>
        <SlideCanvas background="ink" className="group-a-canvas group-a-canvas--title">
          <Stack className="group-a-slide group-a-title" justify="between" gap="xl">
            <Cluster justify="between" align="start" wrap={false}>
              <span className="group-a-title__brand-lockup">
                <BrandMark size="lg" />
              </span>
              <SlideLabel>DAY 10 · 20-MINUTE DEMO</SlideLabel>
            </Cluster>

            <Stack className="group-a-title__content" gap="lg">
              <div className="group-a-title__rule" aria-hidden="true" />
              <SlideTitle>ReconX — Enterprise Trade Reconciliation Platform</SlideTitle>
              <BodyText variant="lede" className="group-a-title__context">
                Deutsche Bank TDI 2026 training case study
              </BodyText>
            </Stack>

            <Cluster className="group-a-title__footer" justify="between" align="end" wrap={false}>
              <BodyText>Evidence boundary first. Demonstration detail second.</BodyText>
              <SlideLabel>TRAINING CASE STUDY · NOT PRODUCTION DEPLOYMENT</SlideLabel>
            </Cluster>
          </Stack>
        </SlideCanvas>
        <aside className="notes">
          Evidence status: implemented for the product title, training context, local brand artwork, and
          presentation framework; gap for the final ten-slide deck; requires human input for roster and
          speaking lead. Refresh if product framing, branding, slide count, or presenter ownership changes.
        </aside>
      </Slide>

      <Slide>
        <SlideCanvas background="paper" className="group-a-canvas group-a-canvas--problem">
          <Stack className="group-a-slide group-a-problem" gap="lg">
            <Stack gap="sm">
              <SlideLabel>02 / OPERATIONS PROBLEM</SlideLabel>
              <SlideTitle>Mismatches need a controlled path</SlideTitle>
            </Stack>

            <div className="group-a-problem__content">
              <Stack className="group-a-problem__explanation" gap="lg">
                <SectionHeading>One trade, two records, one break to investigate.</SectionHeading>
                <BodyText>
                  ReconX models the operator&apos;s journey from capture to comparison to evidence-backed
                  resolution.
                </BodyText>
                <ul className="group-a-bullets">
                  <li>Capture the internal trade record.</li>
                  <li>Compare it with an external record.</li>
                  <li>Investigate and resolve the break with evidence.</li>
                </ul>
              </Stack>

              <div className="group-a-ledger" aria-label="Illustrative internal and external trade ledger">
                <div className="group-a-ledger__caption">ILLUSTRATIVE DISCREPANCY · NOT PRODUCTION DATA</div>
                <div className="group-a-ledger__table">
                  <div className="group-a-ledger__cell group-a-ledger__cell--blank" />
                  <div className="group-a-ledger__cell group-a-ledger__cell--heading">INTERNAL RECORD</div>
                  <div className="group-a-ledger__cell group-a-ledger__cell--heading">EXTERNAL RECORD</div>

                  <div className="group-a-ledger__cell group-a-ledger__field">trade reference</div>
                  <div className="group-a-ledger__cell">same reference</div>
                  <div className="group-a-ledger__cell">same reference</div>

                  <div className="group-a-ledger__cell group-a-ledger__field">quantity</div>
                  <div className="group-a-ledger__cell">captured</div>
                  <div className="group-a-ledger__cell">received</div>

                  <div className="group-a-ledger__cell group-a-ledger__field">price</div>
                  <div className="group-a-ledger__cell group-a-ledger__cell--break">recorded value</div>
                  <div className="group-a-ledger__cell group-a-ledger__cell--break">received value</div>
                </div>
                <Callout label="Reconciliation break" title="BREAK" tone="warning">
                  A mismatch is now inspectable: compare the records, then resolve with evidence.
                </Callout>
              </div>
            </div>
          </Stack>
        </SlideCanvas>
        <aside className="notes">
          Evidence status: implemented for database-backed trades, seeded reconciliation breaks, the matching
          engine, and break-resolution operations; requires fresh runtime evidence for any visible count or
          captured pair; gap for complete automatic reconciliation. The ledger is illustrative and carries no
          financial-impact claim. Refresh after Trade, break schema/seed, reconciliation, or runtime-data changes.
        </aside>
      </Slide>

      <Slide>
        <SlideCanvas background="soft" className="group-a-canvas group-a-canvas--architecture">
          <Stack className="group-a-slide group-a-architecture" gap="md">
            <Cluster justify="between" align="end" wrap={false}>
              <Stack gap="sm">
                <SlideLabel>03 / CURRENT RUNTIME BOUNDARY</SlideLabel>
                <SlideTitle>Architecture, as it runs today</SlideTitle>
              </Stack>
              <span className="group-a-architecture__status">SOURCE-SUPPORTED PATH</span>
            </Cluster>

            <BodyText variant="lede" className="group-a-architecture__lede">
              Capture and observation are source-supported; event orchestration remains unfinished.
            </BodyText>

            <div className="group-a-architecture__stage" aria-label="Current runtime architecture">
              <div className="group-a-architecture__active-route">
                <div className="group-a-architecture__route-label">ACTIVE PATH · IMPLEMENTED</div>
                <div className="group-a-architecture__flow">
                  <ArchitectureNode state="active" detail="browser">Browser</ArchitectureNode>
                  <span className="group-a-architecture__arrow" aria-hidden="true">→</span>
                  <ArchitectureNode technology="nginx" state="active" detail="frontend proxy">nginx</ArchitectureNode>
                  <span className="group-a-architecture__arrow" aria-hidden="true">→</span>
                  <ArchitectureNode technology="spring" state="active" detail="REST API">Spring API</ArchitectureNode>
                  <span className="group-a-architecture__arrow" aria-hidden="true">→</span>
                  <ArchitectureNode technology="postgresql" state="active" detail="durable record">PostgreSQL</ArchitectureNode>
                </div>
              </div>

              <div className="group-a-architecture__branches">
                <div className="group-a-architecture__branch group-a-architecture__branch--sse">
                  <span className="group-a-architecture__branch-label">LIVE FEEDBACK · IMPLEMENTED</span>
                  <span className="group-a-architecture__branch-line" aria-hidden="true">↩</span>
                  <span>SSE on trade create → browser</span>
                </div>

                <div className="group-a-architecture__branch group-a-architecture__branch--observability">
                  <span className="group-a-architecture__branch-label">METRICS · CONFIGURED</span>
                  <span className="group-a-architecture__branch-source">Spring API</span>
                  <span className="group-a-architecture__arrow" aria-hidden="true">→</span>
                  <ArchitectureNode state="configured" detail="scrape">Prometheus</ArchitectureNode>
                  <span className="group-a-architecture__arrow" aria-hidden="true">→</span>
                  <ArchitectureNode state="configured" detail="panels">Grafana</ArchitectureNode>
                </div>

                <div className="group-a-architecture__branch group-a-architecture__branch--infrastructure">
                  <span className="group-a-architecture__branch-label">COMPOSE TOPOLOGY · CONFIGURED</span>
                  <ArchitectureNode technology="kafka" state="configured" detail="broker">Kafka</ArchitectureNode>
                  <ArchitectureNode state="configured" detail="coordination">Zookeeper</ArchitectureNode>
                  <span className="group-a-architecture__infra-note">Spring ↔ Kafka: bootstrap / health only</span>
                </div>
              </div>

              <div className="group-a-architecture__gap" aria-label="Kafka application orchestration gap">
                <Cluster justify="between" align="center" wrap={false}>
                  <Stack gap="2xs">
                    <div className="group-a-architecture__gap-title">KAFKA APPLICATION ORCHESTRATION — GAP</div>
                    <div className="group-a-architecture__gap-copy">
                      Application event → reconciliation worker is not implemented; this is not a live arrow.
                    </div>
                  </Stack>
                  <Cluster className="group-a-architecture__legend" gap="md" wrap={false}>
                    <span><i className="group-a-architecture__swatch group-a-architecture__swatch--active" /> implemented</span>
                    <span><i className="group-a-architecture__swatch group-a-architecture__swatch--configured" /> configured</span>
                    <span><i className="group-a-architecture__swatch group-a-architecture__swatch--gap" /> gap</span>
                  </Cluster>
                </Cluster>
              </div>
            </div>
          </Stack>
        </SlideCanvas>
        <aside className="notes">
          Evidence status: implemented for REST capture, PostgreSQL persistence, create-time SSE, and metric
          registration; configured for the seven-service Compose topology, dependency gates, Prometheus scrape,
          and Grafana provisioning; gap for producer/listener flow, queued-job worker, audit/DLQ path, and complete
          health proof; requires fresh runtime evidence for reachability or healthy services. Recheck against the
          current-state ADV160 Mermaid source before final acceptance.
        </aside>
      </Slide>

      <Slide>
        <SlideCanvas background="paper" className="group-a-canvas group-a-canvas--stack">
          <Stack className="group-a-slide group-a-stack" gap="lg">
            <Cluster justify="between" align="end" wrap={false}>
              <Stack gap="sm">
                <SlideLabel>04 / STACK BY RESPONSIBILITY</SlideLabel>
                <SlideTitle>Stack by responsibility, not by shopping list</SlideTitle>
              </Stack>
              <span className="group-a-stack__aside">A foundation with explicit maturity boundaries</span>
            </Cluster>

            <div className="group-a-stack__bands" aria-label="ReconX technology stack by responsibility">
              <div className="group-a-stack__band">
                <div className="group-a-stack__band-label"><span>01</span> DOMAIN / API</div>
                <Cluster className="group-a-stack__band-content" gap="lg" align="center" wrap>
                  <span className="group-a-stack__state group-a-stack__state--implemented">IMPLEMENTED</span>
                  <StackTechnology technology="java">Java 25</StackTechnology>
                  <StackTechnology technology="spring">Spring Boot 3.5.0</StackTechnology>
                  <StackTechnology>sealed types · DTO boundaries</StackTechnology>
                </Cluster>
              </div>

              <div className="group-a-stack__band">
                <div className="group-a-stack__band-label"><span>02</span> DATA / AUDIT</div>
                <Cluster className="group-a-stack__band-content" gap="lg" align="center" wrap>
                  <span className="group-a-stack__state group-a-stack__state--implemented">IMPLEMENTED</span>
                  <StackTechnology technology="postgresql">PostgreSQL 16</StackTechnology>
                  <StackTechnology>Liquibase · JPA / Envers</StackTechnology>
                  <StackTechnology>JSONB · partitions</StackTechnology>
                </Cluster>
              </div>

              <div className="group-a-stack__band">
                <div className="group-a-stack__band-label"><span>03</span> UI / LIVE UPDATES</div>
                <Cluster className="group-a-stack__band-content" gap="lg" align="center" wrap>
                  <span className="group-a-stack__state group-a-stack__state--implemented">IMPLEMENTED</span>
                  <StackTechnology technology="react">React 19</StackTechnology>
                  <StackTechnology technology="vite">Vite 6</StackTechnology>
                  <StackTechnology technology="nginx">nginx</StackTechnology>
                  <StackTechnology>SSE</StackTechnology>
                </Cluster>
              </div>

              <div className="group-a-stack__band group-a-stack__band--qualified">
                <div className="group-a-stack__band-label"><span>04</span> MESSAGING / OPS</div>
                <Cluster className="group-a-stack__band-content" gap="lg" align="center" wrap>
                  <span className="group-a-stack__state group-a-stack__state--configured">CONFIGURED</span>
                  <StackTechnology technology="kafka">Kafka scaffold</StackTechnology>
                  <StackTechnology technology="prometheus">Prometheus 2.54.1</StackTechnology>
                  <StackTechnology technology="grafana">Grafana 11.2.0</StackTechnology>
                  <span className="group-a-stack__qualifier">application path unfinished</span>
                </Cluster>
              </div>

              <div className="group-a-stack__band">
                <div className="group-a-stack__band-label"><span>05</span> DELIVERY / PRESENTATION</div>
                <Cluster className="group-a-stack__band-content" gap="lg" align="center" wrap>
                  <span className="group-a-stack__state group-a-stack__state--configured">CONFIGURED</span>
                  <StackTechnology technology="docker">Docker</StackTechnology>
                  <StackTechnology>Compose</StackTechnology>
                  <StackTechnology technology="github-actions">GitHub Actions verification</StackTechnology>
                  <StackTechnology>Reveal.js 6 · Playwright</StackTechnology>
                </Cluster>
              </div>
            </div>

            <BodyText className="group-a-stack__footer">
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
    </>
  );
}

export default GroupASlides;
