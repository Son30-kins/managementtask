"use client";

import { Plus, Trash, X } from "lucide-react";
import { createContext, use, useState, useTransition } from "react";
import { createTaskAction, deleteTaskAction, updateTaskAction, type TaskFormData } from "@/actions/tasks";
import { Modal } from "@/components/modal";
import { Button, Field, Input, Notice, Select, Textarea } from "@/components/ui";
import { PRIORITIES, STATUSES, type PriorityValue, type StatusValue } from "@/core/priority";
import { formatInTz, toLocalInputValue } from "@/core/time";
import { getMessages } from "@/i18n";
import { DEFAULT_LOCALE } from "@/i18n/config";
import { useI18n } from "@/i18n/provider";
import { cn } from "@/lib/cn";
import { RecurrencePicker } from "./recurrence";
import type { TaskDTO } from "./task-bits";
import type { Recurrence } from "@/lib/types";

type DialogState = { mode: "new"; status: StatusValue; due?: string } | { mode: "edit"; task: TaskDTO } | null;

/** openNew(trạng thái, hạn "YYYY-MM-DDTHH:mm") — trang Lịch mở sẵn hạn của ngày được bấm. */
const Ctx = createContext<{ openNew: (status?: StatusValue, due?: string) => void; openEdit: (task: TaskDTO) => void } | null>(null);

export function useTaskDialog() {
  const ctx = use(Ctx);
  if (!ctx) throw new Error(getMessages(DEFAULT_LOCALE).taskDialog.outsideProvider);
  return ctx;
}

export function TaskDialogProvider({ tz, children }: { tz: string; children: React.ReactNode }) {
  const { t } = useI18n();
  const [state, setState] = useState<DialogState>(null);
  const [version, setVersion] = useState(0);

  const open = (s: DialogState) => {
    setState(s);
    setVersion((v) => v + 1); // form mới mỗi lần mở
  };

  return (
    <Ctx value={{ openNew: (status = "TODO", due) => open({ mode: "new", status, due }), openEdit: (task) => open({ mode: "edit", task }) }}>
      {children}
      <Modal open={state !== null} onClose={() => setState(null)} title={state?.mode === "edit" ? t.taskDialog.editTitle : t.taskDialog.newTitle}>
        {state && <TaskForm key={version} state={state} tz={tz} onDone={() => setState(null)} />}
      </Modal>
    </Ctx>
  );
}

