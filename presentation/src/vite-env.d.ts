/// <reference types="vite/client" />

interface ReconXPresentationConfig {
  demoUrl?: string;
}

interface Window {
  __RECONX_PRESENTATION_CONFIG__?: ReconXPresentationConfig;
}
