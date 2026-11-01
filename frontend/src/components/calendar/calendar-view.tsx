"use client";

import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  TouchSensor,
  useDraggable,
  useDroppable,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import { ChevronLeft, ChevronRight, Repeat } from "lucide-react";
import Link from "next/link";
import { useEffect, useMemo, useState, useTransition } from "react";
import { moveDueAction } from "@/actions/tasks";
import { useTaskDialog } from "@/components/tasks/task-dialog";
import { addDays, formatDateKey, monthStart } from "@/core/calendar";
import type { PriorityValue } from "@/core/priority";
import { formatInTz, fromLocal, localParts } from "@/core/time";
import { useI18n } from "@/i18n/provider";
import { cn } from "@/lib/cn";
import type { Task } from "@/lib/types";

type View = "month" | "week";

/** Viền trái của thẻ task theo mức ưu tiên. */
const PRIORITY_BORDER: Record<PriorityValue, string> = {
  URGENT: "border-l-urgent",
  HIGH: "border-l-warning",
  MEDIUM: "border-l-calm",
  LOW: "border-l-muted",
};

/** Số task hiện trong một ô ở chế độ tháng; còn lại bấm "+N" để xem tuần đó. */
const MONTH_CELL_LIMIT = 3;

export function CalendarView({
  view,
  anchor,
  days,
  today,
  tasks,
  tz,
}: {
  view: View;
  anchor: string;
  days: string[];
  today: string;
  tasks: Task[];
  tz: string;
}) {
  const { locale, t } = useI18n();
  const c = t.calendar;
  const { openEdit, openNew } = useTaskDialog();
  const [items, setItems] = useState(tasks);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [, startTransition] = useTransition();

  // Dữ liệu mới từ server (sau khi lưu / chuyển tháng) thay cho bản lạc quan
  // eslint-disable-next-line react-hooks/set-state-in-effect -- đồng bộ props → state cục bộ cho kéo thả
  useEffect(() => setItems(tasks), [tasks]);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 200, tolerance: 6 } }),
    useSensor(KeyboardSensor),
  );

  const byDay = useMemo(() => {
    const map = new Map<string, Task[]>();
    for (const task of items) {
      if (!task.dueAt) continue;
      const key = localParts(task.dueAt, tz).dateKey;
      map.set(key, [...(map.get(key) ?? []), task]);
    }
    for (const list of map.values()) list.sort((a, b) => a.dueAt!.getTime() - b.dueAt!.getTime());
    return map;
  }, [items, tz]);

  function onDragEnd(e: DragEndEvent) {
    setActiveId(null);
    const task = items.find((x) => x.id === e.active.id);
    const target = e.over?.id;
    if (!task?.dueAt || typeof target !== "string" || localParts(task.dueAt, tz).dateKey === target) return;
    const time = formatInTz(task.dueAt, tz, "HH:mm", locale);
    const previous = items;
    setItems((all) => all.map((x) => (x.id === task.id ? { ...x, dueAt: fromLocal(target, time, tz) } : x)));
    startTransition(async () => {
      const res = await moveDueAction(task.id, `${target}T${time}`).catch(() => ({ ok: false as const, error: c.moveFailed }));
      if (!res.ok) {
        setItems(previous);
        setError(res.error);
      }
    });
  }

  const href = (v: View, date: string) => `/calendar?view=${v}&date=${date}`;
  const prev = view === "month" ? monthStart(anchor, -1) : addDays(anchor, -7);
  const next = view === "month" ? monthStart(anchor, 1) : addDays(anchor, 7);
  const title =
    view === "month"
      ? formatDateKey(anchor, locale, { month: "long", year: "numeric" })
      : `${formatDateKey(days[0], locale, { day: "numeric", month: "short" })} – ${formatDateKey(days[6], locale, {
          day: "numeric",
          month: "short",
          year: "numeric",
        })}`;
  const anchorMonth = anchor.slice(0, 7);
  const active = activeId ? items.find((x) => x.id === activeId) : null;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Link href={href(view, prev)} aria-label={c.previous} className="flex h-9 w-9 items-center justify-center rounded-full border border-line-strong hover:bg-hover">
            <ChevronLeft size={18} />
          </Link>
          <Link href={href(view, next)} aria-label={c.next} className="flex h-9 w-9 items-center justify-center rounded-full border border-line-strong hover:bg-hover">
            <ChevronRight size={18} />
          </Link>
          <Link href={href(view, today)} className="rounded-full border border-line-strong px-4 py-2 text-sm font-semibold hover:bg-hover">
            {c.today}
          </Link>
          <h1 className="ml-2 text-xl font-bold first-letter:uppercase">{title}</h1>
        </div>
        <div className="flex rounded-full bg-hover p-1">
          {(["month", "week"] as const).map((v) => (
            <Link
              key={v}
              href={href(v, anchor)}
              aria-current={v === view ? "page" : undefined}
              className={cn(
                "rounded-full px-4 py-1.5 text-sm font-semibold transition",
                v === view ? "bg-card text-fg shadow-sm" : "text-fg-2 hover:text-fg",
              )}
            >
              {v === "month" ? c.month : c.week}
            </Link>
          ))}
        </div>
      </div>

      <p className="text-xs text-fg-2">{c.hint}</p>
      {error && (
        <p role="alert" className="rounded-2xl bg-danger/10 px-4 py-3 text-sm font-medium text-danger">
          {error}
        </p>
      )}

      {/* id cố định → aria id của dnd-kit giống nhau giữa server và client (tránh lỗi hydration) */}
      <DndContext id="calendar" sensors={sensors} onDragStart={(e) => setActiveId(String(e.active.id))} onDragEnd={onDragEnd} onDragCancel={() => setActiveId(null)}>
        <div className="overflow-x-auto rounded-2xl border border-line bg-card">
          <div className="grid min-w-[44rem] grid-cols-7">
            {t.recurrence.dayNames.map((name) => (
              <div key={name} className="border-b border-line px-3 py-2 text-xs font-semibold text-fg-2">
                {name}
              </div>
            ))}
            {days.map((key) => {
              const list = byDay.get(key) ?? [];
              const shown = view === "month" ? list.slice(0, MONTH_CELL_LIMIT) : list;
              return (
                <DayCell
                  key={key}
                  dateKey={key}
                  label={formatDateKey(key, locale, { day: "numeric" })}
                  isToday={key === today}
                  dimmed={view === "month" && key.slice(0, 7) !== anchorMonth}
                  tall={view === "week"}
                  onAdd={() => openNew("TODO", `${key}T17:00`)}
                >
                  {shown.map((task) => (
                    <DraggableChip key={task.id} task={task} hidden={task.id === activeId}>
                      <Chip task={task} tz={tz} onOpen={() => openEdit(task)} />
                    </DraggableChip>
                  ))}
                  {list.length > shown.length && (
                    <Link href={href("week", key)} className="block px-1 text-xs font-semibold text-fg-2 hover:text-fg">
                      {c.more(list.length - shown.length)}
                    </Link>
                  )}
                </DayCell>
              );
            })}
          </div>
        </div>
        <DragOverlay>{active ? <Chip task={active} tz={tz} onOpen={() => {}} dragging /> : null}</DragOverlay>
      </DndContext>
    </div>
  );
}

