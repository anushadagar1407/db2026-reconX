import { Deck } from '@revealjs/react';
import RevealNotes from 'reveal.js/plugin/notes';
import { getDemoUrl } from './config/runtime';
import {
  ArchitectureSlide,
  DeliverySlide,
  LearningsSlide,
  LiveDemoSlide,
  MonitoringSlide,
  ProblemSlide,
  QuestionsSlide,
  ReconciliationSlide,
  TechStackSlide,
  TitleSlide,
} from './slides';

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
      <TitleSlide />
      <ProblemSlide />
      <ArchitectureSlide />
      <TechStackSlide />
      <LiveDemoSlide demoUrl={demoUrl} />
      <ReconciliationSlide />
      <DeliverySlide />
      <MonitoringSlide />
      <LearningsSlide />
      <QuestionsSlide />
    </Deck>
  );
}
