"use client";

import { CalendarDays, CircleAlert, ListChecks, Sparkles } from "lucide-react";
import Link from "next/link";
import { useState, useTransition } from "react";
import { previewTemplateAction, runTemplateAction, type PreviewItem } from "@/actions/templates";
import { PriorityBadge } from "@/components/tasks/task-bits";
import { Button, Card, Field, Input, Notice, Select, Textarea } from "@/components/ui";
import type { PriorityValue } from "@/core/priority";
import type { TemplateField } from "@/core/template-schema";
import { useI18n } from "@/i18n/provider";

type Values = Record<string, string | boolean>;

function initialValues(fields: TemplateField[]): Values {
  return Object.fromEntries(fields.map((f) => [f.key, f.type === "CHECKBOX" ? false : f.defaultValue]));
}

export function RunTemplateForm({ templateId, fields }: { templateId: string; fields: TemplateField[] }) {
  const { t } = useI18n();
  const [values, setValues] = useState<Values>(() => initialValues(fields));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [preview, setPreview] = useState<{ tasks: PreviewItem[]; warnings: string[] } | null>(null);
  const [created, setCreated] = useState<number | null>(null);
  const [pending, startTransition] = useTransition();

  const set = (key: string, v: string | boolean) => {
    setValues((all) => ({ ...all, [key]: v }));
    setPreview(null); // dữ liệu đổi → xem trước lại
  };

  function doPreview(e?: React.FormEvent) {
    e?.preventDefault();
    setMessage(null);
    startTransition(async () => {
      const res = await previewTemplateAction(templateId, values);
      if (res.ok) {
        setErrors({});
        setPreview({ tasks: res.tasks, warnings: res.warnings });
      } else {
        setErrors(res.errors);
        setMessage(res.message ?? null);
        setPreview(null);
      }
    });
  }

  function doCreate() {
    startTransition(async () => {
      const res = await runTemplateAction(templateId, values);
      if (res.ok) {
        setCreated(res.count);
        setPreview(null);
        setValues(initialValues(fields));
      } else {
        setErrors(res.errors);
        setMessage(res.message ?? null);
      }
    });
  }

  if (created !== null) {
    return (
      <Card className="space-y-4 p-8 text-center">
        <Sparkles className="mx-auto text-success" size={36} />
        <h2 className="text-xl font-bold">{t.run.created(created)}</h2>
        <p className="text-fg-2">{t.run.createdHint}</p>
        <div className="flex justify-center gap-2">
          <Button variant="secondary" onClick={() => setCreated(null)}>
            {t.run.fillAgain}
          </Button>
          <Link href="/" className="rounded-full bg-primary px-6 py-3 text-sm font-semibold text-on-primary">
            {t.run.viewBoard}
          </Link>
        </div>
      </Card>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
      <Card className="p-6">
        <form onSubmit={doPreview} className="space-y-4">
          {message && <Notice tone="danger">{message}</Notice>}
          {fields.length === 0 && <p className="text-sm text-fg-2">{t.run.noFields}</p>}
          {fields.map((f) => (
            <FieldInput key={f.key} field={f} value={values[f.key]} error={errors[f.key]} onChange={(v) => set(f.key, v)} />
          ))}
          <Button type="submit" variant="secondary" disabled={pending} className="w-full">
            {pending && !preview ? t.run.computing : t.run.preview}
          </Button>
        </form>
      </Card>

      <div className="space-y-4">
        {!preview && (
          <Card className="flex min-h-64 flex-col items-center justify-center gap-2 p-8 text-center text-fg-2">
            <ListChecks size={28} />
            {t.run.previewHint}
          </Card>
        )}
        {preview && (
          <>
            {preview.warnings.map((w) => (
              <Notice key={w} tone="warning">
                <span className="inline-flex items-start gap-2">
                  <CircleAlert size={16} className="mt-0.5 shrink-0" /> {w}
                </span>
              </Notice>
            ))}
            {preview.tasks.length === 0 ? (
              <Card className="p-6 text-center text-fg-2">{t.run.nothingToCreate}</Card>
            ) : (
              <ul className="space-y-3">
                {preview.tasks.map((task, i) => (
                  <li key={i}>
                    <Card className="space-y-2 p-5">
                      <div className="flex items-start justify-between gap-3">
                        <h3 className="font-bold">{task.title}</h3>
                        <PriorityBadge priority={task.priority as PriorityValue} />
                      </div>
                      {task.description && <p className="whitespace-pre-line text-sm text-fg-2">{task.description}</p>}
                      <p className="flex items-center gap-2 text-sm">
                        <CalendarDays size={15} className="text-fg-2" />
                        {task.due ? <span className="first-letter:uppercase">{task.due}</span> : <span className="text-fg-2">{t.run.noDue}</span>}
                      </p>
                      {task.subtasks.length > 0 && (
                        <ul className="list-disc pl-5 text-sm text-fg-2">
                          {task.subtasks.map((s) => (
                            <li key={s}>{s}</li>
                          ))}
                        </ul>
                      )}
                      {task.tags.length > 0 && <p className="text-xs text-fg-2">#{task.tags.join(" #")}</p>}
                    </Card>
                  </li>
                ))}
              </ul>
            )}
            {preview.tasks.length > 0 && (
              <Button onClick={doCreate} disabled={pending} className="w-full">
                {pending ? t.run.creating : t.run.create(preview.tasks.length)}
              </Button>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export function FieldInput({
  field,
  value,
  error,
  onChange,
}: {
  field: TemplateField;
  value: string | boolean | undefined;
  error?: string;
  onChange: (v: string | boolean) => void;
}) {
  const { t } = useI18n();
  const label = (
    <>
      {field.label}
      {field.required && <span className="text-danger"> *</span>}
    </>
  );
  const str = typeof value === "string" ? value : "";

  if (field.type === "CHECKBOX") {
    return (
      <label className="flex items-center gap-3 rounded-xl border border-line px-4 py-3">
        <input type="checkbox" checked={value === true} onChange={(e) => onChange(e.target.checked)} className="h-4 w-4" />
        <span className="text-sm font-medium">{field.label}</span>
      </label>
    );
  }
  return (
    <Field label={label} error={error}>
      {field.type === "TEXTAREA" ? (
        <Textarea value={str} onChange={(e) => onChange(e.target.value)} maxLength={2000} />
      ) : field.type === "SELECT" ? (
        <Select value={str} onChange={(e) => onChange(e.target.value)}>
          <option value="">{t.run.choose}</option>
          {field.options.map((o) => (
            <option key={o} value={o}>
              {o}
            </option>
          ))}
        </Select>
      ) : (
        <Input
          value={str}
          onChange={(e) => onChange(e.target.value)}
          type={field.type === "DATE" ? "date" : field.type === "DATETIME" ? "datetime-local" : field.type === "NUMBER" ? "number" : "text"}
          maxLength={2000}
        />
      )}
    </Field>
  );
}
