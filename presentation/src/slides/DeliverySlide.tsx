import { Slide } from '@revealjs/react';
import {
  BodyText,
  Callout,
  Cluster,
  Grid,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
  TechIcon,
} from '../design-system';

const verificationJobs = [
  { name: 'Backend', detail: 'container / Compose', status: 'passed' },
  { name: 'Frontend', detail: 'container / Compose', status: 'passed' },
  { name: 'Presentation', detail: 'container / build', status: 'passed' },
] as const;

function PipelineGate({ detail, name, status }: (typeof verificationJobs)[number]) {
  return (
    <div className="delivery-slide__pipeline-gate" data-status={status}>
      <span className="delivery-slide__pipeline-status" aria-hidden="true" />
      <strong>{name}</strong>
      <span>{detail}</span>
    </div>
  );
}

export function DeliverySlide() {
  return (
    <Slide aria-label="Slide 7: CI/CD delivery rail">
      <SlideCanvas className="delivery-slide-canvas" contentClassName="delivery-slide">
        <Stack gap="lg" className="delivery-slide__stack">
          <Cluster className="delivery-slide__header" align="end" justify="between" wrap={false}>
            <Stack gap="xs" className="delivery-slide__heading">
              <SlideLabel>07 / CI/CD DELIVERY RAIL · 11:00–12:40</SlideLabel>
              <SlideTitle>CI verifies the build — then stops</SlideTitle>
              <BodyText variant="lede">A commit-bound verification rail, not a release claim.</BodyText>
            </Stack>
            <div className="delivery-slide__provenance" aria-label="Verified remote run provenance">
              <span className="delivery-slide__provenance-label">Verified remote run</span>
              <code>PR #272 · run 30898100384</code>
              <code>commit 4f1a939a</code>
            </div>
          </Cluster>

          <div className="delivery-slide__pipeline" data-testid="delivery-rail">
            <div className="delivery-slide__pipeline-track" aria-hidden="true" />
            <Cluster className="delivery-slide__pipeline-route" gap="md" align="stretch" wrap={false}>
              <div className="delivery-slide__pipeline-origin">
                <span className="delivery-slide__pipeline-eyebrow">Trigger</span>
                <strong>PR / manual dispatch</strong>
                <span>verification path</span>
              </div>

              <Grid columns={3} gap="sm" className="delivery-slide__pipeline-gates">
                {verificationJobs.map((job) => (
                  <PipelineGate key={job.name} {...job} />
                ))}
              </Grid>

              <div className="delivery-slide__pipeline-boundary" data-testid="push-boundary">
                <span className="delivery-slide__pipeline-eyebrow">Build boundary</span>
                <code>push: false</code>
                <span>build completed · not pushed, loaded, or exported</span>
              </div>

              <div className="delivery-slide__release-zone">
                <span className="delivery-slide__pipeline-eyebrow">Not evidenced here</span>
                <strong>GHCR · deployment · release</strong>
                <span>no success arrow</span>
              </div>
            </Cluster>

            <Cluster className="delivery-slide__artifact-row" gap="md" align="center" wrap={false}>
              <div className="delivery-slide__artifact-spool">
                <span className="delivery-slide__pipeline-eyebrow">Artifact spool</span>
                <span>JUnit / Failsafe / Vitest / JaCoCo reports</span>
              </div>
              <div className="delivery-slide__artifact-note">
                <TechIcon technology="github-actions" size="sm" decorative />
                <span>uploaded by full verification jobs</span>
              </div>
            </Cluster>
          </div>

          <Callout className="delivery-slide__evidence-callout" label="CI evidence status" title="Passed on the named run" tone="success">
            <p>Backend, frontend, and presentation verification passed; JUnit/Vitest report checks passed.</p>
            <p className="delivery-slide__callout-muted">Image builds completed without push, load, or export. Load job and native fallbacks: skipped.</p>
          </Callout>
        </Stack>
      </SlideCanvas>
      <aside className="notes">
        Stable narrative: CI is a bounded verification rail. Evidence status: implemented for PR #272, run
        30898100384, commit 4f1a939a8ac9f1fd8916f2a5899f288bc708f603 after explicit repository-scoped
        revalidation. The passed jobs are backend, frontend, and presentation container verification plus the
        JUnit/Vitest report checks. The load job and native fallback jobs were skipped. Configured workflows build
        images with push: false; Buildx does not push, load, or export image outputs. Full verification uploads the
        Surefire/Failsafe/JaCoCo and Vitest reports. The separate presentation-export workflow uploads PNG/PDF exports
        only on pushes to develop/main or manual dispatch. No GHCR, deployment, or release behavior is evidenced.
        Refresh after workflow, image-push setting, remote-run, or source-commit changes. The README companion flow
        is the current-state ADV160 CI/CD source.
      </aside>
    </Slide>
  );
}

export default DeliverySlide;
