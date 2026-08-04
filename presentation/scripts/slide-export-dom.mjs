export function inspectSlideDom({ h, v }) {
  const slidesRoot = document.querySelector('.reveal .slides');
  if (!slidesRoot) throw new Error('Reveal slide container was not found.');
  const horizontal = [...slidesRoot.children]
    .filter((element) => element.tagName === 'SECTION')[h];
  const vertical = horizontal
    ? [...horizontal.children].filter((element) => element.tagName === 'SECTION')
    : [];
  const target = vertical.length > 0 ? vertical[v] : horizontal;
  if (!target) throw new Error(`Reveal slide ${h}/${v} is missing.`);

  const canvas = target.querySelector('.slide-canvas');
  const safeFrame = canvas?.querySelector('.slide-canvas__safe-area') ?? null;
  const safeContent = safeFrame?.querySelector('.slide-canvas__safe-content') ?? null;
  const tolerance = 1;
  const metrics = (element) => element ? {
    clientWidth: element.clientWidth,
    clientHeight: element.clientHeight,
    scrollWidth: element.scrollWidth,
    scrollHeight: element.scrollHeight,
    horizontal: element.scrollWidth > element.clientWidth + tolerance,
    vertical: element.scrollHeight > element.clientHeight + tolerance,
  } : null;
  const documentMetrics = metrics(document.documentElement);
  const slideMetrics = metrics(target);
  const canvasMetrics = metrics(canvas);
  const safeFrameMetrics = metrics(safeFrame);
  const safeAreaMetrics = safeContent ? {
    ...metrics(safeContent),
    inset: safeFrame.getAttribute('data-slide-inset') ?? 'unknown',
    enforcement: safeFrame.getAttribute('data-slide-inset') === 'edge'
      ? 'full-bleed'
      : 'safe-content',
  } : null;
  const outsideSafeArea = [];

  if (safeContent) {
    const safeRect = safeContent.getBoundingClientRect();
    for (const element of safeContent.querySelectorAll('*')) {
      const style = getComputedStyle(element);
      if (style.display === 'none' || style.visibility === 'hidden') continue;
      const rect = element.getBoundingClientRect();
      if (rect.width === 0 && rect.height === 0) continue;
      if (
        rect.left < safeRect.left - tolerance
        || rect.top < safeRect.top - tolerance
        || rect.right > safeRect.right + tolerance
        || rect.bottom > safeRect.bottom + tolerance
      ) {
        outsideSafeArea.push({
          tag: element.tagName.toLowerCase(),
          id: element.id || null,
          className: typeof element.className === 'string' ? element.className : null,
        });
        if (outsideSafeArea.length === 20) break;
      }
    }
  }

  const autoFit = [...target.querySelectorAll('.ds-autofit[data-overflow="true"]')]
    .map((element) => ({
      id: element.id || null,
      className: element.className,
      scale: element.getAttribute('data-scale'),
    }));
  const missingSlideCanvas = !canvas || !safeFrame || !safeContent;
  const detected = missingSlideCanvas
    || documentMetrics.horizontal
    || documentMetrics.vertical
    || slideMetrics.horizontal
    || slideMetrics.vertical
    || canvasMetrics.horizontal
    || canvasMetrics.vertical
    || safeAreaMetrics.horizontal
    || safeAreaMetrics.vertical
    || outsideSafeArea.length > 0
    || autoFit.length > 0;
  const heading = target.querySelector('h1, h2, h3');
  const title = target.getAttribute('aria-label')
    || target.getAttribute('data-title')
    || heading?.textContent?.trim()
    || `Slide ${h + 1}/${v + 1}`;

  return {
    title,
    overflow: {
      detected,
      missingSlideCanvas,
      document: documentMetrics,
      slide: slideMetrics,
      canvas: canvasMetrics,
      safeFrame: safeFrameMetrics,
      safeArea: safeAreaMetrics,
      outsideSafeArea,
      autoFit,
    },
  };
}
