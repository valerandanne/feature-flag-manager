import React, { useEffect, useMemo, useState } from 'react'
import { apiGet, apiPatch } from '../api'

type Flag = { id: string; name: string; description?: string }
type FlagEnv = { id: string; enabled: boolean; rollout: number; version: number }

const ENVIRONMENTS = ['development', 'staging', 'production']

export default function FlagsList() {
  const [flags, setFlags] = useState<Flag[]>([])
  const [envName, setEnvName] = useState<string>('production')
  const [loading, setLoading] = useState(false)
  const [flagEnvs, setFlagEnvs] = useState<Record<string, FlagEnv | null>>({})
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pendingRollout, setPendingRollout] = useState<Record<string, number>>({})

  useEffect(() => {
    loadFlags()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [envName])

  async function loadFlags() {
    setLoading(true)
    setError(null)
    try {
      const data = await apiGet('/api/v1/flags')
      setFlags(data)
      const cfgs: Record<string, FlagEnv | null> = {}
      await Promise.all(
        data.map(async (f: any) => {
          try {
            cfgs[f.name] = await apiGet(`/api/v1/flags/${f.name}/env/${envName}`)
          } catch (e) {
            cfgs[f.name] = null
          }
        })
      )
      setFlagEnvs(cfgs)
      setPendingRollout({})
    } catch (err: any) {
      setError('Could not load flags: ' + (err.message || err))
    } finally {
      setLoading(false)
    }
  }

  async function toggleFlag(flagName: string) {
    const cur = flagEnvs[flagName]
    const newEnabled = !(cur && cur.enabled)
    try {
      const body = { enabled: newEnabled, version: cur ? cur.version : 0 }
      const updated = await apiPatch(`/api/v1/flags/${flagName}/env/${envName}`, body)
      setFlagEnvs((s) => ({ ...s, [flagName]: updated }))
    } catch (err: any) {
      setError(`Could not update "${flagName}": ` + (err.message || err))
    }
  }

  async function commitRollout(flagName: string) {
    const cur = flagEnvs[flagName]
    const value = pendingRollout[flagName]
    if (!cur || value === undefined || value === cur.rollout) return
    try {
      const body = { rollout: value, version: cur.version }
      const updated = await apiPatch(`/api/v1/flags/${flagName}/env/${envName}`, body)
      setFlagEnvs((s) => ({ ...s, [flagName]: updated }))
    } catch (err: any) {
      setError(`Could not update rollout for "${flagName}": ` + (err.message || err))
    } finally {
      setPendingRollout((s) => {
        const next = { ...s }
        delete next[flagName]
        return next
      })
    }
  }

  const filteredFlags = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return flags
    return flags.filter(
      (f) => f.name.toLowerCase().includes(q) || (f.description || '').toLowerCase().includes(q)
    )
  }, [flags, query])

  const enabledCount = flags.filter((f) => flagEnvs[f.name]?.enabled).length

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Feature Flags</h2>
          <p>Toggle functionality per client and environment without a redeploy.</p>
        </div>
        {flags.length > 0 && (
          <span className="count-chip">
            {enabledCount} / {flags.length} enabled in {envName}
          </span>
        )}
      </div>

      {error && (
        <div className="toast">
          <span>{error}</span>
          <button onClick={() => setError(null)}>Dismiss</button>
        </div>
      )}

      <div className="toolbar">
        <div className="env-tabs">
          {ENVIRONMENTS.map((env) => (
            <button
              key={env}
              className={`env-tab ${env === envName ? 'active' : ''}`}
              onClick={() => setEnvName(env)}
            >
              {env}
            </button>
          ))}
        </div>

        <div className="search-wrap">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="7" />
            <path d="M21 21l-4.35-4.35" />
          </svg>
          <input
            className="search-input"
            placeholder="Search flags..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>

        <button className={`refresh-btn ${loading ? 'spinning' : ''}`} onClick={loadFlags} disabled={loading}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 12a9 9 0 1 1-2.64-6.36" />
            <path d="M21 3v6h-6" />
          </svg>
          Refresh
        </button>
      </div>

      {loading && flags.length === 0 && (
        <div>
          {[0, 1, 2].map((i) => (
            <div key={i} className="skeleton-card" />
          ))}
        </div>
      )}

      {!loading && filteredFlags.length === 0 && (
        <div className="empty-state">
          <div className="empty-state-icon">🚩</div>
          <h3>{flags.length === 0 ? 'No flags yet' : 'No flags match your search'}</h3>
          <p>
            {flags.length === 0
              ? 'Flags created via the API will show up here.'
              : 'Try a different search term.'}
          </p>
        </div>
      )}

      <div className="flag-list">
        {filteredFlags.map((f) => {
          const cfg = flagEnvs[f.name]
          const enabled = !!cfg?.enabled
          const rolloutValue = pendingRollout[f.name] ?? cfg?.rollout ?? 100

          return (
            <div className="flag-card" key={f.id}>
              <div className="flag-card-row">
                <div className="flag-info">
                  <div className="flag-name-row">
                    <span className="flag-name">{f.name}</span>
                    <span className={`status-pill ${enabled ? 'on' : 'off'}`}>
                      {enabled ? 'Enabled' : 'Disabled'}
                    </span>
                  </div>
                  {f.description && <p className="flag-desc">{f.description}</p>}
                  <p className="flag-meta">version {cfg ? cfg.version : 'n/a'}</p>
                </div>

                <div className="flag-actions">
                  <label className="switch">
                    <input type="checkbox" checked={enabled} onChange={() => toggleFlag(f.name)} />
                    <span className="switch-track" />
                  </label>
                </div>
              </div>

              {enabled && cfg && (
                <div className="rollout-row">
                  <label htmlFor={`rollout-${f.id}`}>Rollout</label>
                  <input
                    id={`rollout-${f.id}`}
                    type="range"
                    min={0}
                    max={100}
                    value={rolloutValue}
                    onChange={(e) =>
                      setPendingRollout((s) => ({ ...s, [f.name]: Number(e.target.value) }))
                    }
                    onMouseUp={() => commitRollout(f.name)}
                    onTouchEnd={() => commitRollout(f.name)}
                    onKeyUp={() => commitRollout(f.name)}
                  />
                  <span className="rollout-value">{rolloutValue}%</span>
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
