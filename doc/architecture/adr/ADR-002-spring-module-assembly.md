# ADR-002: Spring module assembly

- Status: Accepted
- Date: 2026-08-26

## Context

SBP modules are selected through Maven dependencies. The Customer module must become available when its JAR is present, and the reference application must continue to build and start when that dependency is absent. Manual application source wiring would couple the reference application to an optional module, while a custom registry or plugin mechanism would add unsupported complexity.

## Decision

`sbp-module-customer` uses standard Spring Boot auto-configuration metadata.

- The module lists one auto-configuration class in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- That configuration registers only the Customer-owned application, persistence, and web beans required by the module.
- Maven dependency composition determines whether the configuration is present on the application classpath.
- The customer application does not import or scan Customer implementation packages manually.
- No custom registry, dynamic loading, reflection-based discovery, Spring Modulith, or architectural enforcement library is introduced.

## Consequences

- Adding or removing the Customer Maven dependency adds or removes the capability without changing application source.
- The module owns its configuration while the customer application remains the executable composition root.
- Auto-configuration behavior must be verified from a consuming application and from the module's integration tests.
- Package-boundary enforcement remains convention- and review-based until evidence justifies tooling.
