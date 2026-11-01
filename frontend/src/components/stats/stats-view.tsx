"use client";

import { ArrowDownRight, ArrowUpRight } from "lucide-react";
import { useState } from "react";
import { EmptyState } from "@/components/cats";
import { Card } from "@/components/ui";
import { formatDateKey } from "@/core/calendar";
import { useI18n } from "@/i18n/provider";
import { cn } from "@/lib/cn";
import type { Stats } from "@/server/queries";

/** Mốc trục tung gọn: 0 / bước / … / đỉnh (bước 1, 2, 5 × 10^k). */
function niceTicks(max: number): number[] {
  if (max <= 4) return Array.from({ length: Math.max(max, 1) + 1 }, (_, i) => i);
  const raw = max / 4;
  const pow = 10 ** Math.floor(Math.log10(raw));
  const step = [1, 2, 5, 10].map((m) => m * pow).find((s) => s >= raw)!;
  return Array.from({ length: Math.ceil(max / step) + 1 }, (_, i) => i * step);
}

export function StatsView({ stats }: { stats: Stats }) {
  const { t } = useI18n();
  const s = t.stats;
  const nothing = stats.weeks.every((w) => w.onTime + w.late + w.created === 0) && stats.open === 0;
  if (nothing) {
    return (
      <Card className="px-6 py-14">
        <EmptyState>{s.empty}</EmptyState>
      </Card>
    );
  }
  const diff = stats.completed7 - stats.completedPrev7;
  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatTile
          label={s.completed7}
          value={String(stats.completed7)}
          delta={
            diff === 0 ? undefined : (
              <span className={cn("inline-flex items-center gap-1", diff > 0 ? "text-[#2f8a57] dark:text-success" : "text-danger")}>
                {diff > 0 ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
                {s.vsPrev(diff)}
              </span>
            )
          }
        />
        <StatTile label={s.onTime} value={stats.onTimeRate30 === null ? "—" : `${stats.onTimeRate30}%`} />
        <StatTile label={s.open} value={String(stats.open)} />
        <StatTile label={s.overdue} value={String(stats.overdue)} />
      </div>
      <div className="grid gap-6 xl:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <WeeklyChart weeks={stats.weeks} />
        <PriorityBars rows={stats.byPriority} />
      </div>
    </div>
  );
}

function StatTile({ label, value, delta }: { label: string; value: string; delta?: React.ReactNode }) {
  return (
    <Card className="space-y-1 p-5">
      <p className="text-sm text-fg-2">{label}</p>
      <p className="text-3xl font-bold">{value}</p>
      {delta && <p className="text-xs font-medium">{delta}</p>}
    </Card>
  );
}

