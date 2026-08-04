import { Slide } from '@revealjs/react';
import { LiveDemoFrame } from '../components/LiveDemoFrame';
import {
  BodyText,
  Cluster,
  Grid,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
} from '../design-system';

export interface LiveDemoSlideProps {
  demoUrl: string;
}

const demoCheckpoints = [
  { number: '01', label: 'Login', detail: 'JWT + role check' },
  { number: '02', label: 'Create', detail: 'validated POST' },
  { number: '03', label: 'Persist', detail: 'HTTP 201 → PostgreSQL' },
  { number: '04', label: 'Observe', detail: 'create-time SSE → trade_created_total' },
] as const;

export function LiveDemoSlide({ demoUrl }: LiveDemoSlideProps) {
  return (
    <Slide aria-label="Slide 5: Live trade journey">
      <SlideCanvas background="blue" contentClassName="live-demo-slide live-demo-slide--demo">
        <Stack gap="md" className="live-demo-slide__layout">
          <Cluster justify="between" align="start" wrap={false} className="live-demo-slide__header">
            <Stack gap="xs" className="live-demo-slide__heading-block">
              <SlideLabel>05 / LIVE TRADE JOURNEY</SlideLabel>
              <SlideTitle>Live demo runway</SlideTitle>
              <BodyText variant="lede">
                Login → validated trade → live update. The slide stays useful if the app does not.
              </BodyText>
            </Stack>
            <div className="live-demo-slide__time-mark" aria-label="Slide duration">4:00</div>
          </Cluster>

          <Grid columns={4} gap="md" className="live-demo-slide__runway" aria-label="Live demo checkpoints">
            {demoCheckpoints.map((checkpoint) => (
              <div className="live-demo-slide__checkpoint" key={checkpoint.number}>
                <span className="live-demo-slide__checkpoint-node" aria-hidden="true">{checkpoint.number}</span>
                <p className="live-demo-slide__checkpoint-label">{checkpoint.label}</p>
                <p className="live-demo-slide__checkpoint-detail">{checkpoint.detail}</p>
              </div>
            ))}
          </Grid>

          <Cluster gap="sm" align="center" wrap={false} className="live-demo-slide__status" aria-label="Live demo fallback status">
            <span className="live-demo-slide__status-flag">Runtime capture pending</span>
            <span>Launch the guarded frame when ready</span>
            <span className="live-demo-slide__status-separator" aria-hidden="true">/</span>
            <span>If unavailable: full-demo link or source/API walkthrough</span>
          </Cluster>

          <div className="live-demo-slide__frame" aria-label="Guarded ReconX live demo">
            <LiveDemoFrame src={demoUrl} />
          </div>
        </Stack>
      </SlideCanvas>
      <aside className="notes">
        Slide 5 · 4:00. Stable narrative: walk the room through one controlled trade journey and switch screens
        only at the four checkpoint boundaries. Evidence status: implemented for JWT login, role-checked trade
        creation, current DTO validation, PENDING persistence, HTTP 201, create-time SSE, and trade_created_total
        increment. Evidence status: configured for the guarded embedded-frame activation and readiness bridge.
        Evidence status: requires fresh runtime evidence for a clean-stack login, current numeric instrument/
        counterparty IDs, a unique reference, browser readiness, HTTP 201, PostgreSQL persistence, SSE reception,
        and metric movement. Keep credentials off the slide and out of screenshots. The exporter leaves the frame
        idle and no capture or recording asset ships with the deck. If the live app is unavailable, use the full-demo
        link or switch to source/API navigation. Human input remains required for a private development account,
        unique reference convention, demo URL, and presenter ownership. Refresh after auth, DTO, seed, SSE, metrics,
        bridge, runtime URL, or runtime-capture changes.
      </aside>
    </Slide>
  );
}

export default LiveDemoSlide;
