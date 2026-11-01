import { z } from "zod";
import type { Messages } from "@/i18n";

const hhmm = z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/);
const offsets = z.array(z.number().int().min(1).max(60 * 24 * 30)).max(10);

export const userSettingsSchema = z.object({
  /** Nhắc trước hạn bao nhiêu phút, theo từng mức ưu tiên */
  reminderOffsets: z.object({
    URGENT: offsets,
    HIGH: offsets,
    MEDIUM: offsets,
    LOW: offsets,
  }),
  quietHours: z.object({ enabled: z.boolean(), start: hhmm, end: hhmm }),
  digest: z.object({ enabled: z.boolean(), time: hhmm }),
  overdueRepeatHours: z.number().int().min(1).max(24 * 7),
  /** Báo cáo tuần sáng thứ 2 (thiếu = bật — cài đặt lưu trước khi có tính năng này) */
  weeklyReport: z.boolean().optional(),
});

export type UserSettings = z.infer<typeof userSettingsSchema>;

export const DEFAULT_USER_SETTINGS: UserSettings = {
  reminderOffsets: {
    URGENT: [1440, 180, 60, 15],
    HIGH: [1440, 120],
    MEDIUM: [1440],
    LOW: [],
  },
  quietHours: { enabled: true, start: "22:00", end: "07:00" },
  digest: { enabled: true, time: "08:00" },
  overdueRepeatHours: 24,
  weeklyReport: true,
};

/** Đọc settings từ DB; thiếu hoặc sai thì dùng mặc định. */
export function resolveUserSettings(raw: unknown): UserSettings {
  const parsed = userSettingsSchema.safeParse(raw);
  return parsed.success ? parsed.data : DEFAULT_USER_SETTINGS;
}

/** Các mốc nhắc có sẵn để chọn trong trang Cài đặt (phút). */
export const REMINDER_OFFSET_CHOICES = [10080, 4320, 2880, 1440, 720, 360, 180, 120, 60, 30, 15, 5] as const;

/** 10080 → "1 tuần" / "1 week" / "1 Woche" (theo t.offsets). */
export function formatOffset(minutes: number, t: Messages["offsets"]): string {
  if (minutes % 10080 === 0) return t.weeks(minutes / 10080);
  if (minutes % 1440 === 0) return t.days(minutes / 1440);
  if (minutes % 60 === 0) return t.hours(minutes / 60);
  return t.minutes(minutes);
}
