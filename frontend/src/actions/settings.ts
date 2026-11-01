"use server";

import { revalidatePath } from "next/cache";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import type { UserSettings } from "@/core/user-settings";
import { getI18n } from "@/i18n/server";
import { apiSend, SESSION_COOKIE } from "@/server/api";

export type SimpleResult = { ok: true; message?: string } | { ok: false; error: string };

export async function savePreferencesAction(input: { timezone: string; settings: UserSettings }): Promise<SimpleResult> {
  const res = await apiSend("PUT", "/api/me/preferences", input);
  if (!res.ok) return { ok: false, error: res.error.message };
  revalidatePath("/", "layout");
  return { ok: true, message: (await getI18n()).t.settings.savedRescheduled };
}

/** Tạo link t.me/<bot>?start=<mã> để người dùng bấm Start trong Telegram. */
export async function createTelegramLinkAction(): Promise<{ ok: true; url: string } | { ok: false; error: string }> {
  const res = await apiSend<{ url: string }>("POST", "/api/telegram/link");
  return res.ok ? { ok: true, url: res.data.url } : { ok: false, error: res.error.message };
}

export async function unlinkTelegramAction(): Promise<SimpleResult> {
  const res = await apiSend("DELETE", "/api/telegram/link");
  revalidatePath("/settings");
  return res.ok ? { ok: true } : { ok: false, error: res.error.message };
}

export async function sendTelegramTestAction(): Promise<SimpleResult> {
  const res = await apiSend<{ message: string }>("POST", "/api/telegram/test");
  return res.ok ? { ok: true, message: res.data.message } : { ok: false, error: res.error.message };
}

export async function markNotificationsReadAction() {
  await apiSend("POST", "/api/notifications/read-all");
  revalidatePath("/", "layout");
}

/** Đăng xuất: API huỷ phiên trong DB, Next xoá cookie trên trình duyệt. */
export async function signOutAction() {
  await apiSend("POST", "/api/auth/logout").catch(() => undefined);
  (await cookies()).delete(SESSION_COOKIE);
  redirect("/login");
}

/** Tạo link lịch iCal mới (link cũ hết hiệu lực). URL chỉ trả về đúng lần này. */
export async function createCalendarLinkAction(): Promise<{ ok: true; url: string } | { ok: false; error: string }> {
  const res = await apiSend<{ url: string }>("POST", "/api/calendar/link");
  revalidatePath("/settings");
  return res.ok ? { ok: true, url: res.data.url } : { ok: false, error: res.error.message };
}

export async function revokeCalendarLinkAction(): Promise<SimpleResult> {
  const res = await apiSend("DELETE", "/api/calendar/link");
  revalidatePath("/settings");
  return res.ok ? { ok: true } : { ok: false, error: res.error.message };
}
