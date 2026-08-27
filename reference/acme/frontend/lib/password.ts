"use client";

/**
 * Client-side password generation for Acme's administrative reset workflow. Passwords are produced
 * in the browser with a cryptographically strong random source, conform to the server-side
 * {@code AcmePasswordPolicy} length bounds, and are never persisted — they are only ever shown in
 * the admin form and copied from that field.
 */

const GENERATED_LENGTH = 16;

// No look-alike characters (0/O, 1/l/I) and no quoting characters, so a generated value is safe to
// display and copy without ambiguity or shell-escaping surprises.
const PASSWORD_CHARSET =
  "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*()-_=+";

export function generatePassword(length: number = GENERATED_LENGTH): string {
  if (length < 1) return "";

  const values = new Uint32Array(length);
  crypto.getRandomValues(values);

  let password = "";
  for (let index = 0; index < length; index += 1) {
    password += PASSWORD_CHARSET[values[index] % PASSWORD_CHARSET.length];
  }
  return password;
}
