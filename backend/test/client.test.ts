import request from 'supertest'
import app from '../src/index'
import prisma from '../src/prismaClient'

// This test verifies that the client API endpoint returns the feature
// flags for a given project and environment when a valid API key is provided.
// It seeds minimal data (environment, project, feature flag, flag-environment
// state, and API key), enables the flag in the production environment, then
// calls the client features endpoint and asserts the response structure and
// that the seeded flag is present in the returned features list.

describe('Client API', () => {
  beforeAll(async () => {
    // seed minimal data required for the test
    await prisma.environment.upsert({ where: { name: 'production' }, update: {}, create: { name: 'production' } })
    const project = await prisma.project.upsert({ where: { key: 'payments' }, update: {}, create: { key: 'payments', name: 'Payments' } })
    const flag = await prisma.featureFlag.upsert({
      where: { projectId_name: { projectId: project.id, name: 'new-payment-flow' } },
      update: {},
      create: { projectId: project.id, name: 'new-payment-flow', description: 'Test flag' }
    })
    const prodEnv = await prisma.environment.findUnique({ where: { name: 'production' } })
    if (prodEnv) {
      await prisma.flagEnv.upsert({
        where: { flagId_envId: { flagId: flag.id, envId: prodEnv.id } },
        update: { enabled: true },
        create: { flagId: flag.id, envId: prodEnv.id, enabled: true, rollout: 100 }
      })
    }
    await prisma.apiKey.upsert({ where: { key: 'payments-mvp-key' }, update: {}, create: { key: 'payments-mvp-key', projectId: project.id, description: 'test key' } })
  })

  afterAll(async () => {
    await prisma.$disconnect()
  })

  it('returns features for project based on API key', async () => {
    const res = await request(app)
      .get('/api/v1/client/features?env=production')
      .set('Authorization', 'Bearer payments-mvp-key')
      .expect(200)

    expect(res.body).toHaveProperty('project')
    expect(res.body).toHaveProperty('env', 'production')
    expect(Array.isArray(res.body.features)).toBe(true)
    // ensure our seeded flag is present
    expect(res.body.features.find((f: any) => f.name === 'new-payment-flow')).toBeTruthy()
  })
})
