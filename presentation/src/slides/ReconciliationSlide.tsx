import { Slide } from '@revealjs/react';
import {
  Callout,
  Cluster,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
} from '../design-system';

export function ReconciliationSlide() {
  return (
    <Slide aria-label="Slide 6: Reconciliation truth line">
      <SlideCanvas background="ink" contentClassName="reconciliation-slide">
        <Stack gap="md" className="reconciliation-slide__layout">
          <Cluster justify="between" align="start" wrap={false} className="reconciliation-slide__header">
            <Stack gap="xs" className="reconciliation-slide__heading-block">
              <SlideLabel>06 / RECONCILIATION TRUTH LINE</SlideLabel>
              <SlideTitle>Reconciliation truth line</SlideTitle>
              <p className="reconciliation-slide__lede">Core logic is real. The seam is visible.</p>
            </Stack>
            <div className="reconciliation-slide__time-mark" aria-label="Slide duration">4:00</div>
          </Cluster>

          <div className="reconciliation-slide__trace" aria-label="Implemented reconciliation core and queued orchestration boundary">
            <div className="reconciliation-slide__trace-core">
              <div className="reconciliation-slide__trace-node reconciliation-slide__trace-node--records">
                <span className="reconciliation-slide__trace-eyebrow">Inputs</span>
                <strong>Internal + external records</strong>
                <span>indexed by trade reference</span>
              </div>
              <div className="reconciliation-slide__trace-node reconciliation-slide__trace-node--engine">
                <span className="reconciliation-slide__trace-eyebrow">Implemented core</span>
                <strong>ReconciliationEngine</strong>
                <span>rule comparison + duration timer</span>
              </div>
              <div className="reconciliation-slide__trace-node reconciliation-slide__trace-node--outcomes">
                <span className="reconciliation-slide__trace-eyebrow">Outcomes</span>
                <strong>MATCHED / BREAK</strong>
                <span>direct engine result</span>
              </div>
            </div>

            <div className="reconciliation-slide__trace-boundary">
              <div className="reconciliation-slide__trace-node reconciliation-slide__trace-node--queue">
                <span className="reconciliation-slide__trace-eyebrow">REST boundary</span>
                <code>POST /api/v1/recon/run</code>
                <strong>202 · QUEUED</strong>
                <span>job row persisted</span>
              </div>
              <span className="reconciliation-slide__break-marker" aria-hidden="true">worker boundary</span>
              <div className="reconciliation-slide__trace-node reconciliation-slide__trace-node--kafka">
                <span className="reconciliation-slide__trace-eyebrow">Configured only</span>
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
        Slide 6 · 4:00. Stable narrative: teach the distinction between domain logic and integration as an
        engineering lesson, not as an apology. Evidence status: implemented for ReconciliationEngine indexing,
        trade-type comparison, MATCHED and break outcomes, direct ReconciliationService persistence, and
        reconciliation_duration around direct calls. Evidence status: configured for the recon controller queue
        row returning 202 QUEUED and Kafka broker/dependency health configuration. Evidence status: gap for a worker
        consuming queued jobs, executable Kafka listeners, producer publication, job-scoped results, audit/DLQ
        routing, or a normal trade POST application event. Evidence status: requires fresh runtime evidence for
        any 202 response, direct-engine result, seeded-break action, or metric capture shown. Kafka is a muted
        configured node, not an observed application event route. Refresh after engine, controller, repository,
        Kafka source, queue status, or successful event-observation changes.
      </aside>
    </Slide>
  );
}

export default ReconciliationSlide;
