import { ApiError, apiRequest, type Customer, type User } from "@/lib/api";

export interface AcmeCustomerProfile {
  customerId: string;
  serviceRegion: string;
  accountNotes: string | null;
}

export interface AcmeCustomerView {
  customer: Customer;
  profile: AcmeCustomerProfile | null;
}

export interface UpdateMyProfileInput {
  email: string;
  givenName: string;
  familyName: string;
  phone: string;
}

export const meApi = {
  get: () => apiRequest<User>("/api/me"),
  update: (input: UpdateMyProfileInput) =>
    apiRequest<User>("/api/me", {
      method: "PUT",
      body: JSON.stringify(input),
    }),
};

export interface AcmeCredentialStatus {
  hasCredential: boolean;
  locked: boolean;
  failedAttempts: number;
  passwordChangeRequired: boolean;
}

export const credentialApi = {
  changePassword: (currentPassword: string, newPassword: string) =>
    apiRequest<AcmeCredentialStatus>("/api/me/password", {
      method: "POST",
      body: JSON.stringify({ currentPassword, newPassword }),
    }),
  status: (userId: string) =>
    apiRequest<AcmeCredentialStatus>(
      `/api/acme/users/${encodeURIComponent(userId)}/credential`,
    ),
  resetPassword: (userId: string, newPassword: string, requireChange: boolean) =>
    apiRequest<AcmeCredentialStatus>(
      `/api/acme/users/${encodeURIComponent(userId)}/password`,
      {
        method: "PUT",
        body: JSON.stringify({ newPassword, requireChange }),
      },
    ),
  unlock: (userId: string) =>
    apiRequest<AcmeCredentialStatus>(
      `/api/acme/users/${encodeURIComponent(userId)}/unlock`,
      { method: "POST" },
    ),
};

export const acmeCustomerApi = {
  async get(customerId: string): Promise<AcmeCustomerView | null> {
    try {
      return await apiRequest<AcmeCustomerView>(
        `/api/acme/customers/${encodeURIComponent(customerId)}`,
      );
    } catch (reason) {
      if (reason instanceof ApiError && reason.status === 404) {
        return null;
      }
      throw reason;
    }
  },

  saveProfile(
    customerId: string,
    profile: { serviceRegion: string; accountNotes: string },
  ) {
    return apiRequest<AcmeCustomerView>(
      `/api/acme/customers/${encodeURIComponent(customerId)}/profile`,
      {
        method: "PUT",
        body: JSON.stringify(profile),
      },
    );
  },
};
