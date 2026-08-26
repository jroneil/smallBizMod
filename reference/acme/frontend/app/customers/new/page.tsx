import { CustomerCreateForm } from "@/components/customer-create-form";
import { PageHeading } from "@/components/page-heading";

export default function NewCustomerPage() {
  return (
    <>
      <PageHeading
        eyebrow="Customers"
        title="Create customer"
        description="Add the customer record now, then complete the Acme service profile on the next screen."
      />
      <CustomerCreateForm />
    </>
  );
}
