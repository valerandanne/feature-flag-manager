import express from 'express'
import prisma from '../prismaClient'

const router = express.Router()

// POST /api/v1/projects
router.post('/api/v1/projects', async (req, res) => {
  const { key, name } = req.body
  if (!key || !name) return res.status(400).json({ error: 'key and name required' })
  try {
    const project = await prisma.project.create({ data: { key, name } })
    res.status(201).json(project)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// GET /api/v1/projects
router.get('/api/v1/projects', async (req, res) => {
  const projects = await prisma.project.findMany()
  res.json(projects)
})

// POST /api/v1/projects/:projectId/flags
router.post('/api/v1/projects/:projectId/flags', async (req, res) => {
  const { projectId } = req.params
  const { name, description } = req.body
  if (!name) return res.status(400).json({ error: 'name required' })
  try {
    const flag = await prisma.featureFlag.create({ data: { projectId, name, description } })
    res.status(201).json(flag)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// GET /api/v1/projects/:projectId/flags
router.get('/api/v1/projects/:projectId/flags', async (req, res) => {
  const { projectId } = req.params
  const flags = await prisma.featureFlag.findMany({ where: { projectId, archived: false } })
  res.json(flags)
})

// PATCH /api/v1/projects/:projectId/flags/:flagName
router.patch('/api/v1/projects/:projectId/flags/:flagName', async (req, res) => {
  const { projectId, flagName } = req.params
  const data: any = {}
  if (req.body.description !== undefined) data.description = req.body.description
  if (req.body.archived !== undefined) data.archived = req.body.archived
  try {
    const flag = await prisma.featureFlag.updateMany({
      where: { projectId, name: flagName },
      data,
    })
    if (flag.count === 0) return res.status(404).json({ error: 'flag not found' })
    const updated = await prisma.featureFlag.findFirst({ where: { projectId, name: flagName } })
    res.json(updated)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// DELETE /api/v1/projects/:projectId/flags/:flagName -> soft delete
router.delete('/api/v1/projects/:projectId/flags/:flagName', async (req, res) => {
  const { projectId, flagName } = req.params
  try {
    const updated = await prisma.featureFlag.updateMany({ where: { projectId, name: flagName }, data: { archived: true } })
    if (updated.count === 0) return res.status(404).json({ error: 'flag not found' })
    res.status(204).send()
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// PATCH per-env config
// PATCH /api/v1/projects/:projectId/flags/:flagName/env/:envName
router.patch('/api/v1/projects/:projectId/flags/:flagName/env/:envName', async (req, res) => {
  const { projectId, flagName, envName } = req.params
  const { enabled, rollout, version } = req.body
  if (version === undefined) return res.status(400).json({ error: 'version required for optimistic locking' })
  try {
    const flag = await prisma.featureFlag.findFirst({ where: { projectId, name: flagName } })
    if (!flag) return res.status(404).json({ error: 'flag not found' })
    const env = await prisma.environment.findUnique({ where: { name: envName } })
    if (!env) return res.status(404).json({ error: 'env not found' })

    const where = { flagId: flag.id, envId: env.id, version }
    const data: any = {}
    if (enabled !== undefined) data.enabled = enabled
    if (rollout !== undefined) data.rollout = rollout
    // increment version
    try {
      const updated = await prisma.flagEnv.updateMany({ where, data: { ...data, version: { increment: 1 } } as any })
      if (updated.count === 0) {
        const current = await prisma.flagEnv.findUnique({ where: { flagId_envId: { flagId: flag.id, envId: env.id } } })
        return res.status(409).json({ error: 'version conflict', current })
      }
      const newRow = await prisma.flagEnv.findUnique({ where: { flagId_envId: { flagId: flag.id, envId: env.id } } })
      return res.json(newRow)
    } catch (err: any) {
      return res.status(500).json({ error: err.message })
    }
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

export default router

