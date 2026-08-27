import { JobCreateForm } from "@/components/job-create-form";
import { PageHeading } from "@/components/page-heading";

export default function NewWorkOrderPage() {
  return (
    <>
      <PageHeading
        eyebrow="Work Orders"
        title="Create work order"
        description="Record a new piece of work for a customer. The work order starts in the Open state and moves through the lifecycle as the work proceeds."
      />
      <JobCreateForm />
    </>
  );
}
