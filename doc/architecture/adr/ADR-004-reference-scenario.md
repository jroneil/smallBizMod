# ADR-004: First proof module and reference scenario

- Status: Accepted
- Date: 2026-08-26

## Context

SBP needs one realistic vertical slice to test reusable artifacts, customer-owned extension data, and the frontend copy model. Building several shallow candidate modules would not provide stronger evidence.

The PRD also requires Contact in the completed V1 baseline. Deferring Contact
during the first proof sequences implementation; it does not remove Contact
from V1.

## Decision

- Customer is the first reusable proof module.
- Acme Service & Repair is the fictional reference customer.
- Acme extends Customer with a service region and account notes in Acme-owned code and persistence.
- The minimum visible flow is list and create Customers, open a Customer, create or edit the Acme profile, and reload the combined view.
- After that proof, V1 adds a separate reusable Contact capability representing
  people associated with Customers.

## Consequences

- Customer remains intentionally generic and does not absorb Acme fields.
- Contact is required for completed V1 but is not part of the first proof slice.
- Contact and the Acme Service Profile are distinct: Contact is reusable person
  data; the Service Profile is Acme-owned service/account data.
- Job and Document remain candidates requiring their own product evidence.
- The reference application may challenge and simplify the framework design.
