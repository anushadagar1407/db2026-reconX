export type BrandMarkVariant = 'lockup' | 'symbol';
export type BrandMarkSize = 'sm' | 'md' | 'lg';

export interface BrandMarkProps {
  variant?: BrandMarkVariant;
  size?: BrandMarkSize;
  decorative?: boolean;
}

const assetByVariant: Record<BrandMarkVariant, string> = {
  lockup: `${import.meta.env.BASE_URL}assets/deutsche-bank-logo.svg`,
  symbol: `${import.meta.env.BASE_URL}assets/deutsche-bank-mark.svg`,
};

// Official Deutsche Bank AG artwork mirrored from Wikimedia Commons source files:
// https://commons.wikimedia.org/wiki/File:Deutsche_Bank_logo.svg
// https://commons.wikimedia.org/wiki/File:Deutsche_Bank_logo_without_wordmark.svg
// The artwork is public domain for copyright purposes and remains trademarked.
export function BrandMark({
  variant = 'lockup',
  size = 'md',
  decorative = false,
}: BrandMarkProps) {
  return (
    <img
      className={`brand-mark brand-mark--${variant} brand-mark--${size}`}
      src={assetByVariant[variant]}
      alt={decorative ? '' : 'Deutsche Bank'}
      aria-hidden={decorative ? true : undefined}
      draggable="false"
    />
  );
}
