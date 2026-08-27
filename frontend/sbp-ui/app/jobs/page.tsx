import Link from "next/link";
import { JobList } from "@/components/job-list";
import { PageHeading } from "@/components/page-heading";

export default function JobsPage() {
  return (
    <>
      <PageHeading
        eyebrow="Operations"
        title="Jobs"
        description="Track the work your team performs for customers: create jobs, keep the details current, and move each one through its lifecycle."
        action={
          <Link
            href="/jobs/new"
            className="inline-flex w-fit items-center justify-center rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
          >
            <span className="mr-2 text-lg leading-none" aria-hidden="true">
              +
            </span>
            Create job
          </Link>
        }
      />
      <JobList />
    </>
  );
}
