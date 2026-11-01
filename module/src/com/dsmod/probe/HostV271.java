/*
 * Deekseep - LSPosed module for the DeepSeek Android app.
 * Copyright (C) the Deekseep authors (original author: lllucccian).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package com.dsmod.probe;

import com.dsmod.probe.LegacyXposedModule.Chain;
import com.dsmod.probe.LegacyXposedModule.Hooker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Whitelisted hook set for mainland DeepSeek 2.7.1 (versionCode 287).
 *
 * <p>The 2.7.1 port was done without access to the mainland 2.4.1 APK, so every symbol used here
 * was located in 2.7.1 through a stable anchor (kotlinx.serialization serial names, analytics
 * event strings, exact constructor/method signatures) instead of a structural diff. Features whose
 * symbols could not be located with the same confidence are intentionally NOT installed on this
 * generation; {@link HostCompat#name(String)} returns an unloadable sentinel for them so that any
 * leftover call path fails closed.</p>
 *
 * <p>Verified 2.7.1 symbols:</p>
 * <ul>
 *   <li>{@code gt0.Q(String,String,Boolean,Boolean,Boolean,int)} - static "page_show" analytics
 *       dispatcher; the settings screen reports page "settings" from a LaunchedEffect.</li>
 *   <li>{@code sx1} - ChatFullCompletionRequest (serializer {@code qx1}), marker interface
 *       {@code du1}; real constructor keeps the classic 11-argument shape, prompt at index 2.</li>
 *   <li>{@code ey1} - ChatHistoryMessagesResponseBizData (serializer {@code cy1}):
 *       a=ServerChatSession(un9), b=List&lt;AppStaticMessage&gt;, c=cache_control,
 *       d=cache_reset_at.</li>
 *   <li>{@code gy} - AppStaticMessage (serializer {@code dy}); fragments list is field {@code v}.
 *       Fragments keep a=type, b=id, c=content; RequestFragment {@code v9a} has an
 *       (int,String) constructor.</li>
 * </ul>
 */
final class HostV271 {
    private static final AtomicBoolean ENTRY_HOOKED = new AtomicBoolean();
    private static final AtomicBoolean HISTORY_HOOKED = new AtomicBoolean();
    /** True when the settings page hook could not be installed; the entry is then always shown. */
    static volatile boolean entryFallback;

    static final String HISTORY_RESPONSE = "ey1";
    static final String STATIC_MESSAGE = "gy";
    static final String STATIC_MESSAGE_FRAGMENTS_FIELD = "v";

    private HostV271() {}

    static void installFeatureHooks(ClassLoader cl) {
        try { installSettingsEntry(cl); }
        catch (Throwable t) { Main.log("v271 settings entry wiring failed: " + t); }
        try { HookChatPipeline.INSTANCE.hookChatRequest(cl); }
        catch (Throwable t) { Main.log("v271 chat request wiring failed: " + t); }
        try { installHistorySanitizer(cl); }
        catch (Throwable t) { Main.log("v271 history sanitizer wiring failed: " + t); }
        Main.log("v271 whitelist installed: entryFallback=" + entryFallback);
    }

    /** Module entry: show the floating Deekseep button while DeepSeek's settings page is open. */
    static void installSettingsEntry(ClassLoader cl) {
        if (!ENTRY_HOOKED.compareAndSet(false, true)) return;
        Method method = HostCompat.settingsEntryMethod(cl);
        if (method == null) {
            entryFallback = true;
            Main.log("v271 page_show dispatcher not found; using always-visible entry");
            postShowButton();
            return;
        }
        Main.MODULE.hook(method).intercept(new Hooker() {
            @Override public Object intercept(Chain chain) throws Throwable {
                Object page = null;
                try { page = chain.getArg(0); } catch (Throwable ignored) {}
                Object result = chain.proceed();
                final boolean settings = "settings".equals(page);
                final boolean known = page instanceof String;
                try {
                    final Main module = Main.MODULE;
                    if (module != null && module.main != null && known) {
                        module.main.post(new Runnable() {
                            @Override public void run() {
                                if (settings) module.showButton();
                                else module.hideButton();
                            }
                        });
                    }
                } catch (Throwable ignored) {}
                return result;
            }
        });
        Main.log("v271 hooked page_show entry " + HostRemap.className(method.getDeclaringClass())
                + "." + HostRemap.name(method));
    }

    static void postShowButton() {
        final Main module = Main.MODULE;
        if (module == null || module.main == null) return;
        module.main.post(new Runnable() {
            @Override public void run() { module.showButton(); }
        });
    }

    /**
     * Removes the module's private {@code <system>...</system>} prompt wrapper from REQUEST
     * fragments of server history before DeepSeek renders or persists it, so an injected custom
     * system prompt never shows up inside the user's own bubbles after a history sync.
     */
    static void installHistorySanitizer(ClassLoader cl) throws Throwable {
        if (!HISTORY_HOOKED.compareAndSet(false, true)) return;
        Class<?> response = HostRemap.load(cl, HISTORY_RESPONSE);
        int n = 0;
        for (Constructor<?> ctor : response.getDeclaredConstructors()) {
            Main.MODULE.hook(ctor).intercept(new Hooker() {
                @Override public Object intercept(Chain chain) throws Throwable {
                    Object r = chain.proceed();
                    try {
                        int cleaned = sanitizeHistory(chain.getThisObject());
                        if (cleaned > 0) Main.log("v271 history prompts cleaned=" + cleaned);
                    } catch (Throwable t) {
                        Main.log("v271 history sanitize err: " + t);
                    }
                    return r;
                }
            });
            n++;
        }
        Main.log("v271 installed history sanitizer " + HISTORY_RESPONSE + " ctor x" + n);
    }

    static int sanitizeHistory(Object response) {
        Object messages = field(response, "b");
        if (!(messages instanceof List)) return 0;
        int cleaned = 0;
        for (Object message : (List<?>) messages) {
            if (message == null
                    || !STATIC_MESSAGE.equals(HostRemap.simpleName(message.getClass()))) continue;
            Object fragments = field(message, STATIC_MESSAGE_FRAGMENTS_FIELD);
            if (!(fragments instanceof List)) continue;
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) fragments;
            for (int i = 0; i < list.size(); i++) {
                Object fragment = list.get(i);
                if (fragment == null) continue;
                if (!"REQUEST".equals(String.valueOf(field(fragment, "a")))) continue;
                Object content = field(fragment, "c");
                if (!(content instanceof String)) continue;
                String text = (String) content;
                String safe = HistoryBridge.stripInjectedSystemPrompts(text);
                if (safe.equals(text)) continue;
                if (setField(fragment, "c", safe)) {
                    cleaned++;
                    continue;
                }
                Object id = field(fragment, "b");
                if (!(id instanceof Integer)) continue;
                try {
                    Constructor<?> copy = fragment.getClass()
                            .getDeclaredConstructor(int.class, String.class);
                    copy.setAccessible(true);
                    list.set(i, copy.newInstance(((Integer) id).intValue(), safe));
                    cleaned++;
                } catch (Throwable ignored) {}
            }
        }
        return cleaned;
    }

    private static Object field(Object target, String name) {
        if (target == null) return null;
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field f = HostRemap.declaredField(type, name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static boolean setField(Object target, String name, Object value) {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field f = HostRemap.declaredField(type, name);
                f.setAccessible(true);
                f.set(target, value);
                return true;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                return false;
            }
        }
        return false;
    }
}
