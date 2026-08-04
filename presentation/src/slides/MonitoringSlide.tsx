import { Slide } from '@revealjs/react';
import type { ReactNode } from 'react';
import {
  Callout,
  Grid,
  SectionHeading,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
} from '../design-system';

const dashboardPanels = ['request rate', 'P95 latency', 'open breaks', 'trades / second', 'reconciliation duration'];

function EvidenceState({
  children,
  number,
  title,
  className = '',
}: {
  children: ReactNode;
  number: string;
  title: string;
  className?: string;
}) {
  return (
    <article className={`monitoring-slide__evidence-state ${className}`.trim()}>
      <div className="monitoring-slide__evidence-marker">
        <span>{number}</span>
        <span className="monitoring-slide__evidence-rule" aria-hidden="true" />
      </div>
      <SectionHeading className="monitoring-slide__evidence-title">{title}</SectionHeading>
      {children}
    </article>
  );
}

export function MonitoringSlide() {
  return (
    <Slide aria-label="Slide 8: Monitoring and load evidence">
      <SlideCanvas background="soft" className="monitoring-slide-canvas" contentClassName="monitoring-slide">
        <Stack gap="lg" className="monitoring-slide__stack">
          <div className="monitoring-slide__header">
            <Stack gap="xs" className="monitoring-slide__heading">
              <SlideLabel>08 / MONITORING &amp; LOAD EVIDENCE · 12:40–14:25</SlideLabel>
              <SlideTitle>Monitoring is provisioned; load proof is pending</SlideTitle>
              <p className="monitoring-slide__lede">The source has a dashboard. The run still needs to be captured.</p>
            </Stack>
            <div className="monitoring-slide__stamp" aria-label="Evidence path">
              <span>Evidence path</span>
              <strong>source → runtime → recovery</strong>
            </div>
          </div>

          <Grid columns={3} gap="lg" className="monitoring-slide__evidence-strip" data-testid="evidence-strip">
            <EvidenceState number="01" title="Five panels are defined." className="monitoring-slide__evidence-state--source">
              <div className="monitoring-slide__panel-index" aria-label="Provisioned dashboard panels">
                {dashboardPanels.map((panel) => (
                  <span key={panel}>
                    <span aria-hidden="true">+</span>
                    {panel}
                  </span>
                ))}
              </div>
              <p className="monitoring-slide__evidence-copy">
                <code>reconx-backend</code> scrapes <code>/api/actuator/prometheus</code>.
              </p>
              <p className="monitoring-slide__evidence-caveat">
                Invalid in-container scrape: <code>localhost:8080</code>.
              </p>
            </EvidenceState>

            <EvidenceState number="02" title="No current image." className="monitoring-slide__evidence-state--capture">
              <div className="monitoring-slide__capture-placeholder" aria-label="Runtime capture pending">
                <span className="monitoring-slide__capture-mark" aria-hidden="true">—</span>
                <strong>runtime capture pending</strong>
                <span>target state + panel values require a fresh run</span>
              </div>
              <p className="monitoring-slide__evidence-copy">A dashboard definition is not a measured screenshot.</p>
            </EvidenceState>

            <EvidenceState number="03" title="The profile is on disk." className="monitoring-slide__evidence-state--load">
              <p className="monitoring-slide__load-profile">
                <strong>10 VUs</strong>
                <span>×</span>
                <strong>10 iterations</strong>
                <span>=</span>
                <strong>100 trade creations</strong>
              </p>
              <p className="monitoring-slide__evidence-copy">Zero-failure checks are configured; no completed run is currently proven.</p>
              <div className="monitoring-slide__pending-line">
                <span>Next capture</span>
                <strong>200-VU baseline / load / recovery</strong>
                <span>pending</span>
              </div>
            </EvidenceState>
          </Grid>

          <Callout className="monitoring-slide__evidence-callout" label="Monitoring evidence status" title="Configured now · fresh runtime evidence required" tone="warning">
            <p>Keep the current 10-VU / 100-create truth. Do not promote a 200-VU success, latency SLA, or recovery claim without a captured run and provenance.</p>
          </Callout>
        </Stack>
      </SlideCanvas>
      <aside className="notes">
        Stable narrative: observability is source, runtime, and provenance. Evidence status: configured for the
        backend scrape, Prometheus self-scrape, datasource/provider, and five dashboard panels. The actual load
        profile is 10 VUs with 10 iterations each, exactly 100 trade creations; its zero-failure checks are
        configured. Runtime target state, panel values, and load captures require fresh runtime evidence. The
        200-VU baseline/load/recovery set is a gap, not a success claim. Refresh after monitoring, dashboard, load
        script, validator, runtime, or screenshot changes. Handoff source tabs: TradeMetrics.java, prometheus.yml,
        reconx-overview.json, and the load script.
      </aside>
    </Slide>
  );
}

export default MonitoringSlide;
