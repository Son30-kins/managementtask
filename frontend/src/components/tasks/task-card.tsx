"use client";

import { BellRing, Ellipsis, ListChecks } from "lucide-react";
import { useOptimistic, useTransition } from "react";
import { toggleSubtaskAction } from "@/actions/tasks";
import { useI18n } from "@/i18n/provider";
import { cn } from "@/lib/cn";
import { RecurrenceBadge } from "./recurrence";
import { DueChip, PriorityBadge, ProgressBar, dueTone, type TaskDTO } from "./task-bits";

/** Số task con hiện trên thẻ; còn lại xem trong chi tiết. */
const VISIBLE_SUBTASKS = 5;

export function TaskCard({
  task,
  tz,
  now,
  onOpen,
  dragging,
}: {
  task: TaskDTO;
  tz: string;
  now: number;
  onOpen: () => void;
  dragging?: boolean;
}) {
  const { t } = useI18n();
  const [, startTransition] = useTransition();
  // Tick ngay trên thẻ: hiện kết quả trước, server lưu xong thì dữ liệu mới thay vào (lỗi → tự quay lại)
  const [subtasks, setSubtaskDone] = useOptimistic(task.subtasks, (all, change: { id: string; done: boolean }) =>
    all.map((s) => (s.id === change.id ? { ...s, done: change.done } : s)),
  );
  const done = subtasks.filter((s) => s.done).length;
  const total = subtasks.length;

  function toggle(id: string, isDone: boolean) {
    startTransition(async () => {
      setSubtaskDone({ id, done: isDone });
      try {
        await toggleSubtaskAction(id, isDone);
      } catch {
        // mất mạng giữa chừng → ô tick tự quay lại trạng thái cũ khi transition kết thúc
      }
    });
  }
  const subtitle = task.templateName ?? task.tags[0] ?? null;
  const tone = dueTone(task, now);

  return (
    // Bấm vào bất kỳ chỗ nào trên thẻ để mở chi tiết (kéo > 6px mới tính là kéo thả, xem PointerSensor ở board)
    <article
      onClick={dragging ? undefined : onOpen}
      className={cn(
        "group cursor-pointer space-y-4 rounded-2xl border border-line bg-card p-5 transition",
        dragging ? "rotate-1 shadow-2xl ring-1 ring-line-strong" : "hover:border-line-strong hover:shadow-sm",
        task.status === "DONE" && "opacity-80",
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onOpen();
          }}
          className="min-w-0 flex-1 text-left"
        >
          <h3 className={cn("text-base font-bold leading-snug", task.status === "DONE" && "line-through decoration-fg-2/60")}>{task.title}</h3>
          {subtitle && <p className="mt-1 truncate text-sm text-fg-2">{subtitle}</p>}
        </button>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onOpen();
          }}
          aria-label={t.task.openDetails}
          className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-line hover:bg-hover"
        >
          <Ellipsis size={16} />
        </button>
      </div>

      <div className="flex items-center justify-between gap-2">
        <PriorityBadge priority={task.priority} />
        {tone === "soon" && (
          <span className="inline-flex items-center gap-1 text-xs font-medium text-warning">
            <BellRing size={13} /> {t.task.dueSoon}
          </span>
        )}
        {tone === "overdue" && <span className="text-xs font-semibold text-danger">{t.task.overdue}</span>}
      </div>

      {total > 0 && (
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm">
            <span className="flex items-center gap-2 text-fg-2">
              <ListChecks size={15} className="text-fg" /> {t.task.progress}
            </span>
            <span className="font-semibold">
              {done}/{total}
            </span>
          </div>
          <ProgressBar done={done} total={total} />
          <ul className="space-y-1 pt-1">
            {subtasks.slice(0, VISIBLE_SUBTASKS).map((s) => (
              <li key={s.id}>
                {/* Tick task con không mở chi tiết và không bắt đầu kéo thẻ */}
                <label
                  onClick={(e) => e.stopPropagation()}
                  onPointerDown={(e) => e.stopPropagation()}
                  onKeyDown={(e) => e.stopPropagation()}
                  className="-mx-2 flex cursor-pointer items-start gap-2.5 rounded-lg px-2 py-1 text-sm hover:bg-hover"
                >
                  <input
                    type="checkbox"
                    checked={s.done}
                    disabled={dragging}
                    onChange={(e) => toggle(s.id, e.target.checked)}
                    aria-label={t.taskDialog.subtaskDone(s.title)}
                    className="mt-0.5 h-4 w-4 shrink-0 cursor-pointer accent-[var(--color-brand)]"
                  />
                  <span className={cn("min-w-0 break-words", s.done && "text-fg-2 line-through")}>{s.title}</span>
                </label>
              </li>
            ))}
          </ul>
          {total > VISIBLE_SUBTASKS && <p className="text-xs font-medium text-fg-2">{t.task.moreSubtasks(total - VISIBLE_SUBTASKS)}</p>}
        </div>
      )}

      <div className="flex items-center justify-between gap-2">
        <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
          <DueChip task={task} tz={tz} now={now} />
          <RecurrenceBadge recurrence={task.recurrence} />
        </div>
        {task.tags.length > 0 && (
          <span className="truncate text-xs text-fg-2" title={task.tags.join(", ")}>
            #{task.tags.slice(0, 2).join(" #")}
          </span>
        )}
      </div>
    </article>
  );
}
