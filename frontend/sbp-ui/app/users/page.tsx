import Link from "next/link";
import { PageHeading } from "@/components/page-heading";
import { UserList } from "@/components/user-list";

export default function UsersPage() {
  return (
    <>
      <PageHeading
        eyebrow="Administration"
        title="Users"
        description="Manage who has access to the application: create accounts, keep profile details current, and control lifecycle state."
        action={
          <div className="flex flex-wrap items-center gap-3">
            <Link
              href="/users/import"
              className="inline-flex w-fit items-center justify-center rounded-lg border border-slate-300 bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
            >
              Bulk import
            </Link>
            <Link
              href="/users/new"
              className="inline-flex w-fit items-center justify-center rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
            >
              <span className="mr-2 text-lg leading-none" aria-hidden="true">
                +
              </span>
              Create user
            </Link>
          </div>
        }
      />
      <UserList />
    </>
  );
}
