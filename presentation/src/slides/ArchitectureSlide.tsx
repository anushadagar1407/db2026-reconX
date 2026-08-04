import { Slide } from '@revealjs/react';
import {
  Cluster,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
  TechIcon,
} from '../design-system';

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
    <div className={`architecture-slide__node architecture-slide__node--${state}`}>
      <Cluster gap="xs" align="center" wrap={false}>
        {technology ? <TechIcon technology={technology} size="sm" decorative /> : null}
        <span className="architecture-slide__node-name">{children}</span>
      </Cluster>
      <span className="architecture-slide__node-detail">{detail}</span>
    </div>
  );
}

export function ArchitectureSlide() {
  return (
    <Slide aria-label="Slide 3: Current runtime architecture">
      <SlideCanvas background="soft" className="architecture-slide-canvas">
        <Stack className="architecture-slide" gap="md">
          <Cluster className="architecture-slide__header" justify="between" align="start" wrap={false}>
            <Stack gap="sm">
              <SlideLabel>03 / CURRENT RUNTIME BOUNDARY</SlideLabel>
              <SlideTitle>Architecture, as it runs today</SlideTitle>
            </Stack>
            <span className="architecture-slide__status">SOURCE-SUPPORTED PATH</span>
          </Cluster>

          <p className="architecture-slide__lede">
            Capture and observation are source-supported; event orchestration remains unfinished.
          </p>

          <div className="architecture-slide__stage" aria-label="Current runtime architecture">
            <div className="architecture-slide__active-route">
              <div className="architecture-slide__route-label">ACTIVE PATH · IMPLEMENTED</div>
              <div className="architecture-slide__flow">
                <ArchitectureNode state="active" detail="browser">Browser</ArchitectureNode>
                <span className="architecture-slide__arrow" aria-hidden="true">→</span>
                <ArchitectureNode technology="nginx" state="active" detail="frontend proxy">nginx</ArchitectureNode>
                <span className="architecture-slide__arrow" aria-hidden="true">→</span>
                <ArchitectureNode technology="spring" state="active" detail="REST API">Spring API</ArchitectureNode>
                <span className="architecture-slide__arrow" aria-hidden="true">→</span>
                <ArchitectureNode technology="postgresql" state="active" detail="durable record">PostgreSQL</ArchitectureNode>
              </div>
            </div>

            <div className="architecture-slide__branches">
              <div className="architecture-slide__branch architecture-slide__branch--sse">
                <span className="architecture-slide__branch-label">LIVE FEEDBACK · IMPLEMENTED</span>
                <span className="architecture-slide__branch-line" aria-hidden="true">↩</span>
                <span>SSE on trade create → browser</span>
              </div>

              <div className="architecture-slide__branch architecture-slide__branch--observability">
                <span className="architecture-slide__branch-label">METRICS · CONFIGURED</span>
                <span className="architecture-slide__branch-source">Spring API</span>
                <span className="architecture-slide__arrow" aria-hidden="true">→</span>
                <ArchitectureNode state="configured" detail="scrape">Prometheus</ArchitectureNode>
                <span className="architecture-slide__arrow" aria-hidden="true">→</span>
                <ArchitectureNode state="configured" detail="panels">Grafana</ArchitectureNode>
              </div>

              <div className="architecture-slide__branch architecture-slide__branch--infrastructure">
                <span className="architecture-slide__branch-label">COMPOSE TOPOLOGY · CONFIGURED</span>
                <div className="architecture-slide__infrastructure-flow">
                  <span className="architecture-slide__branch-source">Spring API</span>
                  <span className="architecture-slide__neutral-link" aria-hidden="true">↔</span>
                  <ArchitectureNode technology="kafka" state="configured" detail="broker">Kafka</ArchitectureNode>
                  <span className="architecture-slide__neutral-link" aria-hidden="true">—</span>
                  <ArchitectureNode state="configured" detail="coordination">Zookeeper</ArchitectureNode>
                </div>
                <span className="architecture-slide__infra-note">Spring ↔ Kafka: bootstrap / health only</span>
              </div>
            </div>

            <div className="architecture-slide__gap" aria-label="Kafka application orchestration gap">
              <Cluster className="architecture-slide__gap-layout" justify="between" align="center" wrap={false}>
                <Stack gap="2xs">
                  <div className="architecture-slide__gap-title">KAFKA APPLICATION ORCHESTRATION — GAP</div>
                  <div className="architecture-slide__gap-copy">
                    Application event → reconciliation worker is not implemented; this is not a live arrow.
                  </div>
                </Stack>
                <Stack className="architecture-slide__gap-evidence" gap="xs">
                  <div className="architecture-slide__gap-route" aria-hidden="true">
                    <span>Spring API</span>
                    <span className="architecture-slide__gap-dash">application event</span>
                    <strong>STOP</strong>
                    <span>worker</span>
                  </div>
                  <Cluster className="architecture-slide__legend" gap="md" wrap={false}>
                    <span><i className="architecture-slide__swatch architecture-slide__swatch--active" /> implemented</span>
                    <span><i className="architecture-slide__swatch architecture-slide__swatch--configured" /> configured</span>
                    <span><i className="architecture-slide__swatch architecture-slide__swatch--gap" /> gap</span>
                  </Cluster>
                </Stack>
              </Cluster>
            </div>
          </div>
        </Stack>
      </SlideCanvas>
      <aside className="notes">
        Evidence status: implemented for REST capture, PostgreSQL persistence, create-time SSE, and metric
        registration; configured for the seven-service Compose topology, dependency gates, Prometheus scrape,
        and Grafana provisioning; gap for producer/listener flow, queued-job worker, audit/DLQ path, and complete
        health proof; requires fresh runtime evidence for reachability or healthy services. The README companion
        diagram is the current-state ADV160 source for these active, configured, and gap semantics.
      </aside>
    </Slide>
  );
}

export default ArchitectureSlide;