function TaskForm({ state, tz, onDone }: { state: NonNullable<DialogState>; tz: string; onDone: () => void }) {
  const { locale, t } = useI18n();
  const td = t.taskDialog;
  const task = state.mode === "edit" ? state.task : null;
  const [title, setTitle] = useState(task?.title ?? "");
  const [description, setDescription] = useState(task?.description ?? "");
  const [due, setDue] = useState(task?.dueAt ? toLocalInputValue(new Date(task.dueAt), tz) : state.mode === "new" ? (state.due ?? "") : "");
  const [priority, setPriority] = useState<PriorityValue>(task?.priority ?? "MEDIUM");
  const [status, setStatus] = useState<StatusValue>(task?.status ?? (state.mode === "new" ? state.status : "TODO"));
  const [tags, setTags] = useState((task?.tags ?? []).join(", "));
  const [recurrence, setRecurrence] = useState<Recurrence | null>(task?.recurrence ?? null);
  const [subtasks, setSubtasks] = useState(task?.subtasks.map((s) => ({ title: s.title, done: s.done })) ?? []);
  const [newSub, setNewSub] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  function addSub() {
    if (!newSub.trim()) return;
    setSubtasks((s) => [...s, { title: newSub.trim(), done: false }]);
    setNewSub("");
  }

  function save(e: React.FormEvent) {
    e.preventDefault();
    if (recurrence && !due) {
      setError(t.recurrence.needsDue);
      return;
    }
    const data: TaskFormData = {
      title,
      description,
      due,
      priority,
      status,
      tags: tags.split(",").map((t) => t.trim()).filter(Boolean),
      recurrence,
      subtasks: newSub.trim() ? [...subtasks, { title: newSub.trim(), done: false }] : subtasks,
    };
    startTransition(async () => {
      const res = task ? await updateTaskAction(task.id, data) : await createTaskAction(data);
      if (res.ok) onDone();
      else setError(res.error);
    });
  }

  function remove() {
    if (!task || !confirm(td.confirmDelete(task.title))) return;
    startTransition(async () => {
      const res = await deleteTaskAction(task.id);
      if (res.ok) onDone();
      else setError(res.error);
    });
  }

  return (
    <form onSubmit={save} className="space-y-5">
      {error && <Notice tone="danger">{error}</Notice>}
      <Field label={td.title}>
        <Input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} autoFocus placeholder={td.titlePlaceholder} />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label={td.due} hint={td.dueHint(tz)}>
          <Input type="datetime-local" value={due} onChange={(e) => setDue(e.target.value)} />
        </Field>
        <Field label={td.status}>
          <Select value={status} onChange={(e) => setStatus(e.target.value as StatusValue)}>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {t.status[s]}
              </option>
            ))}
          </Select>
        </Field>
      </div>

      <Field label={t.recurrence.label} hint={recurrence && !due ? undefined : t.recurrence.hint} error={recurrence && !due ? t.recurrence.needsDue : undefined}>
        <RecurrencePicker value={recurrence} onChange={setRecurrence} />
      </Field>

      <fieldset className="space-y-1.5">
        <legend className="text-[13px] font-semibold text-fg-2">{td.priority}</legend>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {PRIORITIES.map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => setPriority(p)}
              aria-pressed={priority === p}
              className={cn(
                "rounded-full border px-3 py-2 text-sm font-semibold transition",
                priority === p ? "border-transparent bg-primary text-on-primary" : "border-line-strong hover:bg-hover",
              )}
            >
              {t.priority[p]}
            </button>
          ))}
        </div>
      </fieldset>

      <Field label={td.description}>
        <Textarea value={description} onChange={(e) => setDescription(e.target.value)} maxLength={5000} placeholder={td.descriptionPlaceholder} />
      </Field>

      <div className="space-y-2">
        <span className="text-[13px] font-semibold text-fg-2">
          {td.subtasks} {subtasks.length > 0 && `(${subtasks.filter((s) => s.done).length}/${subtasks.length})`}
        </span>
        <ul className="space-y-1.5">
          {subtasks.map((s, i) => (
            <li key={i} className="flex items-center gap-3 rounded-xl border border-line px-3 py-2">
              <input
                type="checkbox"
                checked={s.done}
                onChange={(e) => setSubtasks((all) => all.map((x, j) => (j === i ? { ...x, done: e.target.checked } : x)))}
                className="h-4 w-4 accent-[var(--color-brand)]"
                aria-label={td.subtaskDone(s.title)}
              />
              <span className={cn("flex-1 text-sm", s.done && "text-fg-2 line-through")}>{s.title}</span>
              <button
                type="button"
                onClick={() => setSubtasks((all) => all.filter((_, j) => j !== i))}
                aria-label={td.removeSubtask}
                className="text-fg-2 hover:text-danger"
              >
                <X size={16} />
              </button>
            </li>
          ))}
        </ul>
        <div className="flex gap-2">
          <Input
            value={newSub}
            onChange={(e) => setNewSub(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                addSub();
              }
            }}
            placeholder={td.addSubtaskPlaceholder}
            maxLength={200}
          />
          <Button variant="secondary" onClick={addSub} aria-label={td.addSubtask} className="px-4">
            <Plus size={16} />
          </Button>
        </div>
      </div>

      <Field label={td.tags} hint={td.tagsHint}>
        <Input value={tags} onChange={(e) => setTags(e.target.value)} placeholder={td.tagsPlaceholder} />
      </Field>

      {task && (
        <p className="text-xs text-fg-2">
          {td.createdAt(formatInTz(new Date(task.createdAt), tz, t.formats.dateTime, locale))}
          {task.templateName && td.fromTemplate(task.templateName)}
          {task.completedAt && td.completedAt(formatInTz(new Date(task.completedAt), tz, t.formats.dateTime, locale))}
        </p>
      )}

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line pt-5">
        {task ? (
          <Button variant="danger" onClick={remove} disabled={pending}>
            <Trash size={16} /> {t.common.delete}
          </Button>
        ) : (
          <span />
        )}
        <div className="flex gap-2">
          <Button variant="secondary" onClick={onDone} disabled={pending}>
            {t.common.cancel}
          </Button>
          <Button type="submit" disabled={pending}>
            {pending ? t.common.saving : task ? td.saveChanges : td.create}
          </Button>
        </div>
      </div>
    </form>
  );
}
