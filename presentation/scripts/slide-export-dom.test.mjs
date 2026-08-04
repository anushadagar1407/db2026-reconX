import { afterEach, describe, expect, it } from 'vitest';
import { inspectSlideDom } from './slide-export-dom.mjs';
import { exportExitCode } from './slide-export-lib.mjs';

function setMetrics(element, width, height) {
  for (const [property, value] of Object.entries({
    clientWidth: width,
    clientHeight: height,
    scrollWidth: width,
    scrollHeight: height,
  })) {
    Object.defineProperty(element, property, { configurable: true, value });
  }
}

function setRect(element, { left, top, width, height }) {
  element.getBoundingClientRect = () => ({
    left,
    top,
    right: left + width,
    bottom: top + height,
    width,
    height,
    x: left,
    y: top,
    toJSON: () => ({}),
  });
}

function mountSlide(inset) {
  document.body.innerHTML = `
    <div class="reveal">
      <div class="slides">
        <section>
          <div class="slide-canvas">
            <div class="slide-canvas__safe-area" data-slide-inset="${inset}">
              <div class="slide-canvas__safe-content">
                <h1>Boundary test</h1>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  `;
  const slide = document.querySelector('section');
  const canvas = document.querySelector('.slide-canvas');
  const safeFrame = document.querySelector('.slide-canvas__safe-area');
  const safeContent = document.querySelector('.slide-canvas__safe-content');
  const heading = document.querySelector('h1');
  setMetrics(document.documentElement, 1_920, 1_080);
  setMetrics(slide, 1_920, 1_080);
  setMetrics(canvas, 1_920, 1_080);
  setMetrics(safeFrame, 1_920, 1_080);
  return { slide, canvas, safeFrame, safeContent, heading };
}

afterEach(() => {
  document.body.innerHTML = '';
});

describe('slide safe-content diagnostics', () => {
  it('reports content that intrudes only into the padded safe margin', () => {
    const { safeFrame, safeContent, heading } = mountSlide('safe');
    setMetrics(safeContent, 1_696, 904);
    setRect(safeFrame, { left: 0, top: 0, width: 1_920, height: 1_080 });
    setRect(safeContent, { left: 112, top: 88, width: 1_696, height: 904 });
    setRect(heading, { left: 100, top: 100, width: 500, height: 80 });

    const diagnostics = inspectSlideDom({ h: 0, v: 0 });

    expect(diagnostics.overflow.safeArea).toMatchObject({
      clientWidth: 1_696,
      clientHeight: 904,
      inset: 'safe',
      enforcement: 'safe-content',
    });
    expect(diagnostics.overflow.outsideSafeArea).toEqual([
      expect.objectContaining({ tag: 'h1' }),
    ]);
    expect(diagnostics.overflow.detected).toBe(true);
    expect(exportExitCode([{ diagnostics }])).toBe(1);
  });

  it('records edge inset as an intentional full-bleed content box', () => {
    const { safeFrame, safeContent, heading } = mountSlide('edge');
    setMetrics(safeContent, 1_920, 1_080);
    setRect(safeFrame, { left: 0, top: 0, width: 1_920, height: 1_080 });
    setRect(safeContent, { left: 0, top: 0, width: 1_920, height: 1_080 });
    setRect(heading, { left: 0, top: 0, width: 500, height: 80 });

    const diagnostics = inspectSlideDom({ h: 0, v: 0 });

    expect(diagnostics.overflow.safeArea).toMatchObject({
      clientWidth: 1_920,
      clientHeight: 1_080,
      inset: 'edge',
      enforcement: 'full-bleed',
    });
    expect(diagnostics.overflow.outsideSafeArea).toEqual([]);
    expect(diagnostics.overflow.detected).toBe(false);
  });
});
