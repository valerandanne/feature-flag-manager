import prisma from '../src/prismaClient'
import { v4 as uuidv4 } from 'uuid'

async function main() {
  // Environments
  const envNames = ['development', 'staging', 'production']
  for (const n of envNames) {
    await prisma.environment.upsert({ where: { name: n }, update: {}, create: { name: n } })
  }

  // Projects
  const payments = await prisma.project.upsert({
    where: { key: 'payments' },
    update: {},
    create: { key: 'payments', name: 'Payments Service' }
  })

  const web = await prisma.project.upsert({
    where: { key: 'web-frontend' },
    update: {},
    create: { key: 'web-frontend', name: 'Web Frontend' }
  })

  // Flags
  const newPayment = await prisma.featureFlag.upsert({
    where: { projectId_name: { projectId: payments.id, name: 'new-payment-flow' } },
    update: {},
    create: { projectId: payments.id, name: 'new-payment-flow', description: 'Toggle new checkout flow' }
  })

  const homepage = await prisma.featureFlag.upsert({
    where: { projectId_name: { projectId: web.id, name: 'homepage-variant' } },
    update: {},
    create: { projectId: web.id, name: 'homepage-variant', description: 'Homepage variant flag' }
  })

  // FlagEnv configs
  const prodEnv = await prisma.environment.findUnique({ where: { name: 'production' } })
  const stagingEnv = await prisma.environment.findUnique({ where: { name: 'staging' } })

  if (prodEnv) {
    await prisma.flagEnv.upsert({
      where: { flagId_envId: { flagId: newPayment.id, envId: prodEnv.id } },
      update: { enabled: false },
      create: { flagId: newPayment.id, envId: prodEnv.id, enabled: false, rollout: 100 }
    })
    await prisma.flagEnv.upsert({
      where: { flagId_envId: { flagId: homepage.id, envId: prodEnv.id } },
      update: { enabled: false },
      create: { flagId: homepage.id, envId: prodEnv.id, enabled: false, rollout: 30 }
    })
  }
  if (stagingEnv) {
    await prisma.flagEnv.upsert({
      where: { flagId_envId: { flagId: newPayment.id, envId: stagingEnv.id } },
      update: { enabled: true },
      create: { flagId: newPayment.id, envId: stagingEnv.id, enabled: true, rollout: 100 }
    })
    await prisma.flagEnv.upsert({
      where: { flagId_envId: { flagId: homepage.id, envId: stagingEnv.id } },
      update: { enabled: true },
      create: { flagId: homepage.id, envId: stagingEnv.id, enabled: true, rollout: 100 }
    })
  }

  // API Keys
  await prisma.apiKey.upsert({
    where: { key: 'payments-mvp-key' },
    update: {},
    create: { key: 'payments-mvp-key', projectId: payments.id, description: 'MVP key for payments' }
  })
  await prisma.apiKey.upsert({
    where: { key: 'search-mvp-key' },
    update: {},
    create: { key: 'search-mvp-key', projectId: web.id, description: 'MVP key for search/web' }
  })

  console.log('Seed complete')
}

main()
  .catch(e => {
    console.error(e)
    process.exit(1)
  })
  .finally(async () => {
    await prisma.$disconnect()
  })
