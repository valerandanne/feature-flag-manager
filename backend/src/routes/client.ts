import express from 'express'
import prisma from '../prismaClient'
import { apiKeyMiddleware, ApiRequest } from '../middleware/apiKeyAuth'

const router = express.Router()

// GET /api/v1/client/features?env=production
router.get('/api/v1/client/features', apiKeyMiddleware, async (req: ApiRequest, res) => {
  const envName = (req.query.env as string) || 'production'
  if (!req.projectId) return res.status(401).json({ error: 'no project for key' })
  const projectId = req.projectId
  try {
    const env = await prisma.environment.findUnique({ where: { name: envName } })
    if (!env) return res.status(400).json({ error: 'invalid env' })
    // find flags for project that are not archived
    const flags = await prisma.featureFlag.findMany({ where: { projectId, archived: false } })
    const features = [] as any[]
    for (const f of flags) {
      const cfg = await prisma.flagEnv.findUnique({ where: { flagId_envId: { flagId: f.id, envId: env.id } } })
      features.push({ name: f.name, enabled: cfg ? cfg.enabled : false })
    }
    return res.json({ project: projectId, env: envName, features })
  } catch (err: any) {
    return res.status(500).json({ error: err.message })
  }
})

export default router
