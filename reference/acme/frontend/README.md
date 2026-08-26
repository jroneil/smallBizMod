# Acme reference frontend

This is the customer-owned Acme Service & Repair frontend. It was copied from the SBP frontend baseline and is now an independent Next.js application. It does not import or depend on `frontend/sbp-ui` at build time or runtime.

## Run the browser proof

Run the full stack from the repository root:

```bash
docker compose -f reference/acme/deployment/compose.yaml up --build -d
```

Podman users can use the equivalent `podman compose` command. Open [http://localhost:3000](http://localhost:3000) and sign in with the demo credentials configured in `.env` (defaults `demo` / `demo-local-development`). Create a customer, open its detail page, add a contact, complete the service region and account notes, save, and reload the page to confirm persistence.

For frontend-only development against a running backend, start this frontend separately:

```bash
cd reference/acme/frontend
npm install
npm run dev
```

## API configuration and sign-in

The browser calls same-origin `/api/...` paths. Next.js proxies those requests to the backend URL configured through the server-only `SBP_API_URL` environment variable:

```bash
SBP_API_URL=http://localhost:8080 npm run dev
```

The default is already `http://localhost:8080`. This convention avoids CORS configuration during local development and targets the backend service name in the container environment.

The backend requires HTTP Basic authentication. The sign-in page stores the entered credentials in browser session storage, and `lib/api.ts` attaches them as an `Authorization` header to API calls. Signing out (or a `401`) clears the stored credentials and returns to `/signin`.

## Verification

```bash
npm run typecheck
npm run lint
npm run build
```

## Copy-and-customize boundary

The Acme-specific code is limited to:

- branding in `lib/branding.ts`;
- the Acme API contract in `lib/acme-api.ts`;
- the service profile form in `components/acme-profile-form.tsx`;
- the customer-owned demo sign-in in `lib/auth.ts`, `app/signin/page.tsx`, and `app/customers/layout.tsx`.

The Contacts panel, customer flows, and shared API client remain baseline functionality that Acme carries as its own copy.
