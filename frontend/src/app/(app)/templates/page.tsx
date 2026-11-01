import { FileStack, Play } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { EmptyState } from "@/components/cats";
import { ScheduleSummary } from "@/components/templates/schedule";
import { TemplateCardActions } from "@/components/templates/template-card-actions";
import { Card, PageHeader } from "@/components/ui";
import { formatInTz } from "@/core/time";
import { getI18n } from "@/i18n/server";
import { listTemplateRuns, listTemplates } from "@/server/queries";
import { requireUser } from "@/server/session";

export async function generateMetadata(): Promise<Metadata> {
  return { title: (await getI18n()).t.meta.templates };
}

export default async function TemplatesPage() {
  const user = await requireUser();
  const [templates, runs, { locale, t }] = await Promise.all([listTemplates(), listTemplateRuns(8), getI18n()]);

  return (
    <div className="space-y-8">
      <PageHeader
        title={t.templates.title}
        description={t.templates.description}
        actions={
          <Link href="/templates/new" className="rounded-full bg-primary px-6 py-3 text-sm font-semibold text-on-primary hover:opacity-90">
            {t.templates.create}
          </Link>
        }
      />

      {templates.length === 0 ? (
        <Card className="px-6 py-14">
          <EmptyState>{t.templates.empty}</EmptyState>
        </Card>
      ) : (
        <div className="grid gap-5 md:grid-cols-2 2xl:grid-cols-3">
          {templates.map((tpl) => (
            <Card key={tpl.id} className="flex flex-col p-6">
              <div className="mb-4 flex items-start justify-between gap-3">
                <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-blush text-brand">
                  <FileStack size={20} />
                </span>
                <TemplateCardActions id={tpl.id} name={tpl.name} />
              </div>
              <h2 className="text-lg font-bold">{tpl.name}</h2>
              {tpl.description && <p className="mt-1 text-sm text-fg-2">{tpl.description}</p>}
              <p className="mt-3 text-[13px] text-fg-2">{t.templates.stats(tpl.fields.length, tpl.rules.length)}</p>
              {tpl.schedule && <ScheduleSummary schedule={tpl.schedule} tz={user.timezone} className="mt-2" />}
              <div className="mt-auto pt-5">
                <Link
                  href={`/templates/${tpl.id}/run`}
                  className="inline-flex items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary hover:opacity-90"
                >
                  <Play size={15} /> {t.templates.fill}
                </Link>
              </div>
            </Card>
          ))}
        </div>
      )}

      {runs.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-lg font-bold">{t.templates.recentRuns}</h2>
          <Card className="divide-y divide-line">
            {runs.map((r) => (
              <div key={r.id} className="flex flex-wrap items-center justify-between gap-2 px-5 py-3 text-sm">
                <span className="font-semibold">{r.templateName}</span>
                <span className="text-fg-2">
                  {t.templates.runSummary(r.taskCount, formatInTz(r.createdAt, user.timezone, t.formats.dateTime, locale))}
                </span>
              </div>
            ))}
          </Card>
        </section>
      )}
    </div>
  );
}
