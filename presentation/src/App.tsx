import { Deck, Slide } from '@revealjs/react';
import RevealNotes from 'reveal.js/plugin/notes';
import { LiveDemoFrame } from './components/LiveDemoFrame';
import { getDemoUrl } from './config/runtime';

const deckConfig = {
  width: 1920,
  height: 1080,
  margin: 0.04,
  controls: true,
  progress: true,
  hash: true,
  center: true,
  transition: 'slide' as const,
  pdfSeparateFragments: false,
};

export default function App() {
  const demoUrl = getDemoUrl();

  return (
    <Deck plugins={[RevealNotes]} config={deckConfig}>
      <Slide>
        <div className="scaffold-slide">
          <p className="scaffold-slide__eyebrow">TICKET-ADV162</p>
          <h1>ReconX presentation scaffold</h1>
          <p>Reveal.js and the live-demo boundary are ready for slide authoring.</p>
          <LiveDemoFrame src={demoUrl} />
        </div>
        <aside className="notes">
          Setup-only slide. Replace it when presentation content is authored.
        </aside>
      </Slide>
    </Deck>
  );
}
