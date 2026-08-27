import { GroupCreateForm } from "@/components/group-create-form";
import { PageHeading } from "@/components/page-heading";

export default function NewGroupPage() {
  return (
    <>
      <PageHeading
        eyebrow="Groups"
        title="Create group"
        description="Add a group that users can be assigned to for logical team membership."
      />
      <GroupCreateForm />
    </>
  );
}
