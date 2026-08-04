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
} from '../../design-system';
import './GroupDSlides.css';

const repositoryUrl = 'https://github.com/anushadagar1407/db2026-reconX';

const learningNotes = [
  {
    index: '01',
    title: 'Configuration is not integration.',
    detail: 'Promote a path only after its behavior is observable end to end.',
  },
  {
    index: '02',
    title: 'Contract drift breaks rehearsals.',
    detail: 'Keep the current request shape beside the walkthrough.',
  },
  {
    index: '03',
    title: 'Evidence needs provenance.',
    detail: 'Attach source, run, and capture context to every claim.',
  },
];

const questionRoutes = [
  { index: '01', label: 'API', detail: 'Trade boundary and request contract' },
  { index: '02', label: 'Recon', detail: 'Matching core and orchestration seam' },
  { index: '03', label: 'Runtime', detail: 'Compose path and observability' },
  { index: '04', label: 'Delivery', detail: 'Verification and source provenance' },
];

export function GroupDSlides() {
  return (
    <>
      <Slide>
        <SlideCanvas
          aria-label="Slide 9: Learnings"
          data-group-d-slide="9"
          contentClassName="group-d-slide group-d-learning"
        >
          <Cluster className="group-d-learning__header" justify="between" align="end" wrap={false}>
            <Stack gap="2xs">
              <SlideLabel>09 / AFTER-ACTION NOTES</SlideLabel>
              <SlideTitle>Learnings we can defend</SlideTitle>
            </Stack>
            <p className="group-d-slide__timing">1:35 · reflection before the close</p>
          </Cluster>

          <Split className="group-d-learning__body" ratio="2:1" gap="xl" align="stretch">
            <div className="group-d-learning__notes" aria-label="Team learning prompts">
              <div className="group-d-learning__margin" aria-hidden="true" />
              <Stack gap="xs">
                {learningNotes.map((note) => (
                  <article className="group-d-learning__note" key={note.index}>
                    <span className="group-d-learning__note-index" aria-hidden="true">{note.index}</span>
                    <Stack gap="2xs">
                      <SectionHeading>{note.title}</SectionHeading>
                      <BodyText>{note.detail}</BodyText>
                    </Stack>
                  </article>
                ))}
              </Stack>
            </div>

            <Stack className="group-d-learning__pending" gap="sm" justify="end">
              <div>
                <SlideLabel>DELIVERY CHECK</SlideLabel>
                <SectionHeading>Presenter reflection pending</SectionHeading>
              </div>
              <BodyText>
                Personalize with confirmed sentences before delivery. Keep the prompts team-level until then.
              </BodyText>
              <p className="group-d-learning__status">No attribution · no rehearsal outcome · no invented voice</p>
            </Stack>
          </Split>
        </SlideCanvas>
        <aside className="notes">
          Evidence status: implemented prompts; requires human input for presenter reflections; gap for retrospective and rehearsal artifacts.
          Use the three prompts as a neutral team handoff. At 14:25–15:15, the confirmed application/reconciliation/SSE owner may open
          TradeController, ReconciliationEngine, and useTradeStream. At 15:15–16:00, the lead and confirmed contributors supply only
          approved sentences. Fallback: say “reflection pending.” Refresh after presenter input, a retrospective artifact, or a
          source-contract/provenance change. No personal attribution, rehearsal result, or delivery outcome is implied.
        </aside>
      </Slide>

      <Slide>
        <SlideCanvas
          aria-label="Slide 10: Questions and repository navigation"
          data-group-d-slide="10"
          contentClassName="group-d-slide group-d-qna"
          background="blue"
        >
          <Split className="group-d-qna__header" ratio="3:2" gap="2xl" align="start">
            <Stack gap="md">
              <SlideLabel>10 / OPEN FLOOR</SlideLabel>
              <SlideTitle>Questions?</SlideTitle>
              <BodyText variant="lede">
                Follow the evidence to the layer that needs a closer look.
              </BodyText>
            </Stack>

            <Stack className="group-d-qna__routes" gap="xs">
              <SlideLabel>ROUTE THE QUESTION</SlideLabel>
              {questionRoutes.map((route) => (
                <div className="group-d-qna__route" key={route.index}>
                  <span className="group-d-qna__route-index" aria-hidden="true">{route.index}</span>
                  <Stack gap="2xs">
                    <SectionHeading>{route.label}</SectionHeading>
                    <p>{route.detail}</p>
                  </Stack>
                </div>
              ))}
            </Stack>
          </Split>

          <Stack className="group-d-qna__anchors" gap="sm">
            <SlideLabel>REPOSITORY / SOURCE</SlideLabel>
            <a className="group-d-qna__url" href={repositoryUrl}>{repositoryUrl}</a>
            <Cluster className="group-d-qna__anchor-list" gap="sm" wrap>
              <span>Open anchors</span>
              <span>TradeController</span>
              <span>ReconciliationEngine</span>
              <span>docker-compose.yml</span>
            </Cluster>
          </Stack>

          <p className="group-d-qna__footer">A sourced answer is better than a finished-sounding one.</p>
        </SlideCanvas>
        <aside className="notes">
          Evidence status: implemented source anchors and supplied repository URL; requires human input for named question owners and
          final tab routing; delivery status remains unverified. Route API, recon, runtime, and delivery questions to roster-backed
          owners when confirmed. Pre-stage TradeController, TradeService, ReconciliationEngine, useTradeStream, docker-compose.yml,
          build.yml, and the Grafana dashboard definition. Fallback: keep the URL visible and navigate local source if the remote page
          is unavailable. Refresh after URL approval, source-file moves, routing changes, or fresh delivery evidence. Do not add an
          unsupported status badge.
        </aside>
      </Slide>
    </>
  );
}

export default GroupDSlides;
