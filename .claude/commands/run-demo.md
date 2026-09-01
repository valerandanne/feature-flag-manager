Run the feature-flag-manager SDK demo: it polls the `new-payment-flow` flag in `staging` and prints
its state whenever it changes, along with `curl` commands to flip the flag live.

Run these steps in order:

## 1. Start the backend with seeded demo data (in background, if not already running)
```bash
cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'
```
Run this in the background. The `dev` profile seeds demo flags (`new-payment-flow`,
`homepage-variant`) across `development`, `staging`, `production`. Wait until it logs that it
started on http://localhost:8080 before continuing.

## 2. Run the SDK demo (in background)
```bash
cd sdk && ./gradlew run
```
Run this in the background and stream its output back to the user — it prints the current flag
state on every poll plus ready-to-use `curl` commands to toggle `new-payment-flow` in `staging`
from another terminal.

## 3. Report status
Confirm both processes are up, show the backend URL (http://localhost:8080) and surface the SDK
demo's `curl` toggle commands to the user. If either step fails, show the error and stop.
