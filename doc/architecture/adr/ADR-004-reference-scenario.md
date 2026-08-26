# ADR-004: First proof module and reference scenario

- Status: Accepted
- Date: 2026-08-26

## Context

SBP needs one realistic vertical slice to test reusable artifacts, customer-owned extension data, and the frontend copy model. Building several shallow candidate modules would not provide stronger evidence.

## Decision

- Customer is the first reusable proof module.
- Acme Service & Repair is the fictional reference customer.
- Acme extends Customer with a service region and account notes in Acme-owned code and persistence.
- The minimum visible flow is list and create Customers, open a Customer, create or edit the Acme profile, and reload the combined view.

## Consequences

- Customer remains intentionally generic and does not absorb Acme fields.
- Contact, Job, and Document require their own product evidence and are not implied by this decision.
- The reference application may challenge and simplify the framework design.
