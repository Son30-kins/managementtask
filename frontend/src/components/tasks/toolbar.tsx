"use client";

import { CalendarDays, LayoutGrid, ListTodo, Plus, X } from "lucide-react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { useI18n } from "@/i18n/provider";
import { cn } from "@/lib/cn";
import { useTaskDialog } from "./task-dialog";

const QUICK_VIEWS = ["all", "today", "overdue", "week"] as const;
const isQuickView = (v: string | null): v is (typeof QUICK_VIEWS)[number] => QUICK_VIEWS.some((x) => x === v);

export function TasksToolbar() {
  const { t } = useI18n();
  const pathname = usePathname();
  const params = useSearchParams();
  const { openNew } = useTaskDialog();
  const query = params.toString();
  const view = params.get("view");
  const q = params.get("q");

  const tab = (href: string, icon: React.ReactNode, label: string) => (
    <Link
      href={`${href}${query ? `?${query}` : ""}`}
      aria-current={pathname === href ? "page" : undefined}
      className={cn(
        "-mb-px flex items-center gap-2 border-b-2 px-4 pb-3 text-[15px] font-semibold transition",
        pathname === href ? "border-fg text-fg" : "border-transparent text-fg-2 hover:text-fg",
      )}
    >
      {icon}
      {label}
    </Link>
  );

  return (
    <div className="mb-6 space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b border-line">
        <nav className="flex">
          {tab("/", <LayoutGrid size={18} />, t.toolbar.board)}
          {tab("/list", <ListTodo size={18} />, t.toolbar.list)}
          {tab("/calendar", <CalendarDays size={18} />, t.nav.calendar)}
        </nav>
        <div className="flex items-center gap-2 pb-3">
          <button
            type="button"
            onClick={() => openNew()}
            className="flex items-center gap-1.5 rounded-full border border-line-strong px-4 py-2.5 text-sm font-semibold hover:bg-hover"
          >
            <Plus size={16} /> {t.toolbar.newTask}
          </button>
          <Link href="/templates" className="rounded-full bg-primary px-6 py-3 text-sm font-semibold text-on-primary hover:opacity-90">
            {t.toolbar.fillTemplate}
          </Link>
        </div>
      </div>
      {/* Bộ lọc nhanh khi màn hình nhỏ (panel bên trái bị ẩn) */}
      <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 xl:hidden">
        {QUICK_VIEWS.map((v) => {
          const label = v === "all" ? t.views.allShort : t.views[v];
          const next = new URLSearchParams(params);
          next.delete("task");
          if (v === "all") next.delete("view");
          else next.set("view", v);
          const active = (view ?? "all") === v;
          return (
            <Link
              key={v}
              href={`${pathname}${next.size ? `?${next}` : ""}`}
              className={cn(
                "shrink-0 rounded-full px-4 py-2 text-sm font-medium",
                active ? "bg-primary text-on-primary" : "bg-hover text-fg-2",
              )}
            >
              {label}
            </Link>
          );
        })}
      </div>
      {(view || q) && (
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <span className="text-fg-2">{t.toolbar.filtering}</span>
          {view !== "all" && isQuickView(view) && <Chip label={t.views[view]} href={withoutParam(pathname, params, "view")} />}
          {q && <Chip label={`“${q}”`} href={withoutParam(pathname, params, "q")} />}
        </div>
      )}
    </div>
  );
}

function withoutParam(pathname: string, params: URLSearchParams, key: string) {
  const next = new URLSearchParams(params);
  next.delete(key);
  return `${pathname}${next.size ? `?${next}` : ""}`;
}

function Chip({ label, href }: { label: string; href: string }) {
  return (
    <Link href={href} className="inline-flex items-center gap-1.5 rounded-full bg-hover px-3 py-1.5 font-medium hover:bg-line">
      {label} <X size={14} />
    </Link>
  );
}
