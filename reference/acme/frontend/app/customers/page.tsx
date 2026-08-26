import Link from "next/link";
import { CustomerList } from "@/components/customer-list";
import { PageHeading } from "@/components/page-heading";

export default function CustomersPage() {
  return (
    <>
      <PageHeading
        eyebrow="Acme customer desk"
        title="Customers"
        description="Keep customer records and service details organized for the Acme team."
        action={
          <Link
            href="/customers/new"
            className="inline-flex w-fit items-center justify-center rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
          >
            <span className="mr-2 text-lg leading-none" aria-hidden="true">
              +
            </span>
            Create customer
          </Link>
        }
      />
      <CustomerList />
    </>
  );
}
