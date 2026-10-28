package com.dsmod.probe;

import java.util.ArrayList;
import java.util.List;

/** JVM regression checks for the 1.8.0 port helpers (pure logic only). */
public final class PortV271RegressionTest {
    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    private static ChatEditorUi.Msg msg(String role, String body, String think) {
        ChatEditorUi.Msg m = new ChatEditorUi.Msg();
        m.role = role;
        m.body = body;
        m.think = think;
        return m;
    }

    public static void main(String[] args) throws Throwable {
        // Font scale parsing/clamping.
        check(FontScaleFeature.parse("1.25") == 1.25f, "parse 1.25");
        check(FontScaleFeature.parse("9") == FontScaleFeature.MAX, "clamp max");
        check(FontScaleFeature.parse("0.1") == FontScaleFeature.MIN, "clamp min");
        check(FontScaleFeature.parse("NaN") == 1f, "NaN -> 1");
        check(FontScaleFeature.parse("garbage") == 1f, "garbage -> 1");

        // Conversation formatting strips the module's private system-prompt wrapper.
        List<ChatEditorUi.Msg> thread = new ArrayList<>();
        thread.add(msg("USER", "<system>\nBe brief\n</system>\n\nHello there", ""));
        thread.add(msg("ASSISTANT", "General Kenobi", "thinking line"));
        String md = ConversationTools.format("T", thread, true);
        check(md.startsWith("# T\n"), "md title");
        check(!md.contains("<system>") && !md.contains("Be brief"), "md strips system wrapper");
        check(md.contains("Hello there") && md.contains("General Kenobi"), "md bodies");
        check(md.contains("> thinking line"), "md quotes reasoning");
        String txt = ConversationTools.format("T", thread, false);
        check(!txt.contains("<system>") && txt.contains("Hello there"), "txt body");

        // In-conversation search: case-insensitive, body then reasoning, 1-based index.
        List<ConversationTools.Hit> hits = ConversationTools.search(thread, "KENOBI");
        check(hits.size() == 1 && hits.get(0).index == 2, "search body hit");
        hits = ConversationTools.search(thread, "thinking");
        check(hits.size() == 1 && hits.get(0).full.equals("General Kenobi"), "search think hit");
        check(ConversationTools.search(thread, "be brief").isEmpty(), "search ignores hidden prompt");
        check(ConversationTools.safeName("a/b:c").equals("a_b_c"), "safe name");

        // 2.7.1 history sanitizer against stand-ins shaped like ey1 / gy / v9a.
        ClassLoader loader = PortV271RegressionTest.class.getClassLoader();
        Class<?> fragmentType = Class.forName("v9a", true, loader);
        java.lang.reflect.Constructor<?> fragmentCtor = fragmentType.getConstructor(
                String.class, int.class, String.class);
        Object request = fragmentCtor.newInstance(
                "REQUEST", 1, "<system>\nX\n</system>\n\nreal question");
        Object response = fragmentCtor.newInstance(
                "RESPONSE", 2, "<system>\nkeep\n</system>\n\nanswer");
        Object message = Class.forName("gy", true, loader).newInstance();
        List<Object> fragments = new ArrayList<Object>();
        fragments.add(request);
        fragments.add(response);
        message.getClass().getField("v").set(message, fragments);
        Object history = Class.forName("ey1", true, loader).newInstance();
        List<Object> messages = new ArrayList<Object>();
        messages.add(message);
        history.getClass().getField("b").set(history, messages);
        int cleaned = HostV271.sanitizeHistory(history);
        check(cleaned == 1, "one request cleaned, got " + cleaned);
        Object cleanedRequest = fragments.get(0);
        String content = (String) fragmentType.getField("c").get(cleanedRequest);
        check("real question".equals(content), "request content stripped: " + content);
        check(((String) fragmentType.getField("c").get(response)).startsWith("<system>"),
                "response untouched");
        check(HostV271.sanitizeHistory(history) == 0, "idempotent");
        System.out.println("PASS: 1.8.0 port helpers (font scale, conversation tools, v271 history)");
    }
}
