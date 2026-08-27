# SBP frontend baseline

This Next.js application is the reusable SBP frontend baseline. It provides a small application shell, the generic Customer list/create/detail flow, and basic Contact list/create behavior on Customer detail. Customer projects copy the baseline and own their resulting frontend; they do not depend on this source tree at runtime.

The current recorded baseline version is `0.1.0`, matching `package.json`. This is a source template version, not a published npm package version.

## Run locally

Start an SBP-compatible backend on `http://localhost:8080`, then run:

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). Browser requests use same-origin `/api/...` paths. Next.js proxies them to `SBP_API_URL`, which defaults to `http://localhost:8080`.

To use another backend URL:

```bash
SBP_API_URL=http://localhost:9080 npm run dev
```

`SBP_API_URL` is server-only configuration and should be set before starting or building Next.js. Components should continue calling relative `/api/...` paths.

## Verification

```bash
npm run typecheck
npm run lint
npm run build
```

## Copy-and-customize boundary

- `lib/branding.ts` is the intentionally small branding seam.
- `lib/api.ts` is the generic Customer and Contact API boundary.
- `app/` and `components/` contain normal Next.js/React application code, not a package or plugin framework.
- Customer-specific screens, API calls, and persistence concepts belong in the copied customer application first.
