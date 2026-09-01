import { Request, Response, NextFunction } from 'express'
import prisma from '../prismaClient'

export interface ApiRequest extends Request {
  projectId?: string
  // allow access to query params in a simple way for tests and handlers
  query?: any
  get?: (name: string) => string | undefined
  headers?: any
}

export async function apiKeyMiddleware(req: ApiRequest, res: Response, next: NextFunction) {
  const auth = (req.get && req.get('authorization')) || (req.headers && req.headers['authorization']) as string | undefined
  if (!auth || !auth.startsWith('Bearer ')) return res.status(401).json({ error: 'Missing api key' })
  const key = auth.slice('Bearer '.length)
  const apiKey = await prisma.apiKey.findUnique({ where: { key }, include: { project: true } })
  if (!apiKey) return res.status(401).json({ error: 'Invalid api key' })
  req.projectId = apiKey.projectId
  next()
}
