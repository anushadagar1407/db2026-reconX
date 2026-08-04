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

const repositoryUrl = 'https://github.com/anushadagar1407/db2026-reconX';
const questionRoutes = [
  { index: '01', label: 'API', detail: 'Trade boundary and request contract' },
  { index: '02', label: 'Recon', detail: 'Matching core and orchestration seam' },
  { index: '03', label: 'Runtime', detail: 'Compose path and observability' },
  { index: '04', label: 'Delivery', detail: 'Verification and source provenance' },
];

export function QuestionsSlide() {
  return (
    <Slide aria-label="Slide 10: Questions and repository navigation">
      <SlideCanvas background="blue" contentClassName="questions-slide">
        <Split className="questions-slide__header" ratio="3:2" gap="2xl" align="start">
          <Stack gap="md">
            <SlideLabel>10 / OPEN FLOOR</SlideLabel>
            <SlideTitle>Questions?</SlideTitle>
            <BodyText variant="lede">Follow the evidence to the layer that needs a closer look.</BodyText>
          </Stack>

          <Stack className="questions-slide__routes" gap="xs">
            <SlideLabel>ROUTE THE QUESTION</SlideLabel>
            {questionRoutes.map((route) => (
              <div className="questions-slide__route" key={route.index}>
                <span className="questions-slide__route-index" aria-hidden="true">{route.index}</span>
                <Stack gap="2xs">
                  <SectionHeading>{route.label}</SectionHeading>
                  <p>{route.detail}</p>
                </Stack>
              </div>
            ))}
          </Stack>
        </Split>

        <Stack className="questions-slide__anchors" gap="sm">
          <SlideLabel>REPOSITORY / SOURCE</SlideLabel>
          <a className="questions-slide__url" href={repositoryUrl}>{repositoryUrl}</a>
          <Cluster className="questions-slide__anchor-list" gap="sm" wrap>
            <span>Open anchors</span>
            <span>TradeController</span>
            <span>ReconciliationEngine</span>
            <span>docker-compose.yml</span>
          </Cluster>
        </Stack>

        <p className="questions-slide__footer">A sourced answer is better than a finished-sounding one.</p>
      </SlideCanvas>
      <aside className="notes">
        Evidence status: implemented source anchors and supplied repository URL; requires human input for named
        question owners and final tab routing; delivery status remains unverified. Route API, recon, runtime, and
        delivery questions to roster-backed owners when confirmed. Pre-stage TradeController, TradeService,
        ReconciliationEngine, useTradeStream, docker-compose.yml, build.yml, and the Grafana dashboard definition.
        Fallback: keep the URL visible and navigate local source if the remote page is unavailable. Refresh after
        URL approval, source-file moves, routing changes, or fresh delivery evidence. Do not add an unsupported
        status badge.
      </aside>
    </Slide>
  );
}

export default QuestionsSlide;
