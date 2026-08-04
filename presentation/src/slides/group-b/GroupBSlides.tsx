import { Slide } from '@revealjs/react';
import { LiveDemoFrame } from '../../components/LiveDemoFrame';
import {
  BodyText,
  Callout,
  Cluster,
  Grid,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
} from '../../design-system';
import './GroupBSlides.css';

export interface GroupBSlidesProps {
  demoUrl: string;
}

const demoCheckpoints = [
  {
    number: '01',
    label: 'Login',
    detail: 'JWT + role check',
  },
  {
    number: '02',
    label: 'Create',
    detail: 'validated POST',
  },
  {
    number: '03',
    label: 'Persist',
    detail: 'HTTP 201 → PostgreSQL',
  },
  {
    number: '04',
    label: 'Observe',
    detail: 'create-time SSE → trade_created_total',
  },
] as const;

export function GroupBSlides({ demoUrl }: GroupBSlidesProps) {
  return (
    <>
      <Slide>
        <SlideCanvas background="blue" contentClassName="group-b-slide group-b-slide--demo">
          <Stack gap="md" className="group-b-demo-layout">
            <Cluster justify="between" align="start" wrap={false} className="group-b-slide-header">
              <Stack gap="xs" className="group-b-heading-block">
                <SlideLabel>05 / Live trade journey</SlideLabel>
                <SlideTitle>Live demo runway</SlideTitle>
                <BodyText variant="lede">
                  Login → validated trade → live update. The slide stays useful if the app does not.
                </BodyText>
              </Stack>
              <div className="group-b-time-mark" aria-label="Slide duration">4:00</div>
            </Cluster>

            <Grid columns={4} gap="md" className="group-b-runway" aria-label="Live demo checkpoints">
              {demoCheckpoints.map((checkpoint) => (
                <div className="group-b-checkpoint" key={checkpoint.number}>
                  <span className="group-b-checkpoint__node" aria-hidden="true">{checkpoint.number}</span>
                  <p className="group-b-checkpoint__label">{checkpoint.label}</p>
                  <p className="group-b-checkpoint__detail">{checkpoint.detail}</p>
                </div>
              ))}
            </Grid>

            <Cluster gap="sm" align="center" wrap={false} className="group-b-demo-status" aria-label="Prepared fallback status">
              <span className="group-b-demo-status__flag">Prepared fallback</span>
              <span>Runtime capture required before promotion</span>
              <span className="group-b-demo-status__separator" aria-hidden="true">/</span>
              <span>Launch the guarded frame only when ready</span>
            </Cluster>

            <div className="group-b-demo-frame" aria-label="Guarded ReconX live demo">
              <LiveDemoFrame src={demoUrl} />
            </div>
          </Stack>
        </SlideCanvas>
        <aside className="notes">
          Slide 5 · 4:00. Stable narrative: walk the room through one controlled trade journey and switch screens only at the four checkpoint boundaries.
          Evidence status: implemented for JWT login, role-checked trade creation, current DTO validation, PENDING persistence, HTTP 201, create-time SSE, and trade_created_total increment. Evidence status: configured for the guarded embedded-frame activation and readiness bridge. Evidence status: requires fresh runtime evidence for a clean-stack login, current numeric instrument/counterparty IDs, a unique reference, browser readiness, HTTP 201, PostgreSQL persistence, SSE reception, and metric movement.
          Keep credentials off the slide and out of screenshots. The frontend Trades route is not persisted-data proof. The exporter leaves LiveDemoFrame idle. If the live app is unavailable, use a prepared real 201 capture or Swagger/API source navigation; do not wait silently. Human input remains required for a private development account, unique reference convention, demo URL, presenter ownership, and a rehearsal capture if needed. Refresh after any auth, DTO, seed, SSE, metrics, bridge, runtime URL, or rehearsal change.
        </aside>
      </Slide>

      <Slide>
        <SlideCanvas background="ink" contentClassName="group-b-slide group-b-slide--reconciliation">
          <Stack gap="md" className="group-b-reconciliation-layout">
            <Cluster justify="between" align="start" wrap={false} className="group-b-slide-header">
              <Stack gap="xs" className="group-b-heading-block">
                <SlideLabel>06 / Reconciliation truth line</SlideLabel>
                <SlideTitle>Reconciliation truth line</SlideTitle>
                <BodyText variant="lede">
                  Core logic is real. The seam is visible.
                </BodyText>
              </Stack>
              <div className="group-b-time-mark" aria-label="Slide duration">4:00</div>
            </Cluster>

            <div className="group-b-trace" aria-label="Implemented reconciliation core and queued orchestration boundary">
              <div className="group-b-trace__core">
                <div className="group-b-trace-node group-b-trace-node--records">
                  <span className="group-b-trace-node__eyebrow">Inputs</span>
                  <strong>Internal + external records</strong>
                  <span>indexed by trade reference</span>
                </div>
                <div className="group-b-trace-node group-b-trace-node--engine">
                  <span className="group-b-trace-node__eyebrow">Implemented core</span>
                  <strong>ReconciliationEngine</strong>
                  <span>rule comparison + duration timer</span>
                </div>
                <div className="group-b-trace-node group-b-trace-node--outcomes">
                  <span className="group-b-trace-node__eyebrow">Outcomes</span>
                  <strong>MATCHED / BREAK</strong>
                  <span>direct engine result</span>
                </div>
              </div>

              <div className="group-b-trace__boundary">
                <div className="group-b-trace-node group-b-trace-node--queue">
                  <span className="group-b-trace-node__eyebrow">REST boundary</span>
                  <code>POST /api/v1/recon/run</code>
                  <strong>202 · QUEUED</strong>
                  <span>job row persisted</span>
                </div>
                <span className="group-b-break-marker" aria-hidden="true">worker boundary</span>
                <div className="group-b-trace-node group-b-trace-node--kafka">
                  <span className="group-b-trace-node__eyebrow">Configured only</span>
                  <strong>Apache Kafka</strong>
                  <span>broker / health path</span>
                </div>
              </div>
            </div>

            <Callout tone="critical" label="Reconciliation evidence boundary" title="Not observed">
              Queue has no worker; no application listener result is observed.
            </Callout>
          </Stack>
        </SlideCanvas>
        <aside className="notes">
          Slide 6 · 4:00. Stable narrative: teach the distinction between domain logic and integration as an engineering lesson, not as an apology.
          Evidence status: implemented for ReconciliationEngine indexing, trade-type comparison, MATCHED and break outcomes, direct ReconciliationService persistence, and reconciliation_duration around direct calls. Evidence status: configured for the recon controller queue row returning 202 QUEUED and for Kafka broker/dependency health configuration. Evidence status: gap for a worker consuming queued jobs, executable Kafka listeners, producer publication, job-scoped results, audit/DLQ routing, or a normal trade POST application event. Evidence status: requires fresh runtime evidence for any 202 response, direct-engine result, seeded-break action, or metric capture shown.
          Kafka is a muted configured node, not an observed application event route. Do not show a fabricated message, automatic result, retry/DLQ claim, audit row from trade creation, or resolved-break timer tick. The fallback is this source-level solid trace; a real 202 capture or manual seeded-break resolution may replace it only with an explicit caption. Refresh after engine, controller, repository, Kafka source, queue status, or successful event-observation changes. Reconciliation owner must confirm the safe direct-engine or seeded-break action before delivery.
        </aside>
      </Slide>
    </>
  );
}
