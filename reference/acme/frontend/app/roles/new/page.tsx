import { PageHeading } from "@/components/page-heading";
import { RoleCreateForm } from "@/components/role-create-form";

export default function NewRolePage() {
  return (
    <>
      <PageHeading
        eyebrow="Roles"
        title="Create role"
        description="Add a role that can be assigned to users to group what they are allowed to do."
      />
      <RoleCreateForm />
    </>
  );
}
