package com.dsmod.probe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure parser and launch policy for the independently verified code249/code257 catalogs. */
final class HostNavigationCatalog {
    private static final int MAX_DEX_BYTES = 48 * 1024 * 1024;
    private static final Pattern ROUTE = Pattern.compile(
            "com\\.deepseek\\.chat\\.ui\\.pages\\.[A-Za-z0-9_.$]{1,160}(?:Route|Graph)");

    static final String DIRECT = "direct";
    static final String HIGH_RISK = "confirm";
    static final String PARAMETERS = "parameters";
    static final String INTERNAL = "internal";
    static final String BLOCKED = "blocked";

    private HostNavigationCatalog() {}

    static List<String> extractRoutes(InputStream input) throws IOException {
        if (input == null) return Collections.emptyList();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(512 * 1024);
        byte[] buffer = new byte[32 * 1024];
        int total = 0;
        while (true) {
            int read = input.read(buffer);
            if (read < 0) break;
            total += read;
            if (total > MAX_DEX_BYTES) throw new IOException("DEX exceeds navigation scan limit");
            bytes.write(buffer, 0, read);
        }
        String raw = new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
        Set<String> unique = new LinkedHashSet<String>();
        Matcher matcher = ROUTE.matcher(raw);
        while (matcher.find()) unique.add(matcher.group());
        ArrayList<String> out = new ArrayList<String>(unique);
        Collections.sort(out);
        return out;
    }

    static String policy(String route) {
        if (route == null) return BLOCKED;
        if (route.endsWith("AccountDeletionRoute")) return HIGH_RISK;
        if (route.contains("AuthNestedGraph") || route.endsWith("WelcomeRoute")
                ) return INTERNAL;
        if (route.endsWith("AgeScreenRoute") || route.endsWith("MobileRebindRoute")
                || route.endsWith("MobileVerificationRoute")
                || route.endsWith("ResetPasswordRoute")
                || route.endsWith("WebViewRoute") || route.endsWith("TableRoute")) {
            return PARAMETERS;
        }
        if (route.endsWith("ChatRoute") || route.endsWith("SettingsNestedGraph")
                || route.endsWith("SettingsRoute")
                || route.endsWith("AccountManagementRoute")
                || route.endsWith("DataControlsSettingsRoute")
                || route.endsWith("FontSizeRoute")
                || route.endsWith("MinorModeBirthdayModifyRoute")
                || route.endsWith("ShareLinksManagementRoute")
                || route.endsWith("TermsOfServiceRoute")) return DIRECT;
        return INTERNAL;
    }

    static String title(String route) {
        if (route == null) return "未知页面";
        if (route.endsWith("ChatRoute")) return "聊天主页";
        if (route.endsWith("SettingsNestedGraph") || route.endsWith("SettingsRoute")) return "设置";
        if (route.endsWith("AccountManagementRoute")) return "账号管理";
        if (route.endsWith("AccountDeletionRoute")) return "删除账号";
        if (route.endsWith("DataControlsSettingsRoute")) return "数据控制";
        if (route.endsWith("FontSizeRoute")) return "字体大小";
        if (route.endsWith("MinorModeBirthdayModifyRoute")) return "生日/未成年人模式";
        if (route.endsWith("ShareLinksManagementRoute")) return "分享链接管理";
        if (route.endsWith("TermsOfServiceRoute")) return "用户协议与隐私";
        if (route.endsWith("PasswordLoginRoute")) return "密码登录";
        if (route.endsWith("RegisterRoute")) return "注册";
        if (route.endsWith("SmsLoginRoute")) return "短信登录";
        if (route.endsWith("WelcomeRoute")) return "欢迎页";
        if (route.endsWith("WebViewRoute")) return "网页页面";
        if (route.endsWith("TableRoute")) return "表格预览";
        int dot = route.lastIndexOf('.');
        return dot >= 0 ? route.substring(dot + 1) : route;
    }

    static String obfuscatedRouteClass(String route) {
        if (HostCompat.isV271()) return routeClassV271(route, HostCompat.hostCode271());
        if (HostCompat.isV236()) return obfuscatedRouteClass249(route);
        if (!HostCompat.isV241()) return null;
        return obfuscatedRouteClass257(route);
    }

    /**
     * 2.7.x route objects, read from each official APK's serializer table (the class whose
     * INSTANCE is handed to ObjectSerializer together with the route's serial name). These are
     * the hosts' REAL class names: load them directly, never through HostRemap.
     */
    static String routeClassV271(String route, int code) {
        if (route == null) return null;
        String[] t;
        switch (code) {
            case 287: t = new String[]{"bf2", "qr9", "nr9", "hr9", "gr9", "ir9", "jr9", "lr9", "or9", "pr9"}; break;
            case 289: t = new String[]{"xf2", "rw9", "ow9", "hw9", "gw9", "jw9", "kw9", "mw9", "pw9", "qw9"}; break;
            case 290: t = new String[]{"oh2", "r0a", "o0a", "h0a", "g0a", "j0a", "k0a", "m0a", "p0a", "q0a"}; break;
            default: return null;
        }
        if (route.endsWith("SettingsNestedGraph.SettingsRoute")) return t[2];
        if (route.endsWith("ChatRoute")) return t[0];
        if (route.endsWith("SettingsNestedGraph")) return t[1];
        if (route.endsWith("AccountManagementRoute")) return t[3];
        if (route.endsWith("AccountDeletionRoute")) return t[4];
        if (route.endsWith("DataControlsSettingsRoute")) return t[5];
        if (route.endsWith("FontSizeRoute")) return t[6];
        if (route.endsWith("MinorModeBirthdayModifyRoute")) return t[7];
        if (route.endsWith("ShareLinksManagementRoute")) return t[8];
        if (route.endsWith("TermsOfServiceRoute")) return t[9];
        return null;
    }

    static String obfuscatedRouteClass249(String route) {
        if (route == null) return null;
        if (route.endsWith("ChatRoute")) return "oa1";
        if (route.endsWith("SettingsNestedGraph")) return "kn7";
        if (route.endsWith("SettingsNestedGraph.SettingsRoute")) return "hn7";
        if (route.endsWith("AccountManagementRoute")) return "en7";
        if (route.endsWith("AccountDeletionRoute")) return "dn7";
        if (route.endsWith("DataControlsSettingsRoute")) return "fn7";
        if (route.endsWith("FontSizeRoute")) return "gn7";
        if (route.endsWith("ShareLinksManagementRoute")) return "in7";
        if (route.endsWith("TermsOfServiceRoute")) return "jn7";
        return null;
    }

    static String obfuscatedRouteClass257(String route) {
        if (route == null) return null;
        if (route.endsWith("ChatRoute")) return "id1";
        if (route.endsWith("SettingsNestedGraph")) return "kv7";
        if (route.endsWith("SettingsNestedGraph.SettingsRoute")) return "hv7";
        if (route.endsWith("AccountManagementRoute")) return "dv7";
        if (route.endsWith("AccountDeletionRoute")) return "cv7";
        if (route.endsWith("DataControlsSettingsRoute")) return "ev7";
        if (route.endsWith("FontSizeRoute")) return "fv7";
        if (route.endsWith("MinorModeBirthdayModifyRoute")) return "gv7";
        if (route.endsWith("ShareLinksManagementRoute")) return "iv7";
        if (route.endsWith("TermsOfServiceRoute")) return "jv7";
        return null;
    }
}
