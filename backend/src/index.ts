import express from 'express'
import bodyParser from 'body-parser'
import cors from 'cors'
import adminRoutes from './routes/admin'
import clientRoutes from './routes/client'
import prisma from './prismaClient'

const app = express()
app.use(cors())
app.use(bodyParser.json())

app.use(adminRoutes)
app.use(clientRoutes)

app.get('/health', (_req, res) => res.json({ ok: true }))

const port = process.env.PORT || 4000

// export app for testing
export default app

// start server when run directly
if (require.main === module) {
  app.listen(port, async () => {
    console.log('Server running on port', port)
    // warm prisma
    await prisma.$connect()
  })
}
