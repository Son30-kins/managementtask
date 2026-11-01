import type { Metadata } from "next";
import { StatsView } from "@/components/stats/stats-view";
import { PageHeader } from "@/components/ui";
import { getI18n } from "@/i18n/server";
import { getStats } from "@/server/queries";
import { requireUser } from "@/server/session";

export async function generateMetadata(): Promise<Metadata> {
  return { title: (await getI18n()).t.meta.stats };
}

export default async function StatsPage() {
  await requireUser();
  const [stats, { t }] = await Promise.all([getStats(), getI18n()]);
  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <PageHeader title={t.stats.title} description={t.stats.description} />
      {stats && <StatsView stats={stats} />}
    </div>
  );
}
