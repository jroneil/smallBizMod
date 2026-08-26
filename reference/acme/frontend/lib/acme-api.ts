import { ApiError, apiRequest, type Customer } from "@/lib/api";

export interface AcmeCustomerProfile {
  customerId: string;
  serviceRegion: string;
  accountNotes: string | null;
}

export interface AcmeCustomerView {
  customer: Customer;
  profile: AcmeCustomerProfile;
}

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
