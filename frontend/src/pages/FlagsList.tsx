import React, { useEffect, useMemo, useState } from 'react'
import { apiGet, apiPatch, apiPost } from '../api'

type FlagEnv = { enabled: boolean; version: number }
type FlagEnvSummary = FlagEnv & { env: string }
type FlagDto = { key: string; name: string; description?: string; envs: FlagEnvSummary[] }
type Flag = { key: string; name: string; description?: string; envs: Record<string, FlagEnv> }

function toFlag(dto: FlagDto): Flag {
  const envs: Record<string, FlagEnv> = {}
  for (const { env, ...rest } of dto.envs) envs[env] = rest
  return { key: dto.key, name: dto.name, description: dto.description, envs }
}

const ENVIRONMENTS = ['development', 'staging', 'production']

export default function FlagsList() {
  const [flags, setFlags] = useState<Flag[]>([])
  const [envName, setEnvName] = useState<string>('production')
  const [loading, setLoading] = useState(false)
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [newFlagKey, setNewFlagKey] = useState('')
  const [newFlagName, setNewFlagName] = useState('')
  const [newFlagDescription, setNewFlagDescription] = useState('')
  const [creating, setCreating] = useState(false)

  const [editingMetadata, setEditingMetadata] = useState<string | null>(null)
  const [nameDraft, setNameDraft] = useState('')
  const [descriptionDraft, setDescriptionDraft] = useState('')
  const [savingMetadata, setSavingMetadata] = useState(false)

  useEffect(() => {
    loadFlags()
  }, [])

  async function loadFlags() {
    setLoading(true)
    setError(null)
    try {
      const data: FlagDto[] = await apiGet('/api/v1/flags')
      setFlags(data.map(toFlag))
    } catch (err: any) {
      setError('Could not load flags: ' + (err.message || err))
    } finally {
      setLoading(false)
    }
  }

  function applyEnvUpdate(flagKey: string, updated: FlagEnv) {
    setFlags((prev) =>
      prev.map((f) => (f.key === flagKey ? { ...f, envs: { ...f.envs, [envName]: updated } } : f))
    )
  }

  async function toggleFlag(flagKey: string) {
    const cur = flags.find((f) => f.key === flagKey)?.envs[envName]
    const newEnabled = !(cur && cur.enabled)
    try {
      const body = { enabled: newEnabled }
      const updated = await apiPatch(`/api/v1/flags/${flagKey}/environments/${envName}`, body)
      applyEnvUpdate(flagKey, updated)
    } catch (err: any) {
      setError(`Could not update "${flagKey}": ` + (err.message || err))
    }
  }

  async function createFlag(e: React.FormEvent) {
    e.preventDefault()
    const key = newFlagKey.trim()
    const name = newFlagName.trim()
    if (!key || !name) return
    setCreating(true)
    setError(null)
    try {
      await apiPost('/api/v1/flags', {
        key,
        name,
        description: newFlagDescription.trim() || undefined,
      })
      setNewFlagKey('')
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

  function startEditMetadata(flag: Flag) {
    setEditingMetadata(flag.key)
    setNameDraft(flag.name)
    setDescriptionDraft(flag.description || '')
  }

  function cancelEditMetadata() {
    setEditingMetadata(null)
    setNameDraft('')
    setDescriptionDraft('')
  }

  async function saveMetadata(flagKey: string) {
    setSavingMetadata(true)
    setError(null)
    try {
      const updated = await apiPatch(`/api/v1/flags/${flagKey}`, {
        name: nameDraft.trim() || null,
        description: descriptionDraft.trim() || null,
      })
      setFlags((s) =>
        s.map((f) => (f.key === flagKey ? { ...f, name: updated.name, description: updated.description } : f))
      )
      setEditingMetadata(null)
      setNameDraft('')
      setDescriptionDraft('')
    } catch (err: any) {
      setError(`Could not update "${flagKey}": ` + (err.message || err))
    } finally {
      setSavingMetadata(false)
    }
  }

  const filteredFlags = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return flags
    return flags.filter(
      (f) =>
        f.name.toLowerCase().includes(q) ||
        f.key.toLowerCase().includes(q) ||
        (f.description || '').toLowerCase().includes(q)
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
            <label htmlFor="new-flag-key">Key</label>
            <input
              id="new-flag-key"
              className="text-input"
              placeholder="my-new-flag"
              value={newFlagKey}
              onChange={(e) => setNewFlagKey(e.target.value)}
              autoFocus
              required
            />
          </div>
          <div className="create-form-row">
            <label htmlFor="new-flag-name">Name</label>
            <input
              id="new-flag-name"
              className="text-input"
              placeholder="Checkout redesign"
              maxLength={25}
              value={newFlagName}
              onChange={(e) => setNewFlagName(e.target.value)}
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
            <button
              type="submit"
              className="primary-btn"
              disabled={creating || !newFlagKey.trim() || !newFlagName.trim()}
            >
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

          return (
            <div className="flag-card" key={f.key}>
              <div className="flag-card-row">
                <div className="flag-info">
                  <div className="flag-name-row">
                    {editingMetadata === f.key ? (
                      <input
                        className="text-input name-edit-input"
                        value={nameDraft}
                        onChange={(e) => setNameDraft(e.target.value)}
                        placeholder="Name"
                        maxLength={25}
                        autoFocus
                      />
                    ) : (
                      <span className="flag-name" onClick={() => startEditMetadata(f)}>
                        {f.name}
                      </span>
                    )}
                    <span className={`status-pill ${enabled ? 'on' : 'off'}`}>
                      {enabled ? 'Enabled' : 'Disabled'}
                    </span>
                  </div>
                  <p className="flag-key">{f.key}</p>
                  {editingMetadata === f.key ? (
                    <div className="desc-edit-row">
                      <input
                        className="text-input desc-edit-input"
                        value={descriptionDraft}
                        onChange={(e) => setDescriptionDraft(e.target.value)}
                        placeholder="Description"
                      />
                      <button
                        className="link-btn"
                        onClick={() => saveMetadata(f.key)}
                        disabled={savingMetadata}
                      >
                        Save
                      </button>
                      <button className="link-btn" onClick={cancelEditMetadata}>
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <p className="flag-desc" onClick={() => startEditMetadata(f)}>
                      {f.description || <span className="flag-desc-placeholder">Add description</span>}
                    </p>
                  )}
                  <p className="flag-meta">version {cfg ? cfg.version : 'n/a'}</p>
                </div>

                <div className="flag-actions">
                  <label className="switch">
                    <input type="checkbox" checked={enabled} onChange={() => toggleFlag(f.key)} />
                    <span className="switch-track" />
                  </label>
                </div>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
