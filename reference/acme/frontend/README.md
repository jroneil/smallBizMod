# Acme reference frontend

This is the customer-owned Acme Service & Repair frontend. It was copied from the SBP frontend baseline and is now an independent Next.js application. It does not import or depend on `frontend/sbp-ui` at build time or runtime.

## Run the browser proof

First start the existing backend and PostgreSQL environment from the repository root:

```bash
docker compose -f reference/acme/deployment/compose.yaml up --build -d
```

Podman users can use the equivalent `podman compose` command. Then start this frontend in a separate terminal:

```bash
cd reference/acme/frontend
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). Create a customer, open its detail page, complete the service region and account notes, save, and reload the page to confirm persistence.

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

The Acme-specific code is currently limited to branding in `lib/branding.ts`, the Acme API contract in `lib/acme-api.ts`, and the service profile form in `components/acme-profile-form.tsx`.
