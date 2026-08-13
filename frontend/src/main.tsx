import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { SharedThreadView } from './components/SharedThreadView.tsx'

// A hand-rolled check, not a router: this app has exactly one route that
// exists outside the authenticated shell, so a routing dependency would be
// more than the app needs. See SharedThreadView for why it's a separate
// top-level render rather than something App branches into.
const sharedMatch = window.location.pathname.match(/^\/shared\/([^/]+)/);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {sharedMatch ? <SharedThreadView token={sharedMatch[1]} /> : <App />}
  </StrictMode>,
)
