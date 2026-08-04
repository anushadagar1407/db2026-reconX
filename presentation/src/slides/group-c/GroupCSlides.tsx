import { Slide } from '@revealjs/react';
import type { ReactNode } from 'react';
import {
  BodyText,
  Callout,
  Cluster,
  Grid,
  SectionHeading,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
  TechIcon,
} from '../../design-system';
import './GroupCSlides.css';

const verificationJobs = [
  { name: 'Backend', detail: 'container / Compose', status: 'passed' },
  { name: 'Frontend', detail: 'container / Compose', status: 'passed' },
  { name: 'Presentation', detail: 'container / build', status: 'passed' },
] as const;

const dashboardPanels = [
  'request rate',
  'P95 latency',
  'open breaks',
  'trades / second',
  'reconciliation duration',
];

function PipelineGate({ detail, name, status }: (typeof verificationJobs)[number]) {
  return (
    <div className="group-c-pipeline__gate" data-status={status}>
      <span className="group-c-pipeline__status" aria-hidden="true" />
      <strong>{name}</strong>
      <span>{detail}</span>
    </div>
  );
}

function DeliverySlide() {
  return (
    <Slide aria-label="Slide 7: CI/CD delivery rail">
      <SlideCanvas className="group-c-slide group-c-slide--delivery" contentClassName="group-c-slide__content">
        <Stack gap="lg" className="group-c-slide__stack">
          <Cluster className="group-c-slide__header" align="end" justify="between" wrap={false}>
            <Stack gap="xs" className="group-c-slide__heading">
              <SlideLabel>Slide 07 · 11:00–12:40 · 1:40</SlideLabel>
              <SlideTitle>CI verifies the build — then stops</SlideTitle>
              <BodyText variant="lede">A commit-bound verification rail, not a release claim.</BodyText>
            </Stack>
            <div className="group-c-provenance" aria-label="Verified remote run provenance">
              <span className="group-c-provenance__label">Verified remote run</span>
              <code>PR #272 · run 30898100384</code>
              <code>commit 4f1a939a</code>
            </div>
          </Cluster>

          <div className="group-c-pipeline" data-testid="delivery-rail">
            <div className="group-c-pipeline__track" aria-hidden="true" />
            <Cluster className="group-c-pipeline__route" gap="md" align="stretch" wrap={false}>
              <div className="group-c-pipeline__origin">
                <span className="group-c-pipeline__eyebrow">Trigger</span>
                <strong>PR / manual dispatch</strong>
                <span>verification path</span>
              </div>

              <Grid columns={3} gap="sm" className="group-c-pipeline__gates">
                {verificationJobs.map((job) => (
                  <PipelineGate key={job.name} {...job} />
                ))}
              </Grid>

              <div className="group-c-pipeline__boundary" data-testid="push-boundary">
                <span className="group-c-pipeline__eyebrow">Build boundary</span>
                <code>push: false</code>
                <span>local image only</span>
              </div>

              <div className="group-c-pipeline__release-zone">
                <span className="group-c-pipeline__eyebrow">Not evidenced here</span>
                <strong>GHCR · deployment · release</strong>
                <span>no success arrow</span>
              </div>
            </Cluster>

            <Cluster className="group-c-pipeline__artifact-row" gap="md" align="center" wrap={false}>
              <div className="group-c-pipeline__artifact-spool">
                <span className="group-c-pipeline__eyebrow">Artifact spool</span>
                <span>JUnit / Vitest reports · image build outputs</span>
              </div>
              <div className="group-c-pipeline__artifact-note">
                <TechIcon technology="github-actions" size="sm" decorative />
                <span>attached to the named run</span>
              </div>
            </Cluster>
          </div>

          <Callout
            className="group-c-evidence-callout"
            label="CI evidence status"
            title="Passed on the named run"
            tone="success"
          >
            <p>Backend, frontend, and presentation verification passed; JUnit/Vitest report checks passed.</p>
            <p className="group-c-callout__muted">Load job and native fallbacks: skipped.</p>
          </Callout>
        </Stack>
      </SlideCanvas>
      <aside className="notes">
        Stable narrative: CI is a bounded verification rail. Evidence status: implemented for PR #272, run
        30898100384, commit 4f1a939a8ac9f1fd8916f2a5899f288bc708f603 after explicit repository-scoped
        revalidation. The passed jobs are backend, frontend, and presentation container verification plus the
        JUnit/Vitest report checks. The load job and native fallback jobs were skipped. Configured workflows build
        images with push: false; they do not prove GHCR, deployment, or release behavior. Refresh after workflow,
        image-push setting, remote-run, or source-commit changes. ADV160 current-state CI/CD Mermaid source remains
        a final-acceptance gap.
      </aside>
    </Slide>
  );
}

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
    <article className={`group-c-evidence-state ${className}`.trim()}>
      <div className="group-c-evidence-state__marker">
        <span>{number}</span>
        <span className="group-c-evidence-state__rule" aria-hidden="true" />
      </div>
      <SectionHeading className="group-c-evidence-state__title">{title}</SectionHeading>
      {children}
    </article>
  );
}

