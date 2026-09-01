import React from 'react'
import FlagsList from './pages/FlagsList'

export default function App() {
  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-inner">
          <div className="brand">
            <div className="brand-mark">⚑</div>
            <div className="brand-text">
              <h1>Feature Flags</h1>
              <p>Admin panel</p>
            </div>
          </div>
          <span className="topbar-badge">MVP</span>
        </div>
      </header>
      <main>
        <FlagsList />
      </main>
    </div>
  )
}
