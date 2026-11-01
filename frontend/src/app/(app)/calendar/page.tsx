import type { Metadata } from "next";
import { CalendarView } from "@/components/calendar/calendar-view";
import { isDateKey, monthGrid, weekDays } from "@/core/calendar";
import { localParts } from "@/core/time";
import { getI18n } from "@/i18n/server";
import { listCalendarTasks } from "@/server/queries";
import { requireUser } from "@/server/session";

export async function generateMetadata(): Promise<Metadata> {
  return { title: (await getI18n()).t.meta.calendar };
}

/** Lịch tháng / tuần. ?view=month|week&date=YYYY-MM-DD (mặc định: tháng hiện tại). */
export default async function CalendarPage({ searchParams }: PageProps<"/calendar">) {
  const user = await requireUser();
  const sp = await searchParams;
  const view = sp.view === "week" ? "week" : "month";
  const today = localParts(new Date(), user.timezone).dateKey;
  const anchor = isDateKey(sp.date) ? sp.date : today;
  const days = view === "month" ? monthGrid(anchor) : weekDays(anchor);
  const tasks = await listCalendarTasks(days[0], days[days.length - 1]);

  return <CalendarView view={view} anchor={anchor} days={days} today={today} tasks={tasks} tz={user.timezone} />;
}