function MonitoringSlide() {
  return (
    <Slide aria-label="Slide 8: monitoring and load evidence">
      <SlideCanvas background="soft" className="group-c-slide group-c-slide--monitoring" contentClassName="group-c-slide__content">
        <Stack gap="lg" className="group-c-slide__stack">
          <Cluster className="group-c-slide__header" align="end" justify="between" wrap={false}>
            <Stack gap="xs" className="group-c-slide__heading">
              <SlideLabel>Slide 08 · 12:40–14:25 · 1:45</SlideLabel>
              <SlideTitle>Monitoring is provisioned; load proof is pending</SlideTitle>
              <BodyText variant="lede">The source has a dashboard. The run still needs to be captured.</BodyText>
            </Stack>
            <div className="group-c-monitoring-stamp" aria-label="Evidence path">
              <span>Evidence path</span>
              <strong>source → runtime → recovery</strong>
            </div>
          </Cluster>

          <Grid columns={3} gap="lg" className="group-c-evidence-strip" data-testid="evidence-strip">
            <EvidenceState number="01" title="Five panels are defined." className="group-c-evidence-state--source">
              <div className="group-c-panel-index" aria-label="Provisioned dashboard panels">
                {dashboardPanels.map((panel) => (
                  <span key={panel}>
                    <span aria-hidden="true">+</span>
                    {panel}
                  </span>
                ))}
              </div>
              <p className="group-c-evidence-state__copy">
                <code>reconx-backend</code> scrapes <code>/api/actuator/prometheus</code>.
              </p>
              <p className="group-c-evidence-state__caveat">
                Invalid in-container scrape: <code>localhost:8080</code>.
              </p>
            </EvidenceState>

            <EvidenceState number="02" title="No current image." className="group-c-evidence-state--capture">
              <div className="group-c-capture-placeholder" aria-label="Runtime capture pending">
                <span className="group-c-capture-placeholder__mark" aria-hidden="true">—</span>
                <strong>runtime capture pending</strong>
                <span>target state + panel values require a fresh run</span>
              </div>
              <p className="group-c-evidence-state__copy">A dashboard definition is not a measured screenshot.</p>
            </EvidenceState>

            <EvidenceState number="03" title="The profile is on disk." className="group-c-evidence-state--load">
              <p className="group-c-load-profile">
                <strong>10 VUs</strong>
                <span>×</span>
                <strong>10 iterations</strong>
                <span>=</span>
                <strong>100 trade creations</strong>
              </p>
              <p className="group-c-evidence-state__copy">Zero-failure checks are configured; no completed run is currently proven.</p>
              <div className="group-c-pending-line">
                <span>Next capture</span>
                <strong>200-VU baseline / load / recovery</strong>
                <span>pending</span>
              </div>
            </EvidenceState>
          </Grid>

          <Callout
            className="group-c-evidence-callout"
            label="Monitoring evidence status"
            title="Configured now · fresh runtime evidence required"
            tone="warning"
          >
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

export function GroupCSlides() {
  return (
    <>
      <DeliverySlide />
      <MonitoringSlide />
    </>
  );
}
