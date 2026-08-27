import { JobCreateForm } from "@/components/job-create-form";
import { PageHeading } from "@/components/page-heading";

export default function NewJobPage() {
  return (
    <>
      <PageHeading
        eyebrow="Jobs"
        title="Create job"
        description="Record a new piece of work for a customer. The job starts in the Open state and moves through the lifecycle as the work proceeds."
      />
      <JobCreateForm />
    </>
  );
}
