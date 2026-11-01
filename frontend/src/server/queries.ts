// Đọc dữ liệu từ API cho Server Component. Chuỗi ngày ISO → Date.
import "server-only";
import type {
  AccessPolicy,
  AdminStatus,
  AdminUser,
  NotificationItem,
  Task,
  TaskCounts,
  TelegramStatus,
  Template,
  TemplateRun,
} from "@/lib/types";
import { apiGet } from "./api";

export const TASK_VIEWS = ["all", "today", "overdue", "week"] as const;
export type TaskView = (typeof TASK_VIEWS)[number];

type Json<T> = { [K in keyof T]: T[K] extends Date ? string : T[K] extends Date | null ? string | null : T[K] };

const date = (v: string) => new Date(v);
const dateOrNull = (v: string | null) => (v ? new Date(v) : null);

function toTask(t: Json<Task>): Task {
  return {
    ...t,
    dueAt: dateOrNull(t.dueAt),
    completedAt: dateOrNull(t.completedAt),
    createdAt: date(t.createdAt),
    updatedAt: date(t.updatedAt),
  };
}

export async function listTasks(opts: { view?: TaskView; q?: string } = {}): Promise<Task[]> {
  const params = new URLSearchParams();
  if (opts.view && opts.view !== "all") params.set("view", opts.view);
  if (opts.q?.trim()) params.set("q", opts.q.trim());
  const rows = await apiGet<Json<Task>[]>(`/api/tasks${params.size ? `?${params}` : ""}`);
  return (rows ?? []).map(toTask);
}

export async function getTask(id: string): Promise<Task | null> {
  if (!/^[0-9a-f-]{36}$/i.test(id)) return null;
  const row = await apiGet<Json<Task>>(`/api/tasks/${id}`);
  return row ? toTask(row) : null;
}

/** Trang Lịch: task có hạn trong các ngày [from, to] ("YYYY-MM-DD", theo múi giờ người dùng). */
export async function listCalendarTasks(from: string, to: string): Promise<Task[]> {
  const rows = await apiGet<Json<Task>[]>(`/api/tasks/calendar?from=${from}&to=${to}`);
  return (rows ?? []).map(toTask);
}

export async function getCalendarFeedStatus(): Promise<{ enabled: boolean }> {
  return (await apiGet<{ enabled: boolean }>("/api/calendar")) ?? { enabled: false };
}

export async function countTasks(): Promise<TaskCounts> {
  return (await apiGet<TaskCounts>("/api/tasks/counts")) ?? { all: 0, today: 0, overdue: 0, week: 0 };
}

type TemplateJson = Omit<Json<Template>, "schedule"> & { schedule: Json<NonNullable<Template["schedule"]>> | null };

function toTemplate(t: TemplateJson): Template {
  const s = t.schedule;
  return {
    ...t,
    createdAt: date(t.createdAt),
    updatedAt: date(t.updatedAt),
    schedule: s ? { ...s, nextRunAt: dateOrNull(s.nextRunAt), lastRunAt: dateOrNull(s.lastRunAt) } : null,
  };
}

export async function listTemplates(): Promise<Template[]> {
  return ((await apiGet<TemplateJson[]>("/api/templates")) ?? []).map(toTemplate);
}

export async function getTemplate(id: string): Promise<Template | null> {
  if (!/^[0-9a-f-]{36}$/i.test(id)) return null;
  const row = await apiGet<TemplateJson>(`/api/templates/${id}`);
  return row ? toTemplate(row) : null;
}

export async function listTemplateRuns(limit = 10): Promise<TemplateRun[]> {
  const rows = (await apiGet<Json<TemplateRun>[]>(`/api/template-runs?limit=${limit}`)) ?? [];
  return rows.map((r) => ({ ...r, createdAt: date(r.createdAt) }));
}

export async function listNotifications(): Promise<{ items: NotificationItem[]; unread: number }> {
  const inbox = await apiGet<{ items: Json<NotificationItem>[]; unread: number }>("/api/notifications");
  return {
    items: (inbox?.items ?? []).map((n) => ({ ...n, createdAt: date(n.createdAt), readAt: dateOrNull(n.readAt) })),
    unread: inbox?.unread ?? 0,
  };
}

export async function getTelegramStatus(): Promise<TelegramStatus> {
  return (await apiGet<TelegramStatus>("/api/telegram")) ?? { botConfigured: false, botUsername: null, linked: false, username: null };
}

// ===== Quản trị (API chỉ cho ROLE_ADMIN) =====

export async function listUsersForAdmin(): Promise<AdminUser[]> {
  return ((await apiGet<Json<AdminUser>[]>("/api/admin/users")) ?? []).map((u) => ({ ...u, createdAt: date(u.createdAt) }));
}

export async function getAccessPolicy(): Promise<AccessPolicy> {
  return (await apiGet<AccessPolicy>("/api/admin/access-policy")) ?? { allowedEmails: [], allowedDomains: [], registrationOpen: true };
}

export async function getAdminStatus(): Promise<AdminStatus> {
  return (await apiGet<AdminStatus>("/api/admin/status")) ?? { telegramConfigured: false, botUsername: null };
}
