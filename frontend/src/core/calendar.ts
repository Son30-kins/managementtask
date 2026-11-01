// Ngày trên lịch dạng "YYYY-MM-DD" (ngày địa phương, không kèm giờ). Tính bằng UTC để không lệch theo múi giờ máy chủ.

const DATE_KEY = /^\d{4}-\d{2}-\d{2}$/;

export function isDateKey(value: unknown): value is string {
  return typeof value === "string" && DATE_KEY.test(value) && !Number.isNaN(Date.parse(`${value}T00:00:00Z`));
}

function toUtc(key: string): Date {
  const [y, m, d] = key.split("-").map(Number);
  return new Date(Date.UTC(y, m - 1, d));
}

function fromUtc(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`;
}

export function addDays(key: string, days: number): string {
  const d = toUtc(key);
  d.setUTCDate(d.getUTCDate() + days);
  return fromUtc(d);
}

/** Ngày 1 của tháng chứa `key`, cộng `months` tháng. */
export function monthStart(key: string, months = 0): string {
  const d = toUtc(key);
  return fromUtc(new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + months, 1)));
}

/** Thứ 2 của tuần chứa `key`. */
export function startOfWeek(key: string): string {
  return addDays(key, -((toUtc(key).getUTCDay() + 6) % 7));
}

/** 7 ngày (thứ 2 → chủ nhật) của tuần chứa `key`. */
export function weekDays(key: string): string[] {
  const start = startOfWeek(key);
  return Array.from({ length: 7 }, (_, i) => addDays(start, i));
}

/** Lưới tháng 6 tuần (42 ngày, bắt đầu thứ 2) chứa tháng của `key`. */
export function monthGrid(key: string): string[] {
  const start = startOfWeek(monthStart(key));
  return Array.from({ length: 42 }, (_, i) => addDays(start, i));
}

/** Định dạng ngày theo ngôn ngữ, vd. { month: "long", year: "numeric" } → "tháng 11 năm 2026". */
export function formatDateKey(key: string, locale: string, options: Intl.DateTimeFormatOptions): string {
  return new Intl.DateTimeFormat(locale, { ...options, timeZone: "UTC" }).format(toUtc(key));
}
