import Link from "next/link";
import { PageHeading } from "@/components/page-heading";
import { RoleList } from "@/components/role-list";

export default function RolesPage() {
  return (
    <>
      <PageHeading
        eyebrow="Administration"
        title="Roles"
        description="Define the roles your team can be assigned. Retired roles keep their history rather than being deleted."
        action={
          <Link
            href="/roles/new"
            className="inline-flex w-fit items-center justify-center rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
          >
            <span className="mr-2 text-lg leading-none" aria-hidden="true">
              +
            </span>
            Create role
          </Link>
        }
      />
      <RoleList />
    </>
  );
}
