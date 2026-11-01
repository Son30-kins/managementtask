// Kiểu dữ liệu nhận từ Workbench API (Spring Boot). Ngày giờ đã được đổi từ chuỗi ISO sang Date
// trong src/server/queries.ts — component dùng như Date bình thường.
import type { PriorityValue, StatusValue } from "@/core/priority";
import type { TemplateField, TemplateRule } from "@/core/template-schema";
import type { UserSettings } from "@/core/user-settings";
import type { Locale } from "@/i18n/config";

export type Role = "ADMIN" | "USER";

export type CurrentUser = {
  id: string;
  name: string;
  email: string;
  image: string | null;
  role: Role;
  timezone: string;
  settings: UserSettings;
  language: Locale;
};

export type Subtask = { id: string; title: string; done: boolean; position: number };

export const WEEKDAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"] as const;
export type Weekday = (typeof WEEKDAYS)[number];

/** Quy tắc lặp lại (khớp record Recurrence của API). */
export type Recurrence = {
  frequency: "DAILY" | "WEEKLY" | "MONTHLY";
  interval: number;
  weekdays: Weekday[];
  monthDay: number | null;
};

export type Task = {
  id: string;
  title: string;
  description: string;
  dueAt: Date | null;
  priority: PriorityValue;
  status: StatusValue;
  tags: string[];
  templateRunId: string | null;
  templateName: string | null;
  completedAt: Date | null;
  createdAt: Date;
  updatedAt: Date;
  subtasks: Subtask[];
  recurrence: Recurrence | null;
};

export type TaskCounts = { all: number; today: number; overdue: number; week: number };

/** Lịch tự chạy của form mẫu. Giá trị trường ngày: "+4" / "+4T17:00" (so với ngày chạy). */
export type TemplateSchedule = {
  enabled: boolean;
  recurrence: Recurrence;
  time: string;
  values: Record<string, string | boolean>;
  nextRunAt: Date | null;
  lastRunAt: Date | null;
  lastError: string | null;
};

export type Template = {
  id: string;
  name: string;
  description: string;
  fields: TemplateField[];
  rules: TemplateRule[];
  createdAt: Date;
  updatedAt: Date;
  schedule: TemplateSchedule | null;
};

export type TemplateRun = { id: string; templateId: string | null; templateName: string; taskCount: number; createdAt: Date };

export type NotificationItem = {
  id: string;
  taskId: string | null;
  title: string;
  body: string;
  readAt: Date | null;
  createdAt: Date;
};

export type TelegramStatus = { botConfigured: boolean; botUsername: string | null; linked: boolean; username: string | null };

export type AccessPolicy = { allowedEmails: string[]; allowedDomains: string[]; registrationOpen: boolean };

export type AdminUser = {
  id: string;
  name: string;
  email: string;
  image: string | null;
  role: Role;
  banned: boolean;
  createdAt: Date;
  taskCount: number;
  telegram: boolean;
};

export type AdminStatus = { telegramConfigured: boolean; botUsername: string | null };

export type PlannedTask = {
  title: string;
  description: string;
  priority: PriorityValue;
  dueAt: string | null;
  subtasks: string[];
  tags: string[];
};
