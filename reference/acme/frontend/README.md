# Acme reference frontend

This is the customer-owned Acme Service & Repair frontend. It was copied from the SBP frontend baseline and is now an independent Next.js application. It does not import or depend on `frontend/sbp-ui` at build time or runtime.

## Run the browser proof

The complete reference application can run from the repository root:

```bash
docker compose -f reference/acme/deployment/compose.yaml up --build -d
```

Podman users can use the equivalent `podman compose` command. Open [http://localhost:3000](http://localhost:3000), enter the development/demo credentials from `reference/acme/deployment/.env` when prompted, create a customer, open its detail page, add a Contact, complete the separate service region and account notes, save, and reload the page to confirm persistence.

For faster frontend development, start only the backend and PostgreSQL containers, then run this frontend in a separate terminal:

```bash
cd reference/acme/frontend
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## API configuration

The browser calls same-origin `/api/...` paths. Next.js proxies those requests to the backend URL configured through the server-only `SBP_API_URL` environment variable:

```bash
SBP_API_URL=http://localhost:8080 npm run dev
```

The default is already `http://localhost:8080`. This convention avoids CORS configuration during local development and can later target the backend service name in a container environment.

## Verification

```bash
npm run typecheck
npm run lint
npm run build
```

The generic Customer and Contact UI was copied from the SBP baseline. Acme-specific code is limited to branding in `lib/branding.ts`, the Acme API contract in `lib/acme-api.ts`, and the separate service profile form in `components/acme-profile-form.tsx`.
