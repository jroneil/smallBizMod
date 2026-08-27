import { PageHeading } from "@/components/page-heading";
import { UserCreateForm } from "@/components/user-create-form";

export default function NewUserPage() {
  return (
    <>
      <PageHeading
        eyebrow="Users"
        title="Create user"
        description="Provision a new application identity with the profile details your team will recognize."
      />
      <UserCreateForm />
    </>
  );
}
