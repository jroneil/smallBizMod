import { expireAuthSession, getAuthorizationHeader } from "@/lib/auth";

export type CustomerStatus = "ACTIVE" | "INACTIVE";

export interface Customer {
  id: string;
  name: string;
  status: CustomerStatus;
}

export type ContactStatus = "ACTIVE" | "INACTIVE";

export interface Contact {
  id: string;
  customerId: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  roleTitle: string | null;
  status: ContactStatus;
}

export interface CreateContactInput {
  customerId: string;
  fullName: string;
  email: string;
  phone: string;
  roleTitle: string;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const authorization = getAuthorizationHeader();
  const response = await fetch(path, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...(authorization ? { Authorization: authorization } : {}),
      ...init?.headers,
    },
  });

  if (response.status === 401) expireAuthSession();

  if (!response.ok) {
    let message = `The request failed (${response.status}).`;

    try {
      const problem = (await response.json()) as {
        detail?: string;
        message?: string;
        title?: string;
      };
      message = problem.detail ?? problem.message ?? problem.title ?? message;
    } catch {
      // Keep the status-based fallback when the response has no JSON body.
    }

    throw new ApiError(message, response.status);
  }

  return (await response.json()) as T;
}

export const customerApi = {
  list: () => apiRequest<Customer[]>("/api/customers"),
  get: (customerId: string) =>
    apiRequest<Customer>(`/api/customers/${encodeURIComponent(customerId)}`),
  create: (name: string) =>
    apiRequest<Customer>("/api/customers", {
      method: "POST",
      body: JSON.stringify({ name }),
    }),
};

export const contactApi = {
  listByCustomer: (customerId: string) =>
    apiRequest<Contact[]>(`/api/customers/${encodeURIComponent(customerId)}/contacts`),
  get: (contactId: string) =>
    apiRequest<Contact>(`/api/contacts/${encodeURIComponent(contactId)}`),
  create: (input: CreateContactInput) =>
    apiRequest<Contact>("/api/contacts", {
      method: "POST",
      body: JSON.stringify(input),
    }),
};
