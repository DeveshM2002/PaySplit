/**
 * main.jsx — The entry point of the React application.
 *
 * WHY StrictMode?
 * React.StrictMode doesn't render anything visible. It enables:
 * - Warnings about deprecated lifecycle methods
 * - Detecting side effects (renders twice in dev to catch issues)
 * - Warning about legacy string ref usage
 * It only runs in development — zero impact in production.
 */
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App.jsx';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>
);
