import Link from "next/link";
import { JobList } from "@/components/job-list";
import { PageHeading } from "@/components/page-heading";

export default function WorkOrdersPage() {
  return (
    <>
      <PageHeading
        eyebrow="Operations"
        title="Work Orders"
        description="Track the work Acme performs for customers: create work orders, keep the details current, and move each one through its lifecycle."
        action={
          <Link
            href="/jobs/new"
            className="inline-flex w-fit items-center justify-center rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
          >
            <span className="mr-2 text-lg leading-none" aria-hidden="true">
              +
            </span>
            Create work order
          </Link>
        }
      />
      <JobList />
    </>
  );
}
