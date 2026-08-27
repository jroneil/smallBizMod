import { MyProfile } from "@/components/my-profile";
import { PageHeading } from "@/components/page-heading";

export default function MePage() {
  return (
    <>
      <PageHeading
        eyebrow="Self-service"
        title="My profile"
        description="View and edit your own profile fields. Account access, roles, and groups remain administrator-managed."
      />
      <MyProfile />
    </>
  );
}
