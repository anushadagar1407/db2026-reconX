import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import 'reveal.js/reveal.css';
import App from './App';
import './design-system/tokens.css';
import './design-system/system.css';
import './presentation.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
