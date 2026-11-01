"use client";

import { useMemo, useState, useTransition } from "react";
import { savePreferencesAction } from "@/actions/settings";
import { Button, Card, Field, Input, Notice, Select } from "@/components/ui";
import { PRIORITIES, type PriorityValue } from "@/core/priority";
import { formatOffset, REMINDER_OFFSET_CHOICES, type UserSettings } from "@/core/user-settings";
import { useI18n } from "@/i18n/provider";
import { cn } from "@/lib/cn";

export function PreferencesForm({ timezone, settings }: { timezone: string; settings: UserSettings }) {
  const { t } = useI18n();
  const st = t.settings;
  const [tz, setTz] = useState(timezone);
  const [s, setS] = useState<UserSettings>(settings);
  const [notice, setNotice] = useState<{ tone: "success" | "danger"; text: string } | null>(null);
  const [pending, startTransition] = useTransition();

  const zones = useMemo(() => {
    const list = typeof Intl.supportedValuesOf === "function" ? Intl.supportedValuesOf("timeZone") : [];
    return list.includes(timezone) ? list : [timezone, ...list];
  }, [timezone]);

  function toggleOffset(p: PriorityValue, minutes: number) {
    setS((cur) => {
      const has = cur.reminderOffsets[p].includes(minutes);
      const next = has ? cur.reminderOffsets[p].filter((m) => m !== minutes) : [...cur.reminderOffsets[p], minutes].sort((a, b) => b - a);
      return { ...cur, reminderOffsets: { ...cur.reminderOffsets, [p]: next.slice(0, 10) } };
    });
  }

  function save() {
    startTransition(async () => {
      const res = await savePreferencesAction({ timezone: tz, settings: s });
      setNotice(res.ok ? { tone: "success", text: res.message ?? t.common.saved } : { tone: "danger", text: res.error });
    });
  }

  return (
    <>
      <Card className="space-y-5 p-6">
        <div>
          <h2 className="text-lg font-bold">{st.remindersTitle}</h2>
          <p className="text-sm text-fg-2">{st.remindersHint}</p>
        </div>
        {PRIORITIES.map((p) => (
          <div key={p} className="space-y-2">
            <p className="text-sm font-semibold">{t.priority[p]}</p>
            <div className="flex flex-wrap gap-1.5">
              {REMINDER_OFFSET_CHOICES.map((minutes) => {
                const on = s.reminderOffsets[p].includes(minutes);
                return (
                  <button
                    key={minutes}
                    type="button"
                    aria-pressed={on}
                    onClick={() => toggleOffset(p, minutes)}
                    className={cn(
                      "rounded-full border px-3 py-1.5 text-xs font-semibold transition",
                      on ? "border-transparent bg-primary text-on-primary" : "border-line-strong text-fg-2 hover:text-fg",
                    )}
                  >
                    {formatOffset(minutes, t.offsets)}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
        <Field label={st.overdueRepeat} className="max-w-xs">
          <Select value={s.overdueRepeatHours} onChange={(e) => setS({ ...s, overdueRepeatHours: Number(e.target.value) })}>
            {[4, 8, 12, 24, 48].map((h) => (
              <option key={h} value={h}>
                {st.hours(h)}
              </option>
            ))}
          </Select>
        </Field>
      </Card>

      <Card className="space-y-5 p-6">
        <h2 className="text-lg font-bold">{st.timeTitle}</h2>
        <Field label={st.timezone} className="max-w-md">
          <Select value={tz} onChange={(e) => setTz(e.target.value)}>
            {zones.map((z) => (
              <option key={z} value={z}>
                {z}
              </option>
            ))}
          </Select>
        </Field>

        <div className="space-y-3">
          <label className="flex items-center gap-3 text-sm font-semibold">
            <input
              type="checkbox"
              checked={s.quietHours.enabled}
              onChange={(e) => setS({ ...s, quietHours: { ...s.quietHours, enabled: e.target.checked } })}
              className="h-4 w-4"
            />
            {st.quietHours}
          </label>
          {s.quietHours.enabled && (
            <div className="flex max-w-sm gap-3">
              <Field label={st.from} className="flex-1">
                <Input type="time" value={s.quietHours.start} onChange={(e) => setS({ ...s, quietHours: { ...s.quietHours, start: e.target.value } })} />
              </Field>
              <Field label={st.to} className="flex-1">
                <Input type="time" value={s.quietHours.end} onChange={(e) => setS({ ...s, quietHours: { ...s.quietHours, end: e.target.value } })} />
              </Field>
            </div>
          )}
        </div>

        <div className="space-y-3">
          <label className="flex items-center gap-3 text-sm font-semibold">
            <input
              type="checkbox"
              checked={s.digest.enabled}
              onChange={(e) => setS({ ...s, digest: { ...s.digest, enabled: e.target.checked } })}
              className="h-4 w-4"
            />
            {st.digest}
          </label>
          <label className="flex items-center gap-3 text-sm font-semibold">
            <input
              type="checkbox"
              checked={s.weeklyReport !== false}
              onChange={(e) => setS({ ...s, weeklyReport: e.target.checked })}
              className="h-4 w-4"
            />
            {st.weeklyReport}
          </label>
          {(s.digest.enabled || s.weeklyReport !== false) && (
            <Field label={st.digestAt} className="max-w-[10rem]">
              <Input type="time" value={s.digest.time} onChange={(e) => setS({ ...s, digest: { ...s.digest, time: e.target.value } })} />
            </Field>
          )}
        </div>
      </Card>

      {notice && <Notice tone={notice.tone}>{notice.text}</Notice>}
      <div className="flex justify-end">
        <Button onClick={save} disabled={pending}>
          {pending ? t.common.saving : st.save}
        </Button>
      </div>
    </>
  );
}
