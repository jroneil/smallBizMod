import { RoleDetail } from "@/components/role-detail";

export default async function RoleDetailPage({
  params,
}: {
  params: Promise<{ roleId: string }>;
}) {
  const { roleId } = await params;

  return <RoleDetail roleId={roleId} />;
}
