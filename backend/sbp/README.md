# SBP reusable backend artifacts

The backend reactor produces ordinary Maven JARs. It does not produce a standalone SBP service.

## Artifacts

- `com.oneilsoftware:sbp-framework:0.1.0-SNAPSHOT` is the minimal framework artifact. It is intentionally empty in the first proof rather than containing unused abstractions.
- `com.oneilsoftware:sbp-module-customer:0.1.0-SNAPSHOT` provides the generic Customer capability and depends only on the framework artifact and its declared Spring/runtime dependencies.
- `com.oneilsoftware:sbp-module-contact:0.1.0-SNAPSHOT` provides people associated with Customers and depends on the Customer module's public contract.

The Acme executable application pins and consumes both artifacts from its own Maven project. A local install is sufficient for this baseline:

```bash
./backend/sbp/mvnw -f backend/sbp/pom.xml clean install
```

No BOM or artifact repository is required yet.

## API boundary

Reusable Customer contracts are under:

```text
com.oneilsoftware.sbp.module.customer.api
```

Reusable Contact contracts are under:

```text
com.oneilsoftware.sbp.module.contact.api
```

Consumers may use those types. Packages under:

```text
com.oneilsoftware.sbp.module.customer.internal
```

are implementation details and are not compatibility promises. The module exposes `CustomerOperations` for in-process composition and owns its repository, entity, table, and Flyway migration. Customer applications must not access those internals.

Spring Boot auto-configuration activates the module when its JAR is present. There is no custom module registry, reflection-based discovery, or separately deployed module process.

Contact uses `CustomerOperations` to validate its Customer identifier. It owns its entity, repository, table, and Flyway migration, does not read Customer persistence, and does not create a cross-module foreign key.
