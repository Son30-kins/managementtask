"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import type { TemplateInput } from "@/core/template-schema";
import { formatInTz } from "@/core/time";
import type { PlannedTask, Recurrence } from "@/lib/types";
import { apiSend } from "@/server/api";
import { getI18n } from "@/i18n/server";
import { requireUser } from "@/server/session";

export type SaveTemplateResult = { ok: false; errors: { path: string; message: string }[] } | undefined;

/** Tạo / sửa form mẫu. API kiểm tra (Bean Validation + quy tắc liên trường) và trả lỗi theo path. */
export async function saveTemplateAction(input: TemplateInput, id?: string): Promise<SaveTemplateResult> {
  const res = id
    ? await apiSend("PUT", `/api/templates/${encodeURIComponent(id)}`, input)
    : await apiSend("POST", "/api/templates", input);
  if (!res.ok) {
    const errors = res.error.errors.length ? res.error.errors : [{ path: "", message: res.error.message }];
    return { ok: false, errors };
  }
  revalidatePath("/", "layout");
  redirect("/templates");
}

export async function duplicateTemplateAction(id: string) {
  await apiSend("POST", `/api/templates/${encodeURIComponent(id)}/duplicate`);
  revalidatePath("/", "layout");
}

export async function deleteTemplateAction(id: string) {
  await apiSend("DELETE", `/api/templates/${encodeURIComponent(id)}`);
  revalidatePath("/", "layout");
}

export type PreviewItem = {
  title: string;
  description: string;
  priority: string;
  due: string | null;
  subtasks: string[];
  tags: string[];
};

export type PreviewActionResult =
  | { ok: true; tasks: PreviewItem[]; warnings: string[] }
  | { ok: false; errors: Record<string, string>; message?: string };

type Values = Record<string, string | boolean>;

export async function previewTemplateAction(templateId: string, values: Values): Promise<PreviewActionResult> {
  const [user, { locale, t }] = await Promise.all([requireUser(), getI18n()]);
  const res = await apiSend<{ tasks: PlannedTask[]; warnings: string[] }>(
    "POST",
    `/api/templates/${encodeURIComponent(templateId)}/preview`,
    { values },
  );
  if (!res.ok) return { ok: false, errors: res.error.fieldErrors, message: res.error.message };
  return {
    ok: true,
    warnings: res.data.warnings,
    tasks: res.data.tasks.map((task) => ({
      title: task.title,
      description: task.description,
      priority: task.priority,
      due: task.dueAt ? formatInTz(new Date(task.dueAt), user.timezone, t.formats.weekdayDateTime, locale) : null,
      subtasks: task.subtasks,
      tags: task.tags,
    })),
  };
}

export type RunActionResult = { ok: true; count: number } | { ok: false; errors: Record<string, string>; message?: string };

export async function runTemplateAction(templateId: string, values: Values): Promise<RunActionResult> {
  const res = await apiSend<{ count: number }>("POST", `/api/templates/${encodeURIComponent(templateId)}/runs`, {
    values,
  });
  if (!res.ok) return { ok: false, errors: res.error.fieldErrors, message: res.error.message };
  revalidatePath("/", "layout");
  return { ok: true, count: res.data.count };
}

export type ScheduleInput = {
  enabled: boolean;
  recurrence: Recurrence;
  time: string;
  values: Record<string, string | boolean>;
};

export type ScheduleActionResult = { ok: true } | { ok: false; message: string; errors: Record<string, string> };

/** Đặt / đổi lịch tự chạy. API kiểm tra giá trị form ngay và trả lỗi theo mã trường. */
export async function saveScheduleAction(templateId: string, input: ScheduleInput): Promise<ScheduleActionResult> {
  const res = await apiSend("PUT", `/api/templates/${encodeURIComponent(templateId)}/schedule`, input);
  if (!res.ok) return { ok: false, message: res.error.message, errors: res.error.fieldErrors };
  revalidatePath("/templates", "layout");
  return { ok: true };
}

export async function deleteScheduleAction(templateId: string) {
  await apiSend("DELETE", `/api/templates/${encodeURIComponent(templateId)}/schedule`);
  revalidatePath("/templates", "layout");
}
