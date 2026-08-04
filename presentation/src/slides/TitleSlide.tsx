import { Slide } from '@revealjs/react';
import {
  BodyText,
  BrandMark,
  Cluster,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
} from '../design-system';

export function TitleSlide() {
  return (
    <Slide aria-label="Slide 1: ReconX title">
      <SlideCanvas background="ink" className="title-slide-canvas">
        <Stack className="title-slide" justify="between" gap="xl">
          <Cluster justify="between" align="start" wrap={false}>
            <span className="title-slide__brand-lockup">
              <BrandMark size="lg" />
            </span>
            <SlideLabel>DAY 10 · 20-MINUTE DEMO</SlideLabel>
          </Cluster>

          <Stack className="title-slide__content" gap="lg">
            <div className="title-slide__rule" aria-hidden="true" />
            <SlideTitle>ReconX — Enterprise Trade Reconciliation Platform</SlideTitle>
            <BodyText variant="lede" className="title-slide__context">
              Deutsche Bank TDI 2026 training case study
            </BodyText>
          </Stack>

          <Cluster className="title-slide__footer" justify="between" align="end" wrap={false}>
            <BodyText>Evidence boundary first. Demonstration detail second.</BodyText>
            <SlideLabel>TRAINING CASE STUDY · NOT PRODUCTION DEPLOYMENT</SlideLabel>
          </Cluster>
        </Stack>
      </SlideCanvas>
      <aside className="notes">
        Evidence status: implemented for the product title, training context, local brand artwork, and
        presentation framework; gap for the final ten-slide deck; requires human input for roster and
        speaking lead. Refresh if product framing, branding, slide count, or presenter ownership changes.
      </aside>
    </Slide>
  );
}

export default TitleSlide;
