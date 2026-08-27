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
  const response = await fetch(path, {
    ...init,
    headers: {
      Accept: "application/json",
      // Multipart FormData bodies set their own Content-Type (with boundary); everything else
      // that carries a body is JSON.
      ...(init?.body && !(init.body instanceof FormData)
        ? { "Content-Type": "application/json" }
        : {}),
      ...init?.headers,
    },
  });

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

  // Some operations (e.g., DELETE) return 204 with no body.
  if (response.status === 204) {
    return undefined as T;
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

export type UserStatus = "ACTIVE" | "INACTIVE";

export interface User {
  id: string;
  username: string;
  email: string;
  givenName: string;
  familyName: string;
  phone: string | null;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserInput {
  username: string;
  email: string;
  givenName: string;
  familyName: string;
  phone: string;
}

export interface UpdateUserInput {
  email: string;
  givenName: string;
  familyName: string;
  phone: string;
}

export type RoleStatus = "ACTIVE" | "RETIRED";

export interface Role {
  id: string;
  name: string;
  description: string | null;
  status: RoleStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRoleInput {
  name: string;
  description: string;
}

export const roleApi = {
  list: () => apiRequest<Role[]>("/api/roles"),
  get: (roleId: string) =>
    apiRequest<Role>(`/api/roles/${encodeURIComponent(roleId)}`),
  create: (input: CreateRoleInput) =>
    apiRequest<Role>("/api/roles", {
      method: "POST",
      body: JSON.stringify(input),
    }),
  update: (roleId: string, input: CreateRoleInput) =>
    apiRequest<Role>(`/api/roles/${encodeURIComponent(roleId)}`, {
      method: "PUT",
      body: JSON.stringify(input),
    }),
  retire: (roleId: string) =>
    apiRequest<Role>(`/api/roles/${encodeURIComponent(roleId)}/retire`, {
      method: "POST",
    }),
};

export type GroupStatus = "ACTIVE" | "RETIRED";

export interface Group {
  id: string;
  name: string;
  description: string | null;
  status: GroupStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateGroupInput {
  name: string;
  description: string;
}

export const groupApi = {
  list: () => apiRequest<Group[]>("/api/groups"),
  get: (groupId: string) =>
    apiRequest<Group>(`/api/groups/${encodeURIComponent(groupId)}`),
  create: (input: CreateGroupInput) =>
    apiRequest<Group>("/api/groups", {
      method: "POST",
      body: JSON.stringify(input),
    }),
  update: (groupId: string, input: CreateGroupInput) =>
    apiRequest<Group>(`/api/groups/${encodeURIComponent(groupId)}`, {
      method: "PUT",
      body: JSON.stringify(input),
    }),
  retire: (groupId: string) =>
    apiRequest<Group>(`/api/groups/${encodeURIComponent(groupId)}/retire`, {
      method: "POST",
    }),
  members: (groupId: string) =>
    apiRequest<User[]>(`/api/groups/${encodeURIComponent(groupId)}/members`),
  listRoles: (groupId: string) =>
    apiRequest<Role[]>(`/api/groups/${encodeURIComponent(groupId)}/roles`),
  assignRole: (groupId: string, roleId: string) =>
    apiRequest<Role[]>(
      `/api/groups/${encodeURIComponent(groupId)}/roles/${encodeURIComponent(roleId)}`,
      { method: "POST" },
    ),
  removeRole: (groupId: string, roleId: string) =>
    apiRequest<Role[]>(
      `/api/groups/${encodeURIComponent(groupId)}/roles/${encodeURIComponent(roleId)}`,
      { method: "DELETE" },
    ),
};

export interface BulkUserImportError {
  index: number;
  message: string;
}

export interface BulkUserImportResult {
  created: User[];
  errors: BulkUserImportError[];
}

export interface BulkCreateUserInput {
  username: string;
  email: string;
  givenName: string;
  familyName: string;
  phone: string;
}

export const userApi = {
  list: (query?: string, status?: UserStatus, roleId?: string, groupId?: string) => {
    const params = new URLSearchParams();
    if (query) params.set("query", query);
    if (status) params.set("status", status);
    if (roleId) params.set("roleId", roleId);
    if (groupId) params.set("groupId", groupId);
    const queryString = params.toString();
    return apiRequest<User[]>(`/api/users${queryString ? `?${queryString}` : ""}`);
  },
  get: (userId: string) =>
    apiRequest<User>(`/api/users/${encodeURIComponent(userId)}`),
  create: (input: CreateUserInput) =>
    apiRequest<User>("/api/users", {
      method: "POST",
      body: JSON.stringify(input),
    }),
  update: (userId: string, input: UpdateUserInput) =>
    apiRequest<User>(`/api/users/${encodeURIComponent(userId)}`, {
      method: "PUT",
      body: JSON.stringify(input),
    }),
  deactivate: (userId: string) =>
    apiRequest<User>(`/api/users/${encodeURIComponent(userId)}/deactivate`, {
      method: "POST",
    }),
  activate: (userId: string) =>
    apiRequest<User>(`/api/users/${encodeURIComponent(userId)}/activate`, {
      method: "POST",
    }),
  listRoles: (userId: string) =>
    apiRequest<Role[]>(`/api/users/${encodeURIComponent(userId)}/roles`),
  listEffectiveRoles: (userId: string) =>
    apiRequest<Role[]>(`/api/users/${encodeURIComponent(userId)}/effective-roles`),
  assignRole: (userId: string, roleId: string) =>
    apiRequest<Role[]>(
      `/api/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleId)}`,
      { method: "POST" },
    ),
  removeRole: (userId: string, roleId: string) =>
    apiRequest<Role[]>(
      `/api/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleId)}`,
      { method: "DELETE" },
    ),
  listGroups: (userId: string) =>
    apiRequest<Group[]>(`/api/users/${encodeURIComponent(userId)}/groups`),
  assignGroup: (userId: string, groupId: string) =>
    apiRequest<Group[]>(
      `/api/users/${encodeURIComponent(userId)}/groups/${encodeURIComponent(groupId)}`,
      { method: "POST" },
    ),
  removeGroup: (userId: string, groupId: string) =>
    apiRequest<Group[]>(
      `/api/users/${encodeURIComponent(userId)}/groups/${encodeURIComponent(groupId)}`,
      { method: "DELETE" },
    ),
  bulkImport: (rows: BulkCreateUserInput[]) =>
    apiRequest<BulkUserImportResult>("/api/users/bulk", {
      method: "POST",
      body: JSON.stringify(rows),
    }),
  export: () => apiRequest<User[]>("/api/users/export"),
};

export interface AuditEvent {
  id: string;
  actor: string | null;
  subjectType: string;
  subjectId: string;
  action: string;
  changedFields: string[];
  detail: string | null;
  occurredAt: string;
}

export interface AuditFilterInput {
  actor?: string;
  subjectType?: string;
  action?: string;
}

export const auditApi = {
  list: (filter: AuditFilterInput = {}) => {
    const params = new URLSearchParams();
    if (filter.actor) params.set("actor", filter.actor);
    if (filter.subjectType) params.set("subjectType", filter.subjectType);
    if (filter.action) params.set("action", filter.action);
    params.set("limit", "100");
    return apiRequest<AuditEvent[]>(`/api/audit?${params.toString()}`);
  },
};

export interface Permission {
  id: string;
  key: string;
  description: string;
}

export const permissionApi = {
  list: () => apiRequest<Permission[]>("/api/permissions"),
  listByRole: (roleId: string) =>
    apiRequest<Permission[]>(`/api/roles/${encodeURIComponent(roleId)}/permissions`),
  listRolesByPermission: (permissionKey: string) =>
    apiRequest<Role[]>(`/api/permissions/${encodeURIComponent(permissionKey)}/roles`),
};

export type JobStatus = "OPEN" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type JobPriority = "LOW" | "NORMAL" | "HIGH";

export interface Job {
  id: string;
  customerId: string;
  title: string;
  description: string | null;
  status: JobStatus;
  priority: JobPriority;
  scheduledDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateJobInput {
  customerId: string;
  title: string;
  description: string;
  priority: JobPriority;
  scheduledDate: string | null;
}

export interface UpdateJobInput {
  title: string;
  description: string;
  priority: JobPriority;
  scheduledDate: string | null;
}

export interface JobPage {
  items: Job[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface JobAuditEvent {
  id: string;
  actor: string | null;
  action: string;
  changedFields: string[];
  detail: string | null;
  occurredAt: string;
}

export interface JobListParams {
  query?: string;
  customerId?: string;
  status?: JobStatus;
  priority?: JobPriority;
  page?: number;
  size?: number;
}

export const jobApi = {
  list: (params: JobListParams = {}) => {
    const search = new URLSearchParams();
    if (params.query) search.set("query", params.query);
    if (params.customerId) search.set("customerId", params.customerId);
    if (params.status) search.set("status", params.status);
    if (params.priority) search.set("priority", params.priority);
    if (params.page !== undefined) search.set("page", String(params.page));
    if (params.size !== undefined) search.set("size", String(params.size));
    const queryString = search.toString();
    return apiRequest<JobPage>(`/api/jobs${queryString ? `?${queryString}` : ""}`);
  },
  get: (jobId: string) =>
    apiRequest<Job>(`/api/jobs/${encodeURIComponent(jobId)}`),
  create: (input: CreateJobInput) =>
    apiRequest<Job>("/api/jobs", {
      method: "POST",
      body: JSON.stringify(input),
    }),
  update: (jobId: string, input: UpdateJobInput) =>
    apiRequest<Job>(`/api/jobs/${encodeURIComponent(jobId)}`, {
      method: "PUT",
      body: JSON.stringify(input),
    }),
  start: (jobId: string) =>
    apiRequest<Job>(`/api/jobs/${encodeURIComponent(jobId)}/start`, {
      method: "POST",
    }),
  complete: (jobId: string) =>
    apiRequest<Job>(`/api/jobs/${encodeURIComponent(jobId)}/complete`, {
      method: "POST",
    }),
  cancel: (jobId: string) =>
    apiRequest<Job>(`/api/jobs/${encodeURIComponent(jobId)}/cancel`, {
      method: "POST",
    }),
  listByCustomer: (customerId: string) =>
    apiRequest<Job[]>(`/api/customers/${encodeURIComponent(customerId)}/jobs`),
  listAuditEvents: (jobId: string, action?: string) => {
    const params = new URLSearchParams();
    if (action) params.set("action", action);
    const queryString = params.toString();
    return apiRequest<JobAuditEvent[]>(
      `/api/jobs/${encodeURIComponent(jobId)}/audit-events${queryString ? `?${queryString}` : ""}`,
    );
  },
};

export type DocumentOwnerType = "CUSTOMER" | "JOB";

export interface Document {
  id: string;
  ownerType: DocumentOwnerType;
  ownerId: string;
  displayName: string;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDocumentInput {
  file: File;
  ownerType: DocumentOwnerType;
  ownerId: string;
  displayName?: string;
}

export interface DocumentPage {
  items: Document[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DocumentAuditEvent {
  id: string;
  actor: string | null;
  action: string;
  changedFields: string[];
  detail: string | null;
  occurredAt: string;
}

export interface DocumentListParams {
  query?: string;
  ownerType?: DocumentOwnerType;
  ownerId?: string;
  page?: number;
  size?: number;
}

export const documentApi = {
  list: (params: DocumentListParams = {}) => {
    const search = new URLSearchParams();
    if (params.query) search.set("query", params.query);
    if (params.ownerType) search.set("ownerType", params.ownerType);
    if (params.ownerId) search.set("ownerId", params.ownerId);
    if (params.page !== undefined) search.set("page", String(params.page));
    if (params.size !== undefined) search.set("size", String(params.size));
    const queryString = search.toString();
    return apiRequest<DocumentPage>(
      `/api/documents${queryString ? `?${queryString}` : ""}`,
    );
  },
  get: (documentId: string) =>
    apiRequest<Document>(`/api/documents/${encodeURIComponent(documentId)}`),
  create: (input: CreateDocumentInput) => {
    const form = new FormData();
    form.append("file", input.file);
    form.append("ownerType", input.ownerType);
    form.append("ownerId", input.ownerId);
    if (input.displayName) form.append("displayName", input.displayName);
    return apiRequest<Document>("/api/documents", {
      method: "POST",
      body: form,
    });
  },
  updateDisplayName: (documentId: string, displayName: string) =>
    apiRequest<Document>(`/api/documents/${encodeURIComponent(documentId)}`, {
      method: "PUT",
      body: JSON.stringify({ displayName }),
    }),
  replaceContent: (documentId: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return apiRequest<Document>(
      `/api/documents/${encodeURIComponent(documentId)}/content`,
      { method: "PUT", body: form },
    );
  },
  delete: (documentId: string) =>
    apiRequest<void>(`/api/documents/${encodeURIComponent(documentId)}`, {
      method: "DELETE",
    }),
  download: async (documentId: string): Promise<Blob> => {
    const response = await fetch(
      `/api/documents/${encodeURIComponent(documentId)}/content`,
    );
    if (!response.ok) {
      let message = `The download failed (${response.status}).`;
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
    return response.blob();
  },
  listAuditEvents: (documentId: string, action?: string) => {
    const params = new URLSearchParams();
    if (action) params.set("action", action);
    const queryString = params.toString();
    return apiRequest<DocumentAuditEvent[]>(
      `/api/documents/${encodeURIComponent(documentId)}/audit-events${queryString ? `?${queryString}` : ""}`,
    );
  },
};
