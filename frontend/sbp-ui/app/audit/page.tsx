import { AuditLog } from "@/components/audit-log";
import { PageHeading } from "@/components/page-heading";

export default function AuditPage() {
  return (
    <>
      <PageHeading
        eyebrow="Administration"
        title="Audit log"
        description="A read-only history of user, role, and group changes. Personal-data values are never shown — only which fields changed."
      />
      <AuditLog />
    </>
  );
}
