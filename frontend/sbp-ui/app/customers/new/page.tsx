import { CustomerCreateForm } from "@/components/customer-create-form";
import { PageHeading } from "@/components/page-heading";

export default function NewCustomerPage() {
  return (
    <>
      <PageHeading
        eyebrow="Customers"
        title="Create customer"
        description="Add the basic customer record now. Customer-specific details can be completed afterward."
      />
      <CustomerCreateForm />
    </>
  );
}
