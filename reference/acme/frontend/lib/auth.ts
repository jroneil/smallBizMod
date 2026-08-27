"use client";

const AUTH_STORAGE_KEY = "acme-reference-auth";
const AUTH_CHANGE_EVENT = "acme-reference-auth-change";

export interface AuthSession {
  username: string;
  authorization: string;
}

export class InvalidCredentialsError extends Error {
  constructor() {
    super("Invalid credentials");
    this.name = "InvalidCredentialsError";
  }
}

export function getStoredAuthValue(): string | null {
  if (typeof window === "undefined") return null;
  return window.sessionStorage.getItem(AUTH_STORAGE_KEY);
}

export function getServerAuthValue(): null {
  return null;
}

export function parseAuthSession(value: string | null): AuthSession | null {
  if (!value) return null;

  try {
    const parsed = JSON.parse(value) as Partial<AuthSession>;
    if (
      typeof parsed.username === "string" &&
      parsed.username.length > 0 &&
      typeof parsed.authorization === "string" &&
      parsed.authorization.startsWith("Basic ")
    ) {
      return {
        username: parsed.username,
        authorization: parsed.authorization,
      };
    }
  } catch {
    // Invalid session data is treated as signed out.
  }

  return null;
}

export function subscribeToAuthChanges(onChange: () => void): () => void {
  window.addEventListener(AUTH_CHANGE_EVENT, onChange);
  window.addEventListener("storage", onChange);

  return () => {
    window.removeEventListener(AUTH_CHANGE_EVENT, onChange);
    window.removeEventListener("storage", onChange);
  };
}

export function getAuthorizationHeader(): string | null {
  return parseAuthSession(getStoredAuthValue())?.authorization ?? null;
}

export function clearAuthSession(): void {
  if (typeof window === "undefined") return;
  window.sessionStorage.removeItem(AUTH_STORAGE_KEY);
  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));
}

export async function authenticate(
  username: string,
  password: string,
): Promise<AuthSession> {
  const authorization = encodeBasicAuthorization(username, password);
  const response = await fetch("/api/auth/session", {
    headers: {
      Accept: "application/json",
      Authorization: authorization,
    },
  });

  if (response.status === 401) throw new InvalidCredentialsError();
  if (!response.ok) {
    throw new Error(`Sign in is temporarily unavailable (${response.status}).`);
  }

  const authenticated = (await response.json()) as { username?: string };
  if (!authenticated.username) {
    throw new Error("Sign in returned an invalid response.");
  }

  const session = { username: authenticated.username, authorization };
  window.sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));
  return session;
}

export function expireAuthSession(): void {
  clearAuthSession();
}

function encodeBasicAuthorization(username: string, password: string): string {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  let binaryValue = "";
  bytes.forEach((byte) => {
    binaryValue += String.fromCharCode(byte);
  });
  return `Basic ${window.btoa(binaryValue)}`;
}
