import Link from "next/link";
import { GroupList } from "@/components/group-list";
import { PageHeading } from "@/components/page-heading";

export default function GroupsPage() {
  return (
    <>
      <PageHeading
        eyebrow="Administration"
        title="Groups"
        description="Group users into logical teams. Retired groups keep their history rather than being deleted."
        action={
          <Link
            href="/groups/new"
            className="inline-flex w-fit items-center justify-center rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
          >
            <span className="mr-2 text-lg leading-none" aria-hidden="true">
              +
            </span>
            Create group
          </Link>
        }
      />
      <GroupList />
    </>
  );
}
