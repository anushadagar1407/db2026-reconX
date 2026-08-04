import { Slide } from '@revealjs/react';
import {
  BodyText,
  Cluster,
  SectionHeading,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Split,
  Stack,
} from '../design-system';

const learningNotes = [
  { index: '01', title: 'Configuration is not integration.', detail: 'Promote a path only after its behavior is observable end to end.' },
  { index: '02', title: 'Contract drift breaks rehearsals.', detail: 'Keep the current request shape beside the walkthrough.' },
  { index: '03', title: 'Evidence needs provenance.', detail: 'Attach source, run, and capture context to every claim.' },
];

export function LearningsSlide() {
  return (
    <Slide aria-label="Slide 9: Learnings">
      <SlideCanvas background="paper" contentClassName="learnings-slide">
        <Cluster className="learnings-slide__header" justify="between" align="start" wrap={false}>
          <Stack gap="2xs">
            <SlideLabel>09 / AFTER-ACTION NOTES</SlideLabel>
            <SlideTitle>Learnings we can defend</SlideTitle>
          </Stack>
          <p className="learnings-slide__timing">1:35 · reflection before the close</p>
        </Cluster>

        <Split className="learnings-slide__body" ratio="2:1" gap="xl" align="stretch">
          <div className="learnings-slide__notes" aria-label="Team learning prompts">
            <div className="learnings-slide__margin" aria-hidden="true" />
            <Stack gap="xs">
              {learningNotes.map((note) => (
                <article className="learnings-slide__note" key={note.index}>
                  <span className="learnings-slide__note-index" aria-hidden="true">{note.index}</span>
                  <Stack gap="2xs">
                    <SectionHeading>{note.title}</SectionHeading>
                    <BodyText>{note.detail}</BodyText>
                  </Stack>
                </article>
              ))}
            </Stack>
          </div>

          <Stack className="learnings-slide__pending" gap="sm" justify="start">
            <div>
              <SlideLabel>DELIVERY CHECK</SlideLabel>
              <SectionHeading>Presenter reflection pending</SectionHeading>
            </div>
            <BodyText>Personalize with confirmed sentences before delivery. Keep the prompts team-level until then.</BodyText>
            <p className="learnings-slide__status">No attribution · no rehearsal outcome · no invented voice</p>
          </Stack>
        </Split>
      </SlideCanvas>
      <aside className="notes">
        Evidence status: implemented prompts; requires human input for presenter reflections; gap for retrospective
        and rehearsal artifacts. Use the three prompts as a neutral team handoff. At 14:25–15:15, the confirmed
        application/reconciliation/SSE owner may open TradeController, ReconciliationEngine, and useTradeStream.
        At 15:15–16:00, the lead and confirmed contributors supply only approved sentences. Fallback: say
        “reflection pending.” Refresh after presenter input, a retrospective artifact, or a source-contract/
        provenance change. No personal attribution, rehearsal result, or delivery outcome is implied.
      </aside>
    </Slide>
  );
}

export default LearningsSlide;
