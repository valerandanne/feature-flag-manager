import React, { useEffect, useMemo, useState } from 'react'
import { apiGet, apiPatch, apiPost } from '../api'

type FlagEnv = { enabled: boolean; rollout: number; version: number }
type FlagEnvSummary = FlagEnv & { env: string }
type FlagDto = { name: string; description?: string; envs: FlagEnvSummary[] }
type Flag = { name: string; description?: string; envs: Record<string, FlagEnv> }

function toFlag(dto: FlagDto): Flag {
  const envs: Record<string, FlagEnv> = {}
  for (const { env, ...rest } of dto.envs) envs[env] = rest
  return { name: dto.name, description: dto.description, envs }
}

const ENVIRONMENTS = ['development', 'staging', 'production']

export default function FlagsList() {
  const [flags, setFlags] = useState<Flag[]>([])
  const [envName, setEnvName] = useState<string>('production')
  const [loading, setLoading] = useState(false)
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pendingRollout, setPendingRollout] = useState<Record<string, number>>({})

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [newFlagName, setNewFlagName] = useState('')
  const [newFlagDescription, setNewFlagDescription] = useState('')
  const [creating, setCreating] = useState(false)

  const [editingDescription, setEditingDescription] = useState<string | null>(null)
  const [descriptionDraft, setDescriptionDraft] = useState('')
  const [savingDescription, setSavingDescription] = useState(false)

  useEffect(() => {
    loadFlags()
  }, [])

  async function loadFlags() {
    setLoading(true)
    setError(null)
    try {
      const data: FlagDto[] = await apiGet('/api/v1/flags')
      setFlags(data.map(toFlag))
      setPendingRollout({})
    } catch (err: any) {
      setError('Could not load flags: ' + (err.message || err))
    } finally {
      setLoading(false)
    }
  }

  function applyEnvUpdate(flagName: string, updated: FlagEnv) {
    setFlags((prev) =>
      prev.map((f) => (f.name === flagName ? { ...f, envs: { ...f.envs, [envName]: updated } } : f))
    )
  }

  async function toggleFlag(flagName: string) {
    const cur = flags.find((f) => f.name === flagName)?.envs[envName]
    const newEnabled = !(cur && cur.enabled)
    try {
      const body = { enabled: newEnabled, version: cur ? cur.version : 0 }
      const updated = await apiPatch(`/api/v1/flags/${flagName}/envs/${envName}`, body)
      applyEnvUpdate(flagName, updated)
    } catch (err: any) {
      setError(`Could not update "${flagName}": ` + (err.message || err))
    }
  }

  async function commitRollout(flagName: string) {
    const cur = flags.find((f) => f.name === flagName)?.envs[envName]
    const value = pendingRollout[flagName]
    if (!cur || value === undefined || value === cur.rollout) return
    try {
      const body = { rollout: value, version: cur.version }
      const updated = await apiPatch(`/api/v1/flags/${flagName}/envs/${envName}`, body)
      applyEnvUpdate(flagName, updated)
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

  async function createFlag(e: React.FormEvent) {
    e.preventDefault()
    const name = newFlagName.trim()
    if (!name) return
    setCreating(true)
    setError(null)
    try {
      await apiPost('/api/v1/flags', {
        name,
        description: newFlagDescription.trim() || undefined,
      })
      setNewFlagName('')
      setNewFlagDescription('')
      setShowCreateForm(false)
      await loadFlags()
    } catch (err: any) {
      setError('Could not create flag: ' + (err.message || err))
    } finally {
      setCreating(false)
    }
  }

  function startEditDescription(flag: Flag) {
    setEditingDescription(flag.name)
    setDescriptionDraft(flag.description || '')
  }

  function cancelEditDescription() {
    setEditingDescription(null)
    setDescriptionDraft('')
  }

  async function saveDescription(flagName: string) {
    setSavingDescription(true)
    setError(null)
    try {
      const updated = await apiPatch(`/api/v1/flags/${flagName}`, {
        description: descriptionDraft.trim() || null,
      })
      setFlags((s) => s.map((f) => (f.name === flagName ? { ...f, description: updated.description } : f)))
      setEditingDescription(null)
      setDescriptionDraft('')
    } catch (err: any) {
      setError(`Could not update description for "${flagName}": ` + (err.message || err))
    } finally {
      setSavingDescription(false)
    }
  }

  const filteredFlags = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return flags
    return flags.filter(
      (f) => f.name.toLowerCase().includes(q) || (f.description || '').toLowerCase().includes(q)
    )
  }, [flags, query])

  const enabledCount = flags.filter((f) => f.envs[envName]?.enabled).length

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Feature Flags</h2>
          <p>Toggle functionality per client and environment without a redeploy.</p>
        </div>
        <div className="page-header-actions">
          {flags.length > 0 && (
            <span className="count-chip">
              {enabledCount} / {flags.length} enabled in {envName}
            </span>
          )}
          <button className="primary-btn" onClick={() => setShowCreateForm((s) => !s)}>
            {showCreateForm ? 'Cancel' : '+ New flag'}
          </button>
        </div>
      </div>

      {error && (
        <div className="toast">
          <span>{error}</span>
          <button onClick={() => setError(null)}>Dismiss</button>
        </div>
      )}

      {showCreateForm && (
        <form className="create-form" onSubmit={createFlag}>
          <div className="create-form-row">
            <label htmlFor="new-flag-name">Name</label>
            <input
              id="new-flag-name"
              className="text-input"
              placeholder="my-new-flag"
              value={newFlagName}
              onChange={(e) => setNewFlagName(e.target.value)}
              autoFocus
              required
            />
          </div>
          <div className="create-form-row">
            <label htmlFor="new-flag-description">Description</label>
            <input
              id="new-flag-description"
              className="text-input"
              placeholder="What does this flag control? (optional)"
              value={newFlagDescription}
              onChange={(e) => setNewFlagDescription(e.target.value)}
            />
          </div>
          <div className="create-form-actions">
            <button type="submit" className="primary-btn" disabled={creating || !newFlagName.trim()}>
              {creating ? 'Creating...' : 'Create flag'}
            </button>
          </div>
        </form>
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
          const cfg = f.envs[envName]
          const enabled = !!cfg?.enabled
          const rolloutValue = pendingRollout[f.name] ?? cfg?.rollout ?? 100

          return (
            <div className="flag-card" key={f.name}>
              <div className="flag-card-row">
                <div className="flag-info">
                  <div className="flag-name-row">
                    <span className="flag-name">{f.name}</span>
                    <span className={`status-pill ${enabled ? 'on' : 'off'}`}>
                      {enabled ? 'Enabled' : 'Disabled'}
                    </span>
                  </div>
                  {editingDescription === f.name ? (
                    <div className="desc-edit-row">
                      <input
                        className="text-input desc-edit-input"
                        value={descriptionDraft}
                        onChange={(e) => setDescriptionDraft(e.target.value)}
                        placeholder="Description"
                        autoFocus
                      />
                      <button
                        className="link-btn"
                        onClick={() => saveDescription(f.name)}
                        disabled={savingDescription}
                      >
                        Save
                      </button>
                      <button className="link-btn" onClick={cancelEditDescription}>
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <p className="flag-desc" onClick={() => startEditDescription(f)}>
                      {f.description || <span className="flag-desc-placeholder">Add description</span>}
                    </p>
                  )}
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
                  <label htmlFor={`rollout-${f.name}`}>Rollout</label>
                  <input
                    id={`rollout-${f.name}`}
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
