import {
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type ReactNode,
} from 'react';

const DEFAULT_MINIMUM_SCALE = 0.72;
const READABLE_SCALE_FLOOR = 0.6;
const SCALE_TOLERANCE = 0.001;

interface FitState {
  scale: number;
  requiredScale: number;
  overflowing: boolean;
}

export interface AutoFitProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  minimumScale?: number;
  align?: 'start' | 'center';
  debugName?: string;
  onOverflowChange?: (overflowing: boolean) => void;
}

function normalizeMinimumScale(minimumScale: number) {
  if (!Number.isFinite(minimumScale)) return DEFAULT_MINIMUM_SCALE;
  return Math.min(1, Math.max(READABLE_SCALE_FLOOR, minimumScale));
}

function formatScale(scale: number) {
  return Number(scale.toFixed(3)).toString();
}

export function AutoFit({
  children,
  minimumScale = DEFAULT_MINIMUM_SCALE,
  align = 'start',
  debugName = 'slide content',
  onOverflowChange,
  className,
  style,
  ...props
}: AutoFitProps) {
  const frameRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);
  const warnedRef = useRef(false);
  const normalizedMinimumScale = normalizeMinimumScale(minimumScale);
  const [fit, setFit] = useState<FitState>({
    scale: 1,
    requiredScale: 1,
    overflowing: false,
  });

  useLayoutEffect(() => {
    if (!frameRef.current || !contentRef.current) return undefined;
    const frame = frameRef.current;
    const content = contentRef.current;

    function measure() {
      const availableWidth = frame.clientWidth;
      const availableHeight = frame.clientHeight;
      if (availableWidth <= 0 || availableHeight <= 0) return;

      const naturalWidth = Math.max(content.scrollWidth, 1);
      const naturalHeight = Math.max(content.scrollHeight, 1);
      const requiredScale = Math.min(
        1,
        availableWidth / naturalWidth,
        availableHeight / naturalHeight,
      );
      const scale = Math.max(requiredScale, normalizedMinimumScale);
      const overflowing = requiredScale + SCALE_TOLERANCE < normalizedMinimumScale;

      setFit((current) => {
        if (
          Math.abs(current.scale - scale) < SCALE_TOLERANCE
          && Math.abs(current.requiredScale - requiredScale) < SCALE_TOLERANCE
          && current.overflowing === overflowing
        ) {
          return current;
        }
        return { scale, requiredScale, overflowing };
      });
    }

    measure();

    if (typeof ResizeObserver === 'undefined') {
      window.addEventListener('resize', measure);
      return () => window.removeEventListener('resize', measure);
    }

    const observer = new ResizeObserver(measure);
    observer.observe(frame);
    observer.observe(content);
    for (const child of content.children) observer.observe(child);
    return () => observer.disconnect();
  }, [normalizedMinimumScale]);

  useEffect(() => {
    onOverflowChange?.(fit.overflowing);
  }, [fit.overflowing, onOverflowChange]);

  useEffect(() => {
    if (!fit.overflowing) {
      warnedRef.current = false;
      return;
    }
    if (warnedRef.current || !import.meta.env.DEV) return;

    warnedRef.current = true;
    console.warn(
      `AutoFit "${debugName}" needs scale ${formatScale(fit.requiredScale)}, below the readable minimum ${formatScale(normalizedMinimumScale)}. Reduce or restructure the content.`,
    );
  }, [debugName, fit.overflowing, fit.requiredScale, normalizedMinimumScale]);

  const classes = [
    'ds-autofit',
    `ds-autofit--align-${align}`,
    className,
  ].filter(Boolean).join(' ');
  const fitStyle = {
    ...style,
    '--autofit-scale': formatScale(fit.scale),
  } as CSSProperties;

  return (
    <div
      ref={frameRef}
      className={classes}
      style={fitStyle}
      {...props}
      data-overflow={fit.overflowing}
      data-scale={formatScale(fit.scale)}
    >
      <div ref={contentRef} className="ds-autofit__content">
        {children}
      </div>
    </div>
  );
}
