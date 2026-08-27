import { BulkImportForm } from "@/components/bulk-import";
import { PageHeading } from "@/components/page-heading";

export default function BulkImportPage() {
  return (
    <>
      <PageHeading
        eyebrow="Users"
        title="Bulk import"
        description="Provision multiple users from a JSON array. Each row is validated before commit and reported individually."
      />
      <BulkImportForm />
    </>
  );
}
