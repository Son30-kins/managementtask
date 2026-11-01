"use client";

import { CalendarPlus, Copy, RefreshCw, Unlink } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { createCalendarLinkAction, revokeCalendarLinkAction } from "@/actions/settings";
import { Button, Card, Input, Notice } from "@/components/ui";
import { useI18n } from "@/i18n/provider";

/** Link lịch iCal: tạo / tạo lại / tắt. URL chỉ hiện ngay sau khi tạo (database chỉ giữ hash). */
export function CalendarFeedCard({ enabled }: { enabled: boolean }) {
  const { t } = useI18n();
  const c = t.ical;
  const router = useRouter();
  const [url, setUrl] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  function create() {
    if (enabled && !confirm(c.confirmRecreate)) return;
    setError(null);
    startTransition(async () => {
      const res = await createCalendarLinkAction();
      if (res.ok) {
        setUrl(res.url);
        setCopied(false);
        router.refresh();
      } else setError(res.error);
    });
  }

  function revoke() {
    if (!confirm(c.confirmRevoke)) return;
    startTransition(async () => {
      await revokeCalendarLinkAction();
      setUrl(null);
      router.refresh();
    });
  }

  async function copy() {
    if (!url) return;
    await navigator.clipboard.writeText(url).catch(() => undefined);
    setCopied(true);
  }

  return (
    <Card className="space-y-4 p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold">{c.title}</h2>
          <p className="text-sm text-fg-2">{c.hint}</p>
        </div>
        <span className={`rounded-full px-3 py-1 text-xs font-semibold ${enabled ? "bg-success/15 text-[#2f8a57] dark:text-success" : "bg-hover text-fg-2"}`}>
          {enabled ? c.on : c.off}
        </span>
      </div>

      {error && <Notice tone="danger">{error}</Notice>}

      {url && (
        <div className="space-y-2">
          <div className="flex gap-2">
            <Input readOnly value={url} onFocus={(e) => e.target.select()} className="font-mono text-xs" aria-label={c.title} />
            <Button variant="secondary" size="sm" onClick={copy} className="shrink-0">
              <Copy size={15} /> {copied ? c.copied : c.copy}
            </Button>
          </div>
          <Notice tone="warning">{c.secret}</Notice>
          <ul className="list-disc space-y-1 pl-5 text-sm text-fg-2">
            <li>{c.google}</li>
            <li>{c.apple}</li>
          </ul>
          <p className="text-xs text-fg-2">{c.localhost}</p>
        </div>
      )}

      <div className="flex flex-wrap gap-2">
        <Button size="sm" variant={enabled ? "secondary" : "primary"} onClick={create} disabled={pending}>
          {enabled ? <RefreshCw size={15} /> : <CalendarPlus size={15} />} {enabled ? c.recreate : c.create}
        </Button>
        {enabled && (
          <Button size="sm" variant="danger" onClick={revoke} disabled={pending}>
            <Unlink size={15} /> {c.revoke}
          </Button>
        )}
      </div>
    </Card>
  );
}
