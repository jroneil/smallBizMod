# ADR-001: Backend artifact and repository topology

- Status: Accepted
- Date: 2026-08-26

## Context

SBP reusable backend capabilities are delivered as versioned Java artifacts consumed by customer applications, but the long-term repository topology was left open by the technical architecture. The first proof needed a concrete project tree without committing to distribution infrastructure that does not yet exist.

## Decision

For v0.1, a single repository holds the reusable artifacts and the reference application.

- A Maven multi-module reactor under `backend/sbp` builds the reusable artifacts together. The reactor is an implementation convenience, not a permanent product requirement.
- Reusable artifacts use the `com.oneilsoftware` group and independent artifact ids:
  - `sbp-parent` (aggregator/parent POM);
  - `sbp-framework` (ordinary JAR);
  - `sbp-module-customer` (ordinary JAR);
  - `sbp-module-contact` (ordinary JAR, added later).
- The executable reference application is an independent Maven project, `com.acmeco.reference:acme-reference-backend`, with its own package namespace.
- The Spring Boot executable/repackage plugin lives only in the customer application. Reusable artifacts are ordinary JARs and never own the application entry point.
- Reusable artifacts remain in one reactor until an actual distribution or multi-team collaboration need justifies splitting them.

## Consequences

- Customer code depends on released artifact coordinates, not on reusable source internals.
- The reactor and the reference application build and test independently of one another.
- A private artifact repository, BOM, and project generator remain deferred until real distribution evidence appears.

## Supersedes

The architecture listed ADR-001 as a required gate before restructuring the generated backend. The topology above is the one the reference implementation actually exercises.
