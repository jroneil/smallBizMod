import { PageHeading } from "@/components/page-heading";
import { PermissionCatalog } from "@/components/permission-catalog";

export default function PermissionsPage() {
  return (
    <>
      <PageHeading
        eyebrow="Administration"
        title="Permission catalog"
        description="A read-only reference of the permissions defined by this application and which roles grant them."
      />
      <PermissionCatalog />
    </>
  );
}
