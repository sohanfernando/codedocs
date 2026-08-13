import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import * as Sentry from '@sentry/react'
import './index.css'
import App from './App.tsx'
import { SharedThreadView } from './components/SharedThreadView.tsx'
import { ErrorFallback } from './components/ErrorFallback.tsx'

// An empty dsn (the default outside production) leaves the SDK disabled —
// capture calls become no-ops instead of throwing, same as the backend.
Sentry.init({
  dsn: import.meta.env.VITE_SENTRY_DSN,
  environment: import.meta.env.MODE,
  tracesSampleRate: 0.1,
  // This app handles user emails and repo contents — neither belongs in an
  // error tracker by default.
  sendDefaultPii: false,
})

// A hand-rolled check, not a router: this app has exactly one route that
// exists outside the authenticated shell, so a routing dependency would be
// more than the app needs. See SharedThreadView for why it's a separate
// top-level render rather than something App branches into.
const sharedMatch = window.location.pathname.match(/^\/shared\/([^/]+)/);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Sentry.ErrorBoundary fallback={ErrorFallback}>
      {sharedMatch ? <SharedThreadView token={sharedMatch[1]} /> : <App />}
    </Sentry.ErrorBoundary>
  </StrictMode>,
)
