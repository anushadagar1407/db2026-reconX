import { Slide } from '@revealjs/react';
import {
  BodyText,
  Callout,
  SectionHeading,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
} from '../design-system';

export function ProblemSlide() {
  return (
    <Slide aria-label="Slide 2: Operations problem">
      <SlideCanvas background="paper" className="problem-slide-canvas">
        <Stack className="problem-slide" gap="lg">
          <Stack gap="sm">
            <SlideLabel>02 / OPERATIONS PROBLEM</SlideLabel>
            <SlideTitle>Mismatches need a controlled path</SlideTitle>
          </Stack>

          <div className="problem-slide__content">
            <Stack className="problem-slide__explanation" gap="lg">
              <SectionHeading>One trade, two records, one break to investigate.</SectionHeading>
              <BodyText>
                ReconX models the operator&apos;s journey from capture to comparison to evidence-backed
                resolution.
              </BodyText>
              <ul className="problem-slide__bullets">
                <li>Capture the internal trade record.</li>
                <li>Compare it with an external record.</li>
                <li>Investigate and resolve the break with evidence.</li>
              </ul>
            </Stack>

            <div className="problem-slide__ledger" aria-label="Illustrative internal and external trade ledger">
              <div className="problem-slide__ledger-caption">ILLUSTRATIVE DISCREPANCY · NOT PRODUCTION DATA</div>
              <div className="problem-slide__ledger-table">
                <div className="problem-slide__ledger-cell problem-slide__ledger-cell--blank" />
                <div className="problem-slide__ledger-cell problem-slide__ledger-cell--heading">INTERNAL RECORD</div>
                <div className="problem-slide__ledger-cell problem-slide__ledger-cell--heading">EXTERNAL RECORD</div>

                <div className="problem-slide__ledger-cell problem-slide__ledger-cell--field">trade reference</div>
                <div className="problem-slide__ledger-cell">same reference</div>
                <div className="problem-slide__ledger-cell">same reference</div>

                <div className="problem-slide__ledger-cell problem-slide__ledger-cell--field">quantity</div>
                <div className="problem-slide__ledger-cell">captured</div>
                <div className="problem-slide__ledger-cell">received</div>

                <div className="problem-slide__ledger-cell problem-slide__ledger-cell--field">price</div>
                <div className="problem-slide__ledger-cell problem-slide__ledger-cell--break">recorded value</div>
                <div className="problem-slide__ledger-cell problem-slide__ledger-cell--break">received value</div>
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
  );
}

export default ProblemSlide;
