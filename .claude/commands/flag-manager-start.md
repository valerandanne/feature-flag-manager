Start the feature-flag-manager backend and frontend locally.

Run these steps in order:

## 1. Start the Kotlin/Spring Boot backend (in background)
```bash
cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'
```
Run this in the background. It applies Flyway schema + seed data automatically.
The backend will be available at http://localhost:8080.

## 2. Start the React/Vite frontend (in background)
```bash
cd frontend && npm install && npm run dev
```
Run this in the background. The frontend will be available at http://localhost:5173 (default Vite port).

## 3. Report status
Wait a few seconds, then check that both processes are up (e.g. `curl -sf http://localhost:8080/actuator/health` if available, or just confirm the log output shows the servers started). Report the URLs for backend and frontend. If either step fails, show the error and stop.
