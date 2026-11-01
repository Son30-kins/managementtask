"use server";

import { revalidatePath } from "next/cache";
import type { PriorityValue, StatusValue } from "@/core/priority";
import type { Recurrence } from "@/lib/types";
import { apiSend, type ApiResult } from "@/server/api";
import { listTasks } from "@/server/queries";

export type ActionResult = { ok: true } | { ok: false; error: string };

export type TaskFormData = {
  title: string;
  description?: string;
  due?: string; // "YYYY-MM-DDTHH:mm" theo giờ của người dùng, hoặc "" — API tự đổi theo múi giờ
  priority: PriorityValue;
  status: StatusValue;
  tags?: string[];
  subtasks?: { title: string; done: boolean }[];
  recurrence?: Recurrence | null;
};

function toBody(data: TaskFormData) {
  return {
    title: data.title,
    description: data.description ?? "",
    due: data.due ? data.due.slice(0, 16) : null,
    priority: data.priority,
    status: data.status,
    tags: (data.tags ?? []).map((t) => t.trim()).filter(Boolean),
    subtasks: (data.subtasks ?? []).filter((s) => s.title.trim()),
    recurrence: data.recurrence ?? null,
  };
}

function done(res: ApiResult<unknown>): ActionResult {
  if (!res.ok) return { ok: false, error: res.error.message };
  revalidatePath("/", "layout");
  return { ok: true };
}

export async function createTaskAction(data: TaskFormData): Promise<ActionResult> {
  return done(await apiSend("POST", "/api/tasks", toBody(data)));
}

export async function updateTaskAction(taskId: string, data: TaskFormData): Promise<ActionResult> {
  return done(await apiSend("PUT", `/api/tasks/${encodeURIComponent(taskId)}`, toBody(data)));
}

export async function moveTaskAction(taskId: string, status: string): Promise<ActionResult> {
  return done(await apiSend("PATCH", `/api/tasks/${encodeURIComponent(taskId)}/status`, { status }));
}

/** Kéo task sang ngày khác trên Lịch: due = "YYYY-MM-DDTHH:mm" theo giờ của người dùng. */
export async function moveDueAction(taskId: string, due: string): Promise<ActionResult> {
  return done(await apiSend("PATCH", `/api/tasks/${encodeURIComponent(taskId)}/due`, { due }));
}

export async function deleteTaskAction(taskId: string): Promise<ActionResult> {
  return done(await apiSend("DELETE", `/api/tasks/${encodeURIComponent(taskId)}`));
}

export async function toggleSubtaskAction(subtaskId: string, isDone: boolean): Promise<ActionResult> {
  return done(await apiSend("PATCH", `/api/subtasks/${encodeURIComponent(subtaskId)}`, { done: isDone }));
}

/** Việc để mèo nhắc: task quá hạn (ưu tiên cao nhất trước) hoặc task đến hạn trong 24 giờ tới. */
export type CatReminder = {
  overdueCount: number;
  task: { id: string; title: string; dueAt: string; overdue: boolean } | null;
};

export async function catReminderAction(): Promise<CatReminder> {
  const [overdue, week] = await Promise.all([listTasks({ view: "overdue" }), listTasks({ view: "week" })]);
  const now = Date.now();
  const soon = week.find((t) => t.dueAt && t.dueAt.getTime() >= now && t.dueAt.getTime() - now < 24 * 3_600_000);
  const pick = overdue[0] ?? soon;
  return {
    overdueCount: overdue.length,
    task: pick?.dueAt
      ? { id: pick.id, title: pick.title, dueAt: pick.dueAt.toISOString(), overdue: pick.dueAt.getTime() < now }
      : null,
  };
}
