const defaultBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export async function apiGet(path: string) {
  const res = await fetch(`${defaultBase}${path}`)
  if (!res.ok) throw new Error(await res.text())
  return res.json()
}

export async function apiPost(path: string, body: any) {
  const res = await fetch(`${defaultBase}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(await res.text())
  return res.json()
}

export async function apiPatch(path: string, body: any) {
  const res = await fetch(`${defaultBase}${path}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(await res.text())
  return res.json()
}