/** Cột chồng 8 tuần: đúng hạn (dưới) + trễ hạn (trên), khe 2px màu nền giữa 2 phần, tooltip theo từng cột. */
function WeeklyChart({ weeks }: { weeks: Stats["weeks"] }) {
  const { locale, t } = useI18n();
  const s = t.stats;
  const [hover, setHover] = useState<number | null>(null);
  const ticks = niceTicks(Math.max(...weeks.map((w) => w.onTime + w.late), 1));
  const top = ticks[ticks.length - 1];
  const short = (start: string) => formatDateKey(start, locale, { day: "numeric", month: "short" });
  const label = (start: string, i: number) => (i === weeks.length - 1 ? s.thisWeek : short(start));

  return (
    <Card className="space-y-4 p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-bold">{s.weeklyTitle}</h2>
        <div className="flex gap-4 text-xs text-fg-2">
          <span className="inline-flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-sm bg-series-1" /> {s.onTimeSeries}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-sm bg-series-2" /> {s.lateSeries}
          </span>
        </div>
      </div>

      <div className="flex gap-2">
        {/* Trục tung: số tròn, đường lưới mảnh */}
        <div className="relative h-48 w-6 shrink-0 text-right text-[11px] tabular-nums text-fg-2">
          {ticks.map((v) => (
            <span key={v} className="absolute right-0 translate-y-1/2" style={{ bottom: `${(v / top) * 100}%` }}>
              {v}
            </span>
          ))}
        </div>
        <div className="relative h-48 flex-1">
          {ticks.map((v) => (
            <div key={v} className="absolute inset-x-0 h-px bg-line" style={{ bottom: `${(v / top) * 100}%` }} />
          ))}
          <div className="absolute inset-0 grid grid-cols-8">
            {weeks.map((w, i) => {
              const total = w.onTime + w.late;
              const active = hover === i;
              return (
                <div
                  key={w.start}
                  tabIndex={0}
                  role="img"
                  aria-label={`${s.weekOf(short(w.start))}: ${s.onTimeSeries} ${w.onTime}, ${s.lateSeries} ${w.late}, ${s.created} ${w.created}`}
                  onPointerEnter={() => setHover(i)}
                  onPointerLeave={() => setHover(null)}
                  onFocus={() => setHover(i)}
                  onBlur={() => setHover(null)}
                  className="relative flex h-full flex-col items-center justify-end outline-none"
                >
                  {/* Chỉ ghi số tuần này; các tuần khác xem qua trục, tooltip và bảng */}
                  {i === weeks.length - 1 && total > 0 && <span className="mb-1 text-xs font-semibold">{total}</span>}
                  <div className={cn("relative flex w-full max-w-6 flex-col gap-[2px] transition", active && "brightness-110")} style={{ height: `${(total / top) * 100}%` }}>
                    {w.late > 0 && <div className="rounded-t bg-series-2" style={{ flexGrow: w.late }} />}
                    {w.onTime > 0 && <div className={cn("bg-series-1", w.late === 0 && "rounded-t")} style={{ flexGrow: w.onTime }} />}
                    {/* Tooltip ngay trên đỉnh cột, không che tiêu đề / chú giải */}
                    {active && (
                      <div className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-2 w-max min-w-36 -translate-x-1/2 rounded-xl border border-line bg-card px-3 py-2 text-xs shadow-lg">
                        <p className="mb-1 font-semibold text-fg-2">{s.weekOf(short(w.start))}</p>
                        <TooltipRow color="bg-series-1" value={w.onTime} label={s.onTimeSeries} />
                        <TooltipRow color="bg-series-2" value={w.late} label={s.lateSeries} />
                        <TooltipRow value={w.created} label={s.created} />
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
      <div className="ml-8 grid grid-cols-8 text-center text-[11px] text-fg-2">
        {/* Màn hẹp: nhãn xen kẽ (luôn giữ "Tuần này") để mỗi nhãn đủ chỗ, không bị cắt */}
        {weeks.map((w, i) => (
          <span key={w.start} className={cn("whitespace-nowrap", i % 2 === 0 && "max-sm:invisible")}>
            {label(w.start, i)}
          </span>
        ))}
      </div>

      <details className="text-sm">
        <summary className="cursor-pointer text-fg-2 hover:text-fg">{s.table}</summary>
        <table className="mt-3 w-full text-left tabular-nums">
          <thead>
            <tr className="border-b border-line text-xs text-fg-2">
              <th className="py-2 font-semibold">{s.week}</th>
              <th className="py-2 font-semibold">{s.onTimeSeries}</th>
              <th className="py-2 font-semibold">{s.lateSeries}</th>
              <th className="py-2 font-semibold">{s.created}</th>
            </tr>
          </thead>
          <tbody>
            {weeks.map((w) => (
              <tr key={w.start} className="border-b border-line last:border-0">
                <td className="py-1.5">{formatDateKey(w.start, locale, { day: "2-digit", month: "2-digit", year: "numeric" })}</td>
                <td className="py-1.5">{w.onTime}</td>
                <td className="py-1.5">{w.late}</td>
                <td className="py-1.5">{w.created}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </Card>
  );
}

/** Dòng tooltip: số đứng trước (đậm), tên nhóm sau, khoá màu là một nét ngắn. */
function TooltipRow({ color, value, label }: { color?: string; value: number; label: string }) {
  return (
    <p className="flex items-center gap-2">
      <span className={cn("h-0.5 w-3 rounded-full", color ?? "bg-transparent")} />
      <span className="font-bold tabular-nums">{value}</span>
      <span className="text-fg-2">{label}</span>
    </p>
  );
}

/** Thanh ngang một màu: việc đang mở theo mức ưu tiên; số ở đầu thanh, số quá hạn ghi kèm. */
function PriorityBars({ rows }: { rows: Stats["byPriority"] }) {
  const { t } = useI18n();
  const max = Math.max(...rows.map((r) => r.open), 1);
  return (
    <Card className="space-y-4 p-6">
      <h2 className="text-lg font-bold">{t.stats.priorityTitle}</h2>
      <div className="space-y-3">
        {rows.map((r) => (
          <div key={r.priority} className="grid grid-cols-[6rem_minmax(0,1fr)] items-center gap-3 text-sm">
            <span className="truncate text-fg-2">{t.priority[r.priority]}</span>
            <div className="flex items-center gap-2">
              {r.open > 0 && <div className="h-4 max-w-full rounded-r bg-series-1" style={{ width: `${(r.open / max) * 80}%` }} />}
              <span className="shrink-0 font-semibold tabular-nums">{r.open}</span>
              {r.overdue > 0 && <span className="shrink-0 text-xs text-danger">· {t.stats.overdueCount(r.overdue)}</span>}
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
}
