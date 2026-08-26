const STORAGE_KEY = "acme-demo-credentials";

type Listener = () => void;

let listeners: Listener[] = [];

export interface DemoCredentials {
  username: string;
  password: string;
}

export function getCredentials(): DemoCredentials | null {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as DemoCredentials;
  } catch {
    return null;
  }
}

export function subscribeAuth(listener: Listener): () => void {
  listeners = [...listeners, listener];
  return () => {
    listeners = listeners.filter((existing) => existing !== listener);
  };
}

function notifyAuthChanged(): void {
  for (const listener of listeners) {
    listener();
  }
}

export function setCredentials(credentials: DemoCredentials): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(credentials));
  notifyAuthChanged();
}

export function clearCredentials(): void {
  sessionStorage.removeItem(STORAGE_KEY);
  notifyAuthChanged();
}

export function basicAuthHeader(): string | null {
  const credentials = getCredentials();
  if (!credentials) {
    return null;
  }
  const token = `${credentials.username}:${credentials.password}`;
  return `Basic ${btoa(token)}`;
}
