# Acme reference frontend

This is the customer-owned Acme Service & Repair frontend. It was copied from the SBP frontend baseline and is now an independent Next.js application. It does not import or depend on `frontend/sbp-ui` at build time or runtime.

## Run the browser proof

The complete reference application can run from the repository root:

```bash
docker compose -f reference/acme/deployment/compose.yaml up --build -d
```

Podman users can use the equivalent `podman compose` command. Open [http://localhost:3000](http://localhost:3000); an unauthenticated visit is sent to the Acme sign-in page. Enter the development/demo credentials from `reference/acme/deployment/.env`, create a customer, open its detail page, add a Contact, complete the separate service region and account notes, save, and reload the page to confirm persistence. Open **Jobs** (labeled "Work Orders") to create, edit, and move work orders through their lifecycle, and use the **Files** panel on a customer or work order detail page to upload, search, download, replace, rename, and delete documents — the customer-owned relabel of the baseline Documents panel. Open **Users** to create, search, bulk import/export, edit, deactivate, and reactivate users, **Roles** to create, edit, retire, and assign roles to users, **Groups** to create, edit, retire, and assign users to groups, **Audit** to review the change history, **Permissions** to review the read-only permission catalog, and **My profile** to view and edit your own non-privileged fields. Use the visible **Sign out** action to clear the tab's authentication state and return to sign-in.

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

## Demo authentication behavior

The sign-in form validates credentials against the Acme-owned `/api/auth/session` endpoint. After a successful sign-in, the frontend stores the HTTP Basic authorization value in browser `sessionStorage` and the centralized API helper adds it to same-origin `/api/...` requests. A 401 clears that state, and the application shell returns the user to `/signin`.

This is intentionally a development/reference mechanism. The stored Basic value is reversible, remains available to JavaScript in the current tab, and is not a production session or identity design. Credentials continue to come from the backend deployment environment; none are compiled into frontend source or public browser configuration.

## Verification

```bash
npm run typecheck
npm run lint
npm run build
```

The generic Customer, Contact, User, Role, Group, Audit, and Permission UI was copied from the SBP baseline. Acme-specific code includes branding, the Acme API contract and service profile form, the self-service "My Profile" page, the audit actor resolver, the customer-owned permission catalog seed, plus this customer-owned demo authentication shell and sign-in page. No authentication code was added to the SBP frontend baseline.
