"use client";

import { useEffect, useRef, useState, type ComponentPropsWithoutRef } from "react";
import { cn } from "@/components/lib/utils.js";
import { formatRunningWorkDuration } from "@/lib/workDuration.js";
import { useMaridIntl } from "@/i18n/IntlProvider.js";

export interface ChatLoadingProps extends ComponentPropsWithoutRef<"div"> {
  loading: boolean;
  initialElapsedMs?: number;
  size?: "default" | "sm";
}

// Bars mirror the launcher mark geometry; phase-offset opacity keeps the pair
// reading as breathing eyes instead of a synchronized blink.
export function MaridBarsLoader() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 20 20"
      className="size-5 shrink-0 overflow-visible text-brand"
    >
      <g fill="currentColor">
        <rect className="marid-loader-bar" x="4.7" y="3.5" width="2.6" height="13" rx="1.3" />
        <rect
          className="marid-loader-bar marid-loader-bar-offset"
          x="12.7"
          y="3.5"
          width="2.6"
          height="13"
          rx="1.3"
        />
      </g>
    </svg>
  );
}

export function ChatLoading({
  loading,
  initialElapsedMs = 0,
  className,
  size: _size,
  ...props
}: ChatLoadingProps) {
  const { intl, locale } = useMaridIntl();
  const [elapsedMs, setElapsedMs] = useState(initialElapsedMs);
  const baselineRef = useRef<{ elapsedMs: number; startedAt: number } | null>(null);
  useEffect(() => {
    if (!loading) {
      baselineRef.current = null;
      return;
    }
    const now = performance.now();
    const previous = baselineRef.current;
    baselineRef.current = {
      elapsedMs: Math.max(
        initialElapsedMs,
        previous ? previous.elapsedMs + now - previous.startedAt : 0,
      ),
      startedAt: now,
    };
  }, [loading, initialElapsedMs]);
  useEffect(() => {
    if (!loading) return;
    // 上游耗时频繁变化曾反复取消计时器，导致回调饿死；校准基准与刷新生命周期分离。
    const tick = () => {
      const baseline = baselineRef.current;
      if (baseline) setElapsedMs(baseline.elapsedMs + performance.now() - baseline.startedAt);
    };
    tick();
    const timer = window.setInterval(tick, 1000);
    return () => window.clearInterval(timer);
  }, [loading]);
  if (!loading) return null;
  return (
    <div
      role="status"
      aria-label={intl.formatMessage({ id: "common.loading" })}
      data-marid-chat-loading-animate="true"
      {...props}
      className={cn("inline-flex w-fit items-center gap-2", className)}
    >
      {/* 与摘要保持相同的 16px 图标槽，替换动效时不改变文字对齐。 */}
      <span data-chat-loading-icon className="flex size-4 shrink-0 items-center justify-center">
        <MaridBarsLoader />
      </span>
      <span className="animated-gradient-text animated-gradient-text-foreground text-ui-base font-normal motion-reduce:animate-none!">
        {intl.formatMessage({ id: "chat.loading.working" })}
      </span>
      <span
        aria-hidden="true"
        className="font-mono text-ui-caption tabular-nums text-foreground-subtle"
      >
        {formatRunningWorkDuration(
          elapsedMs,
          (descriptor) => intl.formatMessage(descriptor),
          locale,
        )}
      </span>
    </div>
  );
}
