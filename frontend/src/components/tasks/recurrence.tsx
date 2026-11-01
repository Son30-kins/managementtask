"use client";

import { Repeat } from "lucide-react";
import { Input, Select } from "@/components/ui";
import type { Messages } from "@/i18n";
import { useI18n } from "@/i18n/provider";
import { cn } from "@/lib/cn";
import { WEEKDAYS, type Recurrence, type Weekday } from "@/lib/types";

const WORKDAYS: Weekday[] = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"];

type Preset = "none" | "daily" | "weekdays" | "weekly" | "monthly" | "custom";

const PRESETS: Record<Exclude<Preset, "none" | "custom">, Recurrence> = {
  daily: { frequency: "DAILY", interval: 1, weekdays: [], monthDay: null },
  weekdays: { frequency: "WEEKLY", interval: 1, weekdays: WORKDAYS, monthDay: null },
  weekly: { frequency: "WEEKLY", interval: 1, weekdays: [], monthDay: null },
  monthly: { frequency: "MONTHLY", interval: 1, weekdays: [], monthDay: null },
};

const sameDays = (a: Weekday[], b: Weekday[]) => a.length === b.length && b.every((d) => a.includes(d));

function presetOf(r: Recurrence | null): Preset {
  if (!r) return "none";
  if (r.interval !== 1) return "custom";
  if (r.frequency === "DAILY") return "daily";
  if (r.frequency === "MONTHLY") return "monthly";
  if (r.weekdays.length === 0) return "weekly";
  return sameDays(r.weekdays, WORKDAYS) ? "weekdays" : "custom";
}

/** "Hằng tuần vào T2, T4" / "Every 2 weeks on Mon, Wed" / "Monatlich am 15." */
export function describeRecurrence(r: Recurrence, t: Messages["recurrence"]): string {
  if (r.frequency === "DAILY") return t.sumDaily(r.interval);
  if (r.frequency === "MONTHLY") return t.sumMonthly(r.interval, r.monthDay);
  if (r.interval === 1 && sameDays(r.weekdays, WORKDAYS)) return t.sumWeekdays;
  const days = WEEKDAYS.filter((d) => r.weekdays.includes(d)).map((d) => t.dayNames[WEEKDAYS.indexOf(d)]);
  return t.sumWeekly(r.interval, days.join(", "));
}

/** Nhãn nhỏ "↻ Hằng tuần" trên thẻ / bảng. */
export function RecurrenceBadge({ recurrence, className }: { recurrence: Recurrence | null; className?: string }) {
  const { t } = useI18n();
  if (!recurrence) return null;
  const text = describeRecurrence(recurrence, t.recurrence);
  return (
    <span className={cn("inline-flex min-w-0 items-center gap-1 text-xs font-medium text-fg-2", className)} title={text}>
      <Repeat size={12} className="shrink-0" />
      <span className="truncate">{text}</span>
    </span>
  );
}

/** Chọn quy tắc lặp: có sẵn vài kiểu thường dùng + tuỳ chỉnh (mỗi N ngày / tuần / tháng, chọn thứ trong tuần). */
export function RecurrencePicker({ value, onChange }: { value: Recurrence | null; onChange: (r: Recurrence | null) => void }) {
  const { t } = useI18n();
  const r = t.recurrence;
  const preset = presetOf(value);

  function choose(next: Preset) {
    if (next === "none") onChange(null);
    else if (next === "custom") onChange(value ?? { ...PRESETS.weekly, interval: 2 });
    else onChange(PRESETS[next]);
  }

  return (
    <div className="space-y-3">
      <Select value={preset} onChange={(e) => choose(e.target.value as Preset)} aria-label={r.label}>
        <option value="none">{r.none}</option>
        <option value="daily">{r.daily}</option>
        <option value="weekdays">{r.weekdays}</option>
        <option value="weekly">{r.weekly}</option>
        <option value="monthly">{r.monthly}</option>
        <option value="custom">{r.custom}</option>
      </Select>

      {preset === "custom" && value && (
        <div className="space-y-3 rounded-xl border border-line p-3">
          <div className="grid grid-cols-[auto_5rem_minmax(0,1fr)] items-center gap-2 text-sm">
            <span className="text-fg-2">{r.every}</span>
            <Input
              type="number"
              min={1}
              max={365}
              value={value.interval}
              onChange={(e) => onChange({ ...value, interval: Math.min(365, Math.max(1, Number(e.target.value) || 1)) })}
              aria-label={r.every}
            />
            <Select
              value={value.frequency}
              onChange={(e) => onChange({ ...value, frequency: e.target.value as Recurrence["frequency"], weekdays: [], monthDay: null })}
              aria-label={r.unit}
            >
              {(["DAILY", "WEEKLY", "MONTHLY"] as const).map((f) => (
                <option key={f} value={f}>
                  {r.units[f][value.interval === 1 ? 0 : 1]}
                </option>
              ))}
            </Select>
          </div>
          {value.frequency === "WEEKLY" && (
            <fieldset className="space-y-1.5">
              <legend className="text-xs text-fg-2">{r.on}</legend>
              <div className="flex flex-wrap gap-1.5">
                {WEEKDAYS.map((d, i) => {
                  const on = value.weekdays.includes(d);
                  return (
                    <button
                      key={d}
                      type="button"
                      aria-pressed={on}
                      onClick={() =>
                        onChange({ ...value, weekdays: on ? value.weekdays.filter((x) => x !== d) : [...value.weekdays, d] })
                      }
                      className={cn(
                        "h-8 min-w-10 rounded-full border px-2 text-xs font-semibold transition",
                        on ? "border-transparent bg-primary text-on-primary" : "border-line-strong text-fg-2 hover:text-fg",
                      )}
                    >
                      {r.dayNames[i]}
                    </button>
                  );
                })}
              </div>
            </fieldset>
          )}
        </div>
      )}
    </div>
  );
}
