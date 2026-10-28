package com.dsmod.probe;

import java.util.ArrayDeque;

/**
 * Paces the hidden tool-result turns the Agent sends back to DeepSeek.
 *
 * Every Agent step is a real chat request on the user's account. Firing them back to back,
 * dozens per minute, looks nothing like a person typing and is the most likely reason accounts
 * using the Agent were suspended. This limiter does not hide anything; it only keeps the Agent
 * at a pace a normal client would produce:
 *  - at least MIN_GAP_MS between two Agent turns;
 *  - at most PER_MINUTE turns in any rolling minute;
 *  - at most PER_HOUR turns in any rolling hour.
 * When a limit is reached the pending result simply waits; nothing is dropped.
 */
final class AgentRateLimiter {
    private AgentRateLimiter() {}

    static final long MIN_GAP_MS = 8_000L;
    static final int PER_MINUTE = 5;
    static final int PER_HOUR = 60;

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 3_600_000L;
    private static final ArrayDeque<Long> SENT = new ArrayDeque<Long>();
    private static long lastHourWarn;

    /** Milliseconds to wait before the next Agent turn may be sent; 0 means send now. */
    static synchronized long waitMs(long now) {
        while (!SENT.isEmpty() && now - SENT.peekFirst() >= HOUR) SENT.pollFirst();
        if (SENT.isEmpty()) return 0L;
        long wait = Math.max(0L, SENT.peekLast() + MIN_GAP_MS - now);
        int inMinute = 0;
        Long oldestInMinute = null;
        for (Long t : SENT) {
            if (now - t < MINUTE) {
                inMinute++;
                if (oldestInMinute == null) oldestInMinute = t;
            }
        }
        if (inMinute >= PER_MINUTE && oldestInMinute != null) {
            wait = Math.max(wait, oldestInMinute + MINUTE - now);
        }
        if (SENT.size() >= PER_HOUR) {
            wait = Math.max(wait, SENT.peekFirst() + HOUR - now);
        }
        return wait;
    }

    static synchronized void recordSent(long now) {
        SENT.addLast(Long.valueOf(now));
    }

    /** True at most once every ten minutes, so the hourly-cap toast does not repeat. */
    static synchronized boolean shouldWarnHourCap(long now) {
        if (SENT.size() < PER_HOUR || now - lastHourWarn < 600_000L) return false;
        lastHourWarn = now;
        return true;
    }
}
