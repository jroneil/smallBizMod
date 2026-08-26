# Acme reference end-to-end browser check

A small headless-Chrome check that exercises the Acme demo flow against a running stack:

```text
visit / (redirects to /signin)
→ sign in with demo credentials
→ create a customer
→ add a contact
→ save the service profile
→ reload and confirm contact + profile persisted
→ sign out (returns to /signin)
```

## Prerequisites

- The full stack is running (`docker compose -f reference/acme/deployment/compose.yaml up --build -d`).
- A local Chrome/Chromium is installed at `/usr/bin/google-chrome` (adjust `executablePath` in `e2e.mjs` if yours differs).

## Run

```bash
cd reference/acme/e2e
npm install
npm test
```

The check uses `puppeteer-core` (no bundled browser) and talks to `http://localhost:3000`. It creates throwaway demo data in the running database, so point it at a disposable local environment.
