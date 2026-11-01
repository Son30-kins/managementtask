import type { Metadata } from "next";
import { PreferencesForm } from "@/components/settings/preferences-form";
import { LanguagePills } from "@/components/language-switcher";
import { CalendarFeedCard } from "@/components/settings/calendar-feed-card";
import { CatReminderCard } from "@/components/settings/cat-reminder-card";
import { TelegramCard } from "@/components/settings/telegram-card";
import { Avatar } from "@/components/shell/header";
import { Card, PageHeader } from "@/components/ui";
import { getI18n } from "@/i18n/server";
import { getCalendarFeedStatus, getTelegramStatus } from "@/server/queries";
import { requireUser } from "@/server/session";

export async function generateMetadata(): Promise<Metadata> {
  return { title: (await getI18n()).t.meta.settings };
}

export default async function SettingsPage() {
  const user = await requireUser();
  const [telegram, calendar, { t }] = await Promise.all([getTelegramStatus(), getCalendarFeedStatus(), getI18n()]);
  const st = t.settings;

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <PageHeader title={st.title} description={st.description} />

      <Card className="flex items-center gap-4 p-6">
        <Avatar name={user.name} image={user.image} size={56} />
        <div className="min-w-0">
          <p className="truncate text-lg font-bold">{user.name}</p>
          <p className="truncate text-sm text-fg-2">
            {user.email} · {st.signedInWithGoogle}
          </p>
          {user.role === "ADMIN" && <p className="mt-1 text-xs font-semibold text-brand">{st.administrator}</p>}
        </div>
      </Card>

      <Card className="space-y-4 p-6">
        <div>
          <h2 className="text-lg font-bold">{st.languageTitle}</h2>
          <p className="text-sm text-fg-2">{st.languageHint}</p>
        </div>
        <LanguagePills />
      </Card>

      <CatReminderCard />

      <TelegramCard
        botConfigured={telegram.botConfigured}
        linked={telegram.linked}
        username={telegram.username}
      />

      <CalendarFeedCard enabled={calendar.enabled} />

      <PreferencesForm timezone={user.timezone} settings={user.settings} />
    </div>
  );
}
