# Feature Flag Backend (MVP)

Quick start for local development.

Prerequisites: Node.js, npm

Install deps and generate Prisma client
```bash
cd backend
npm install
npx prisma generate
```

Run migrations and seed the database
```bash
npx prisma migrate dev --name init
npx ts-node --transpile-only prisma/seed.ts
```

Start dev server
```bash
npm run dev
```

Client API example (use seeded key `payments-mvp-key`)
```bash
curl -H "Authorization: Bearer payments-mvp-key" "http://localhost:4000/api/v1/client/features?env=production"
```

Run tests
```bash
npm test
```
