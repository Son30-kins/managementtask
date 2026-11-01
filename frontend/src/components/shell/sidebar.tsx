"use client";

import { CalendarDays, ChartColumn, FileStack, LayoutGrid, ListTodo, LogOut, Settings, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Logo, PawPrint } from "@/components/cats";
import { signOutAction } from "@/actions/settings";
import { useI18n } from "@/i18n/provider";
import type { Messages } from "@/i18n";
import { cn } from "@/lib/cn";

function navItems(t: Messages["nav"], isAdmin: boolean) {
  const items = [
    { href: "/", icon: LayoutGrid, label: t.board, short: t.boardShort },
    { href: "/list", icon: ListTodo, label: t.list, short: t.list },
    { href: "/calendar", icon: CalendarDays, label: t.calendar, short: t.calendar },
    { href: "/stats", icon: ChartColumn, label: t.stats, short: t.stats },
    { href: "/templates", icon: FileStack, label: t.templates, short: t.templates },
    { href: "/settings", icon: Settings, label: t.settings, short: t.settings },
  ];
  return isAdmin ? [...items, { href: "/admin", icon: ShieldCheck, label: t.admin, short: t.admin }] : items;
}

function useNav(isAdmin: boolean) {
  const { t } = useI18n();
  const pathname = usePathname();
  const items = navItems(t.nav, isAdmin);
  const isActive = (href: string) => (href === "/" ? pathname === "/" : pathname.startsWith(href));
  return { items, isActive, t };
}

export function useSignOut() {
  return () => signOutAction();
}

export function Sidebar({ isAdmin }: { isAdmin: boolean }) {
  const { items, isActive, t } = useNav(isAdmin);
  const signOut = useSignOut();

  return (
    <aside className="sticky top-0 hidden h-screen w-[88px] shrink-0 flex-col items-center bg-sidebar py-6 text-white md:flex">
      <div className="mb-8 flex gap-1 text-pink" aria-hidden>
        <PawPrint className="h-2.5 w-2.5" />
        <PawPrint className="h-2.5 w-2.5 opacity-60" />
        <PawPrint className="h-2.5 w-2.5 opacity-30" />
      </div>
      <Link href="/" aria-label={t.nav.home} className="mb-10 transition hover:-rotate-6">
        <Logo className="h-9 w-9 text-[#ffd9b8]" />
      </Link>
      <nav className="flex flex-1 flex-col items-center gap-3">
        {items.map(({ href, icon: Icon, label }) => (
          <Link
            key={href}
            href={href}
            title={label}
            aria-label={label}
            aria-current={isActive(href) ? "page" : undefined}
            className={cn(
              "flex h-12 w-12 items-center justify-center rounded-full transition",
              isActive(href) ? "bg-pink text-[#3b2a2f] shadow-[0_4px_14px_rgb(255_143_166/0.35)]" : "text-white/55 hover:bg-white/10 hover:text-white",
            )}
          >
            <Icon size={21} strokeWidth={1.8} />
          </Link>
        ))}
      </nav>
      <button
        type="button"
        onClick={signOut}
        title={t.nav.signOut}
        aria-label={t.nav.signOut}
        className="flex h-12 w-12 items-center justify-center rounded-full text-white/50 transition hover:text-white"
      >
        <LogOut size={21} strokeWidth={1.8} />
      </button>
    </aside>
  );
}

export function MobileNav({ isAdmin }: { isAdmin: boolean }) {
  const { items, isActive } = useNav(isAdmin);
  return (
    <nav className="fixed inset-x-0 bottom-0 z-30 flex justify-around border-t border-white/10 bg-sidebar px-2 pb-[env(safe-area-inset-bottom)] text-white md:hidden">
      {items.map(({ href, icon: Icon, label, short }) => (
        <Link
          key={href}
          href={href}
          aria-label={label}
          className={cn("flex flex-1 flex-col items-center gap-1 py-2.5 text-[11px]", isActive(href) ? "text-pink" : "text-white/55")}
        >
          <Icon size={20} strokeWidth={1.8} />
          {short}
        </Link>
      ))}
    </nav>
  );
}
