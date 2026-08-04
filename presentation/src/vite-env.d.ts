/// <reference types="vite/client" />

interface ReconXPresentationConfig {
  demoUrl?: string | null;
}

interface Window {
  __RECONX_PRESENTATION_CONFIG__?: ReconXPresentationConfig;
}
