"use client";

import { AlarmClock, Trash } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { deleteScheduleAction, saveScheduleAction } from "@/actions/templates";
import { Modal } from "@/components/modal";
import { describeRecurrence } from "@/components/tasks/recurrence";
import { FieldInput } from "@/components/templates/run-form";
import { Button, Field, Input, Notice, Select } from "@/components/ui";
import type { TemplateField } from "@/core/template-schema";
import { formatInTz } from "@/core/time";
import { useI18n } from "@/i18n/provider";
import { cn } from "@/lib/cn";
import { WEEKDAYS, type Recurrence, type TemplateSchedule } from "@/lib/types";

const RELATIVE = /^([+-]?\d{1,3})(?:T(\d{2}:\d{2}))?$/;

/** "+4T17:00" → { days: 4, time: "17:00" }; giá trị khác → hôm nay. */
function parseRelative(value: string | boolean | undefined): { days: number; time: string } {
  const m = typeof value === "string" ? RELATIVE.exec(value) : null;
  return { days: m ? Number(m[1]) : 0, time: m?.[2] ?? "17:00" };
}

/** Dòng tóm tắt "⏰ Hằng tuần vào T2 · 08:00 — Lần tới: …" (trang điền form + danh sách form mẫu). */
export function ScheduleSummary({ schedule, tz, className }: { schedule: TemplateSchedule; tz: string; className?: string }) {
  const { locale, t } = useI18n();
  const s = t.schedule;
  const rule = s.summary(describeRecurrence(schedule.recurrence, t.recurrence), schedule.time);
  return (
    <p className={cn("flex flex-wrap items-center gap-x-2 gap-y-1 text-[13px]", className)}>
      <span className={cn("inline-flex items-center gap-1.5 font-semibold", schedule.enabled ? "text-brand" : "text-fg-2")}>
        <AlarmClock size={14} /> {rule}
      </span>
      {schedule.enabled && schedule.nextRunAt && (
        <span className="text-fg-2">{s.next(formatInTz(schedule.nextRunAt, tz, t.formats.weekdayDateTime, locale))}</span>
      )}
      {!schedule.enabled && <span className="text-fg-2">· {schedule.lastError ? s.failed : s.off}</span>}
    </p>
  );
}

/** Nút "Tự chạy theo lịch" + hộp thoại đặt lịch cho một form mẫu. */
export function ScheduleButton({
  templateId,
  fields,
  schedule,
}: {
  templateId: string;
  fields: TemplateField[];
  schedule: TemplateSchedule | null;
}) {
  const { t } = useI18n();
  const [open, setOpen] = useState(false);
  const [version, setVersion] = useState(0);
  return (
    <>
      <button
        type="button"
        onClick={() => {
          setVersion((v) => v + 1); // form mới mỗi lần mở
          setOpen(true);
        }}
        className="inline-flex items-center gap-2 rounded-full border border-line-strong px-5 py-2.5 text-sm font-semibold hover:bg-hover"
      >
        <AlarmClock size={15} /> {t.schedule.button}
      </button>
      <Modal open={open} onClose={() => setOpen(false)} title={t.schedule.button}>
        {open && <ScheduleForm key={version} templateId={templateId} fields={fields} schedule={schedule} onDone={() => setOpen(false)} />}
      </Modal>
    </>
  );
}

