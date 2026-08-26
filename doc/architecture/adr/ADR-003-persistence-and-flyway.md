# ADR-003: Persistence and Flyway ownership

- Status: Accepted
- Date: 2026-08-26

## Context

The first persistent reusable module must prove clear ownership without introducing operational structure for modules that do not yet exist. The architecture required comparison of a schema per module with a shared schema.

A schema per module provides stronger physical separation and independent Flyway histories, but it adds configuration, startup orchestration, and cross-schema considerations before SBP has a second persistent reusable module. A shared schema is conventional and sufficient for the first proof, provided ownership remains explicit in code and migrations.

## Decision

For v0.1, each customer application uses:

- one PostgreSQL database;
- one shared application schema;
- one Flyway schema-history table.

`sbp-module-customer` owns its Customer tables, constraints, indexes when justified, JPA mappings, repositories, and migration files. Its migrations are packaged in the Customer JAR under Flyway's standard migration location. Customer-specific application tables and migrations remain customer-owned.

Migration versions share one ordered history. The first Customer migration uses version `V1`. Coordination is reconsidered only when another persistent reusable module or customer extension creates an actual collision or upgrade problem.

## Consequences

- Standard Spring Boot Flyway startup can discover the migration from the selected module JAR without custom orchestration.
- Removing the module dependency removes its migration from a clean assembly; it does not automatically drop tables already created in an existing database.
- Logical ownership is enforced by artifact/package boundaries and tests rather than PostgreSQL schemas or database users.
- Schema-per-module, multiple Flyway histories, and custom migration coordinators are deferred.