function DayCell({
  dateKey,
  label,
  isToday,
  dimmed,
  tall,
  onAdd,
  children,
}: {
  dateKey: string;
  label: string;
  isToday: boolean;
  dimmed: boolean;
  tall: boolean;
  onAdd: () => void;
  children: React.ReactNode;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: dateKey });
  return (
    <div
      ref={setNodeRef}
      data-date={dateKey}
      onClick={(e) => {
        // Bấm vào chỗ trống của ô (không phải thẻ task) → thêm task cho ngày này
        if (e.target === e.currentTarget) onAdd();
      }}
      className={cn(
        "min-w-0 cursor-pointer space-y-1 border-b border-l border-line p-1.5 transition [&:nth-child(7n+1)]:border-l-0",
        tall ? "min-h-[28rem]" : "min-h-28",
        dimmed && "bg-hover/40",
        isOver && "bg-brand/10",
      )}
    >
      <div
        onClick={onAdd}
        className={cn(
          "flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold",
          isToday ? "bg-primary text-on-primary" : dimmed ? "text-fg-2/60" : "text-fg-2",
        )}
      >
        {label}
      </div>
      {children}
    </div>
  );
}

function DraggableChip({ task, hidden, children }: { task: Task; hidden: boolean; children: React.ReactNode }) {
  const { attributes, listeners, setNodeRef } = useDraggable({ id: task.id });
  return (
    <div ref={setNodeRef} {...listeners} {...attributes} className={cn("touch-manipulation", hidden && "opacity-30")}>
      {children}
    </div>
  );
}

function Chip({ task, tz, onOpen, dragging }: { task: Task; tz: string; onOpen: () => void; dragging?: boolean }) {
  const { locale } = useI18n();
  const done = task.status === "DONE";
  return (
    <button
      type="button"
      onClick={(e) => {
        e.stopPropagation();
        onOpen();
      }}
      title={task.title}
      className={cn(
        "flex w-full min-w-0 items-center gap-1.5 rounded-md border-l-[3px] bg-hover px-1.5 py-1 text-left text-xs transition hover:bg-line",
        PRIORITY_BORDER[task.priority],
        done && "opacity-60",
        dragging && "rotate-1 bg-card shadow-lg ring-1 ring-line-strong",
      )}
    >
      {task.dueAt && <span className="shrink-0 font-semibold text-fg-2">{formatInTz(task.dueAt, tz, "HH:mm", locale)}</span>}
      <span className={cn("truncate font-medium", done && "line-through")}>{task.title}</span>
      {task.recurrence && <Repeat size={10} className="shrink-0 text-fg-2" />}
    </button>
  );
}
