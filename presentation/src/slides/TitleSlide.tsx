import { useState } from 'react';
import { Slide } from '@revealjs/react';
import {
  BodyText,
  BrandMark,
  Cluster,
  SectionHeading,
  SlideCanvas,
  SlideLabel,
  SlideTitle,
  Stack,
} from '../design-system';
import { contributors } from './contributors';

function contributorInitials(displayName: string, login: string) {
  const initials = displayName
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2);

  return (initials || login.slice(0, 2)).toUpperCase();
}

function ContributorPortrait({ displayName, login, avatarSrc }: (typeof contributors)[number]) {
  const [imageFailed, setImageFailed] = useState(false);
  const altText = `Portrait of ${displayName}; GitHub login @${login}`;

  return (
    <div className="title-slide__contributor">
      <div className="title-slide__portrait-frame">
        {imageFailed ? (
          <div className="title-slide__portrait-fallback" role="img" aria-label={altText}>
            {contributorInitials(displayName, login)}
          </div>
        ) : (
          <img
            className="title-slide__portrait"
            src={avatarSrc}
            alt={altText}
            width="420"
            height="420"
            loading="eager"
            decoding="async"
            onError={() => setImageFailed(true)}
          />
        )}
      </div>
      <strong className="title-slide__contributor-name">{displayName}</strong>
      <span className="title-slide__contributor-login">@{login}</span>
    </div>
  );
}

export function TitleSlide() {
  return (
    <Slide aria-label="Slide 1: ReconX title">
      <SlideCanvas background="ink" className="title-slide-canvas">
        <Stack className="title-slide" justify="between" gap="xl">
          <Cluster className="title-slide__header" justify="between" align="start" wrap={false}>
            <span className="title-slide__brand-lockup">
              <BrandMark size="md" />
            </span>
            <SlideLabel>DAY 10 · 20-MINUTE DEMO</SlideLabel>
          </Cluster>

          <div className="title-slide__middle">
            <Stack className="title-slide__content" gap="lg">
              <div className="title-slide__rule" aria-hidden="true" />
              <SlideTitle>ReconX — Enterprise Trade Reconciliation Platform</SlideTitle>
              <BodyText variant="lede" className="title-slide__context">
                Deutsche Bank TDI 2026 training case study
              </BodyText>
            </Stack>

            <div className="title-slide__contributors" role="group" aria-labelledby="contributors-heading">
              <Cluster className="title-slide__contributors-header" justify="between" align="end" wrap={false}>
                <SectionHeading id="contributors-heading" className="title-slide__contributors-heading">
                  Contributors
                </SectionHeading>
                <span className="title-slide__contributors-meta">PUBLIC GITHUB RECORD · {contributors.length}</span>
              </Cluster>
              <div className="title-slide__portrait-ribbon">
                <div className="title-slide__roster-label">
                  <span>Verified roster</span>
                  <small>Names shown as published</small>
                </div>
                {contributors.map((contributor) => (
                  <ContributorPortrait key={contributor.login} {...contributor} />
                ))}
              </div>
            </div>
          </div>

          <Cluster className="title-slide__footer" justify="between" align="end" wrap={false}>
            <BodyText>Evidence boundary first. Demonstration detail second.</BodyText>
            <SlideLabel>TRAINING CASE STUDY · NOT PRODUCTION DEPLOYMENT</SlideLabel>
          </Cluster>
        </Stack>
      </SlideCanvas>
      <aside className="notes">
        Evidence status: implemented for the product title, training context, local brand artwork, presentation
        framework, and the verified public contributor snapshot. The roster uses public display names with an
        exact-login fallback and local avatar assets; it does not imply employment, ownership, or production use.
        Refresh if product framing, branding, slide count, or the public contributor set changes.
      </aside>
    </Slide>
  );
}

export default TitleSlide;
