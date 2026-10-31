package com.dsmod.probe;

import android.app.Activity;
import android.graphics.Typeface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * YunSeek 自定义字体: an imported .ttf/.otf copied into DeepSeek's private files dir and made the
 * process default Typeface (Typeface.DEFAULT / SANS_SERIF / setDefault / "sans-serif" system map).
 * Text is laid out and drawn with the same font, so host Compose text and module Views both pick
 * it up after the next launch. Every step is best-effort; a broken file is simply ignored.
 */
final class FontOverride {
    static final String FILE = "/data/data/com.deepseek.chat/files/yunseek_font.bin";
    private static final AtomicBoolean APPLIED = new AtomicBoolean();

    private FontOverride() {}

    static boolean hasFont() { return new File(FILE).isFile() && new File(FILE).length() > 0; }

    static String importFont(Activity act, Uri uri) {
        if (act == null || uri == null) return "未选择文件";
        File tmp = new File(FILE + ".tmp");
        InputStream in = null;
        OutputStream out = null;
        try {
            in = act.getContentResolver().openInputStream(uri);
            if (in == null) return "无法读取字体文件";
            out = new FileOutputStream(tmp);
            byte[] buf = new byte[16384];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > 40L * 1024 * 1024) return "字体文件过大（>40MB）";
                out.write(buf, 0, n);
            }
            out.close();
            out = null;
            Typeface probe = Typeface.createFromFile(tmp);   // validates the font
            if (probe == null) return "不是有效的 TTF/OTF 字体";
            File target = new File(FILE);
            if (target.exists()) target.delete();
            if (!tmp.renameTo(target)) return "保存字体失败";
            return null;
        } catch (Throwable t) {
            return "导入失败：" + t.getMessage();
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
            tmp.delete();
        }
    }

    static boolean clear() { return !new File(FILE).exists() || new File(FILE).delete(); }

    /** Called once per process from activity resume; effective for text laid out afterwards. */
    @SuppressWarnings("unchecked")
    static void applyOnce() {
        if (!hasFont() || !APPLIED.compareAndSet(false, true)) return;
        try {
            Typeface regular = Typeface.createFromFile(FILE);
            if (regular == null) return;
            for (String name : new String[]{"DEFAULT", "SANS_SERIF"}) {
                try {
                    Field f = Typeface.class.getDeclaredField(name);
                    f.setAccessible(true);
                    f.set(null, regular);
                } catch (Throwable ignored) {}
            }
            try {
                Method m = Typeface.class.getDeclaredMethod("setDefault", Typeface.class);
                m.setAccessible(true);
                m.invoke(null, regular);
            } catch (Throwable ignored) {}
            try {
                Field f = Typeface.class.getDeclaredField("sSystemFontMap");
                f.setAccessible(true);
                Map<String, Typeface> map = (Map<String, Typeface>) f.get(null);
                if (map != null) {
                    java.util.HashMap<String, Typeface> copy = new java.util.HashMap<>(map);
                    copy.put("sans-serif", regular);
                    try { f.set(null, copy); } catch (Throwable e) { map.put("sans-serif", regular); }
                }
            } catch (Throwable ignored) {}
            Main.log("custom font applied");
        } catch (Throwable t) {
            Main.log("custom font skipped: " + t);
        }
    }
}