function ScheduleForm({
  templateId,
  fields,
  schedule,
  onDone,
}: {
  templateId: string;
  fields: TemplateField[];
  schedule: TemplateSchedule | null;
  onDone: () => void;
}) {
  const { t } = useI18n();
  const s = t.schedule;
  const r = t.recurrence;
  const router = useRouter();
  const [enabled, setEnabled] = useState(schedule?.enabled ?? true);
  const [rule, setRule] = useState<Recurrence>(
    schedule?.recurrence ?? { frequency: "WEEKLY", interval: 1, weekdays: ["MONDAY"], monthDay: null },
  );
  const [time, setTime] = useState(schedule?.time ?? "08:00");
  const [values, setValues] = useState<Record<string, string | boolean>>(() =>
    Object.fromEntries(
      fields.map((f) => [
        f.key,
        schedule?.values[f.key] ??
          (f.type === "CHECKBOX" ? false : f.type === "DATE" ? "+0" : f.type === "DATETIME" ? "+0T17:00" : f.defaultValue),
      ]),
    ),
  );
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  const set = (key: string, v: string | boolean) => {
    setValues((all) => ({ ...all, [key]: v }));
    // Sửa ô nào thì bỏ lỗi của ô đó
    setErrors((all) => Object.fromEntries(Object.entries(all).filter(([k]) => k !== key)));
  };

  function save(e: React.FormEvent) {
    e.preventDefault();
    startTransition(async () => {
      const res = await saveScheduleAction(templateId, { enabled, recurrence: rule, time, values });
      if (res.ok) {
        onDone();
        router.refresh();
      } else {
        setErrors(res.errors);
        setMessage(res.message);
      }
    });
  }

  function remove() {
    if (!confirm(s.confirmRemove)) return;
    startTransition(async () => {
      await deleteScheduleAction(templateId);
      onDone();
      router.refresh();
    });
  }

  return (
    <form onSubmit={save} className="space-y-5">
      <p className="text-sm text-fg-2">{s.intro}</p>
      {message && <Notice tone="danger">{message}</Notice>}

      <label className="flex items-center gap-3 text-sm font-semibold">
        <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} className="h-4 w-4" />
        {s.enabled}
      </label>

      <fieldset className="space-y-3 rounded-2xl border border-line p-4">
        <legend className="px-1 text-[13px] font-semibold text-fg-2">{s.repeat}</legend>
        <div className="grid grid-cols-[auto_5rem_minmax(0,1fr)] items-center gap-2 text-sm">
          <span className="text-fg-2">{r.every}</span>
          <Input
            type="number"
            min={1}
            max={365}
            value={rule.interval}
            onChange={(e) => setRule({ ...rule, interval: Math.min(365, Math.max(1, Number(e.target.value) || 1)) })}
            aria-label={r.every}
          />
          <Select
            value={rule.frequency}
            aria-label={r.unit}
            onChange={(e) => {
              const frequency = e.target.value as Recurrence["frequency"];
              setRule({
                ...rule,
                frequency,
                weekdays: frequency === "WEEKLY" ? ["MONDAY"] : [],
                monthDay: frequency === "MONTHLY" ? 1 : null,
              });
            }}
          >
            {(["DAILY", "WEEKLY", "MONTHLY"] as const).map((f) => (
              <option key={f} value={f}>
                {r.units[f][rule.interval === 1 ? 0 : 1]}
              </option>
            ))}
          </Select>
        </div>
        {rule.frequency === "WEEKLY" && (
          <div className="flex flex-wrap gap-1.5" role="group" aria-label={r.on}>
            {WEEKDAYS.map((d, i) => {
              const on = rule.weekdays.includes(d);
              return (
                <button
                  key={d}
                  type="button"
                  aria-pressed={on}
                  onClick={() => setRule({ ...rule, weekdays: on ? rule.weekdays.filter((x) => x !== d) : [...rule.weekdays, d] })}
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
        )}
        <div className="grid gap-3 sm:grid-cols-2">
          {rule.frequency === "MONTHLY" && (
            <Field label={s.monthDay}>
              <Input
                type="number"
                min={1}
                max={31}
                value={rule.monthDay ?? 1}
                onChange={(e) => setRule({ ...rule, monthDay: Math.min(31, Math.max(1, Number(e.target.value) || 1)) })}
              />
            </Field>
          )}
          <Field label={s.time}>
            <Input type="time" value={time} onChange={(e) => setTime(e.target.value)} required />
          </Field>
        </div>
      </fieldset>

      {fields.length > 0 && (
        <fieldset className="space-y-4">
          <legend className="mb-2 text-[13px] font-semibold text-fg-2">{s.values}</legend>
          {fields.map((f) =>
            f.type === "DATE" || f.type === "DATETIME" ? (
              <RelativeDateInput key={f.key} field={f} value={values[f.key]} error={errors[f.key]} onChange={(v) => set(f.key, v)} />
            ) : (
              <FieldInput key={f.key} field={f} value={values[f.key]} error={errors[f.key]} onChange={(v) => set(f.key, v)} />
            ),
          )}
        </fieldset>
      )}

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line pt-5">
        {schedule ? (
          <Button variant="danger" onClick={remove} disabled={pending}>
            <Trash size={16} /> {s.remove}
          </Button>
        ) : (
          <span />
        )}
        <div className="flex gap-2">
          <Button variant="secondary" onClick={onDone} disabled={pending}>
            {t.common.cancel}
          </Button>
          <Button type="submit" disabled={pending}>
            {pending ? t.common.saving : s.save}
          </Button>
        </div>
      </div>
    </form>
  );
}

/** Trường ngày của lịch: "Ngày chạy + [4] ngày (lúc [17:00])" → lưu "+4" / "+4T17:00". */
function RelativeDateInput({
  field,
  value,
  error,
  onChange,
}: {
  field: TemplateField;
  value: string | boolean | undefined;
  error?: string;
  onChange: (v: string) => void;
}) {
  const { t } = useI18n();
  const s = t.schedule;
  const { days, time } = parseRelative(value);
  const encode = (d: number, tm: string) => (field.type === "DATETIME" ? `+${d}T${tm}` : `+${d}`);
  return (
    <Field
      label={
        <>
          {field.label}
          {field.required && <span className="text-danger"> *</span>}
        </>
      }
      error={error}
    >
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span className="text-fg-2">{s.runDay}</span>
        <div className="w-20">
          <Input type="number" min={0} max={365} value={days} onChange={(e) => onChange(encode(Number(e.target.value) || 0, time))} />
        </div>
        <span className="text-fg-2">{s.days}</span>
        {field.type === "DATETIME" && (
          <>
            <span className="text-fg-2">{s.at}</span>
            <div className="w-32">
              <Input type="time" value={time} onChange={(e) => onChange(encode(days, e.target.value || "17:00"))} />
            </div>
          </>
        )}
      </div>
    </Field>
  );
}
