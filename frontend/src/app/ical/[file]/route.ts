import { API_URL } from "@/server/api";

export const dynamic = "force-dynamic";

/**
 * Link lịch iCal: /ical/<token>.ics → API /api/calendar/feed/<token>. API không mở ra ngoài nên web chuyển tiếp;
 * ứng dụng lịch (Google, Apple…) tải link này không có phiên đăng nhập — token bí mật trong URL thay cho phiên.
 */
export async function GET(_request: Request, ctx: RouteContext<"/ical/[file]">) {
  const { file } = await ctx.params;
  if (!file.endsWith(".ics")) return new Response("Not found", { status: 404 });
  const token = file.slice(0, -".ics".length);

  const res = await fetch(`${API_URL}/api/calendar/feed/${encodeURIComponent(token)}`, { cache: "no-store" });
  if (!res.ok) return new Response("Not found", { status: 404 });
  return new Response(await res.text(), {
    headers: {
      "content-type": "text/calendar; charset=utf-8",
      "cache-control": "no-store",
      "x-robots-tag": "noindex",
    },
  });
}
