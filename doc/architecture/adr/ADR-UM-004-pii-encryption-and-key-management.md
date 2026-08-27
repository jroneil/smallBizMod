# ADR-UM-004: PII encryption at rest, key management, and searchable blind indexes

- Status: Accepted (refined 2026-08-27)
- Date: 2026-08-27

## Context

The User Maintenance PRD makes encryption of personal data at rest a hard requirement (NFR-UM-01), with keys managed separately from the data and never in source, committed configuration, or the application database (NFR-UM-02). List/search views must mask encrypted fields by default (NFR-UM-04). UM-FR-01 requires a searchable/filterable User list by name, email, status, role, and group.

Randomized encryption makes the recoverable PII unreadable at rest, but it also made name/email SQL search impossible. That regression is unacceptable: administrators must still be able to find Users by the searchable profile fields the PRD requires, without storing plaintext PII.

## Decision

- **Recoverable PII uses field-level AES-256-GCM** (authenticated encryption). Each encryption uses a fresh random 12-byte IV; the stored value is Base64(IV || ciphertext). `email`, `givenName`, `familyName`, and `phone` are encrypted at rest; `username` stays plaintext (application identity, not personal data).
- **Searchable PII uses a keyed blind index**, not deterministic encryption and not an unkeyed hash. A separate HMAC-SHA-256 key produces a non-reversible search token from the normalized value. `email`, `givenName`, `familyName`, and a combined `fullName` each get a token stored in dedicated columns (`email_token`, `given_name_token`, `family_name_token`, `full_name_token`).
- **Key separation**: the search key (`sbp.usermaintenance.search-key`) is distinct from the encryption key (`sbp.usermaintenance.encryption-key`); the module refuses to start if they are equal, and both are required, externally supplied Base64 values never placed in source, committed configuration, or the database. No KMS integration is introduced.
- **Normalization**: values are trimmed, Unicode NFKC-normalized, then lowercased (`Locale.ROOT`) before tokenization. The same normalization is applied to the query, so search is case- and diacritic-insensitive exact match.
- **Search semantics** (all implemented as a single text `query` plus filters):
  - `query` matches username (case-insensitive substring, SQL) or the normalized name/email tokens (exact): given name, family name, full name ("given family"), or email.
  - `status`, `roleId`, and `groupId` filter by lifecycle state and assignments (role/group via EXISTS subqueries on the assignment joins).
  - This is normalized exact/token matching; arbitrary substring/trigram search over encrypted fields is deliberately not built.
- **Masking** is unchanged: the list endpoint masks `email` and `phone`; the detail endpoint (explicit reveal) returns full decrypted values.
- **Backfill**: `V9` adds the token columns as nullable and adds indexes. A module `ApplicationRunner` backfills tokens for pre-existing encrypted rows at startup by decrypting with the encryption key and tokenizing with the search key. Pre-encryption plaintext rows cannot be backfilled and require a development reset (the reference deployment resets its volume).

## Consequences

- Direct database inspection shows ciphertext for personal fields and opaque HMAC tokens for the search index — no plaintext name/email anywhere in persistence.
- Useful admin search is restored without weakening encryption: username substring, normalized name/email exact match, and status/role/group filtering.
- Blind indexes leak equality of the normalized indexed value under a fixed key, and do not support arbitrary substring search. This is the accepted tradeoff; deterministic encryption was rejected because it would leak plaintext equality and be reversible through brute force.
- The audit log remains value-free (changed field names only), and search tokens never appear in audit or API output.
- The env-provided key approach is reference-only; key rotation and production KMS remain open customer decisions.
