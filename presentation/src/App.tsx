import { Deck, Slide } from '@revealjs/react';
import RevealNotes from 'reveal.js/plugin/notes';
import { LiveDemoFrame } from './components/LiveDemoFrame';
import { getDemoUrl } from './config/runtime';
import {
  BodyText,
  BrandMark,
  Cluster,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
} from './design-system';

const deckConfig = {
  width: 1920,
  height: 1080,
  margin: 0.02,
  controls: true,
  progress: true,
  hash: true,
  center: false,
  transition: 'slide' as const,
  pdfSeparateFragments: false,
};

export default function App() {
  const demoUrl = getDemoUrl();

  return (
    <Deck plugins={[RevealNotes]} config={deckConfig}>
      <Slide>
        <SlideCanvas>
          <Stack className="scaffold-slide" gap="md">
            <Cluster justify="between" wrap={false}>
              <BrandMark size="md" />
              <SlideLabel>TICKET-ADV162</SlideLabel>
            </Cluster>
            <Stack gap="sm">
              <SlideTitle>ReconX presentation scaffold</SlideTitle>
              <BodyText variant="lede">
                Reveal.js and the live-demo boundary are ready for slide authoring.
              </BodyText>
            </Stack>
            <div className="scaffold-slide__demo">
              <LiveDemoFrame src={demoUrl} />
            </div>
          </Stack>
        </SlideCanvas>
        <aside className="notes">
          Setup-only slide. Replace it when presentation content is authored.
        </aside>
      </Slide>
    </Deck>
  );
}
