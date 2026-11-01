"use client";

import { Check } from "lucide-react";
import { useTransition } from "react";
import { moveTaskAction } from "@/actions/tasks";
import { EmptyState } from "@/components/cats";
import { useI18n } from "@/i18n/provider";
import { cn } from "@/lib/cn";
import { RecurrenceBadge } from "./recurrence";
import { DueChip, PriorityBadge, type TaskDTO } from "./task-bits";
import { useTaskDialog } from "./task-dialog";

export function TaskTable({ tasks, tz, now }: { tasks: TaskDTO[]; tz: string; now: number }) {
  const { t } = useI18n();
  const { openEdit } = useTaskDialog();
  const [pending, startTransition] = useTransition();

  if (tasks.length === 0) {
    return (
      <EmptyState className="rounded-2xl border border-dashed border-line-strong px-6 py-14">
        {t.table.empty}
      </EmptyState>
    );
  }

  return (
    <div className="overflow-x-auto rounded-2xl border border-line bg-card">
      <table className="w-full min-w-[720px] text-sm">
        <thead>
          <tr className="border-b border-line text-left text-[13px] text-fg-2">
            <th className="w-12 px-4 py-3" aria-label={t.table.done} />
            <th className="px-4 py-3 font-semibold">{t.table.task}</th>
            <th className="px-4 py-3 font-semibold">{t.table.priority}</th>
            <th className="px-4 py-3 font-semibold">{t.table.due}</th>
            <th className="px-4 py-3 font-semibold">{t.table.status}</th>
            <th className="px-4 py-3 font-semibold">{t.table.progress}</th>
          </tr>
        </thead>
        <tbody>
          {tasks.map((task) => {
            const done = task.status === "DONE";
            const subDone = task.subtasks.filter((s) => s.done).length;
            return (
              <tr key={task.id} className="border-b border-line last:border-0 hover:bg-hover">
                <td className="px-4 py-3">
                  <button
                    type="button"
                    disabled={pending}
                    onClick={() => startTransition(async () => void (await moveTaskAction(task.id, done ? "TODO" : "DONE")))}
                    aria-label={done ? t.table.markUndone : t.table.markDone}
                    className={cn(
                      "flex h-6 w-6 items-center justify-center rounded-full border-2 transition",
                      done ? "border-success bg-success text-white" : "border-line-strong hover:border-success",
                    )}
                  >
                    {done && <Check size={14} strokeWidth={3} />}
                  </button>
                </td>
                <td className="px-4 py-3">
                  <button type="button" onClick={() => openEdit(task)} className="text-left">
                    <span className={cn("font-semibold", done && "text-fg-2 line-through")}>{task.title}</span>
                    {(task.templateName || task.tags.length > 0) && (
                      <span className="block text-xs text-fg-2">{task.templateName ?? `#${task.tags.join(" #")}`}</span>
                    )}
                    <RecurrenceBadge recurrence={task.recurrence} className="mt-0.5" />
                  </button>
                </td>
                <td className="px-4 py-3">
                  <PriorityBadge priority={task.priority} />
                </td>
                <td className="px-4 py-3">
                  <DueChip task={task} tz={tz} now={now} />
                </td>
                <td className="px-4 py-3 text-fg-2">{t.status[task.status]}</td>
                <td className="px-4 py-3 text-fg-2">{task.subtasks.length ? `${subDone}/${task.subtasks.length}` : "—"}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
