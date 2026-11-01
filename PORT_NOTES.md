# Deekseep port notes (to latest mainland DeepSeek)

Progress log. Newest at bottom.

## 2026-10-25 start
- Repo at dd087f3 (v1.7.5). No JDK/SDK/jadx in sandbox initially. 2 cores, 7GB RAM.
- Sandbox is **aarch64**. Toolchain at /workspace/work/tools (source /workspace/work/tools/env.sh):
  JDK 17 aarch64 (Adoptium), SDK platform-35 + build-tools 35 (from dl.google.com zips; sdkmanager can't resolve on arm64),
  native aapt/aapt2/zipalign replaced by lzhiyong/android-sdk-tools 35.0.2 static aarch64. jadx 1.5.1 in tools/jadx, apktool 2.10.0 jar.
- Host APK: https://download.deepseek.com/apk/deepseek.apk (Last-Modified 2026-10-23) -> host/deepseek-mainland-latest.apk
- **Target host: DeepSeek mainland 2.7.1, versionCode 287** (sha256 86e77d41...efc8, 22,006,745 bytes). No com.pairip. Contains libsmsdk (Shumei) still.
- jadx 2.7.1 output: host/jadx-271 (58 decompile errors, fine).
- Mainland 2.4.1 APK NOT obtainable from sandbox (apkpure/apkcombo/uptodown 403/Cloudflare; download.deepseek.com serves only latest).
  => remapping done by string anchors / serialization names / signatures directly in 2.7.1, not by old->new diff.

### Architecture findings
- Entry: handleLoadPackage -> HostCompat.initialize -> supportsMaintainedFeatureRuntime gate -> lifecycle -> performModuleInjection -> installAllFeatureHooks (~60 hook installers, most with per-generation literal names).
- Module UI opened via floating button (Main.showButton) shown when host settings page is visible (settings composable hook / page_show analytics hook / nav route).
- Many module tools (export all, search, stats, backup, chat editor) read host WCDB **directly via SQLiteDatabase** (deepseek_chat_<uuid>.db: chat_session_list, chat_session_messages_<sid>) -> host-version independent if schema unchanged.
- Strategy for 2.7.1: new `v271` generation with a WHITELIST installer; HostCompat.name() returns an unresolvable sentinel for any unmapped symbol on v271, so stray legacy lookups fail closed (ClassNotFound) instead of hooking an unrelated class.

### 2.7.1 anchors found
- ChatFullCompletionRequest = `sx1` (serializer qx1 names "com.deepseek.chat.network.chat.model.chat.ChatFullCompletionRequest"), implements `du1`.
  Real ctor (String sid, Integer parent, String prompt, ArrayList refFiles, bool thinking, bool search, String audio, bool preempt, String model, String action?, int mask) == classic 11-arg shape.
- page_show analytics: static `gt0.Q(String page, String, Boolean, Boolean, Boolean, int)`; page "settings" fired by settings screen (s4 case 21).
- Baseline build of unmodified 1.7.5 succeeded on aarch64 toolchain (36s).
- 2.7.1 history: ChatHistoryMessagesResponseBizData = `ey1`(a=un9 session, b=List<gy>, c=cache_control, d=cache_reset_at); session `un9.a`=id.
  AppStaticMessage = `gy` (fragments list = field `v`; legacy "t", v230 "u"). RequestFragment = `v9a`(a=type "REQUEST", b=id, c=content; ctor (int,String)).

## Implementation (done)
- HostCompat: `v271` flag; detection = no pairip + `sx1` has the 11-arg ChatFullCompletionRequest ctor + implements `du1`.
  Detected first and short-circuits all older detections. supportsHostVersionName += "2.7.1"; generationName "2.7.1/code287-cn".
  name() on v271 -> name271(): only ew0->sx1, pw0->ey1 mapped; everything else -> "deekseep unmapped <x>" (unloadable).
  settingsEntryMethod on v271 -> gt0.Q(String,String,Boolean,Boolean,Boolean,int). trainingControlMethod/updateDialogMethod -> null on v271.
- New HostV271.java: whitelist installer = settings entry (show on page "settings", hide on any other page_show),
  HookChatPipeline.hookChatRequest (sx1; Agent tool prompt / chat-proxy / recall-context branches disabled on v271),
  history sanitizer on ey1 ctors (strip <system> wrapper from REQUEST fragments in gy.v).
- Main: installAllFeatureHooks -> HostV271 whitelist on v271; ensureOptionalHooks no-op on v271; early dual-chat root hook,
  RemoteFeatureFlags.enforce, Agent backend probe, heartbeat intent skipped on v271.
- New features: ConversationTools.java (per-conversation copy / export .md / export .txt / share / search-in-conversation; read-only SQLite),
  FontScaleFeature.java (DeepSeek-only text size via sparse fontScale override on MainActivity.attachBaseContext; framework API only).
- Donation removal: DeekseepUi sponsor row+dialog, SettingsActivity sponsor row+dialog, ModuleGlyphView SPONSOR, HubMaterialGlyphView sponsor glyph,
  UiLanguageCatalog sponsor strings, sponsor_qr.png (git rm), compose-launcher sponsor dialog/menu/card + gradle staging, README/README_CN/DISCLAIMER afdian lines.

## Build / test (done 2026-10-25)
- `source /workspace/work/tools/env.sh && (cd module-universal && bash build.sh)` -> OK.
- Output: out/Deekseep-1.8.0-port-mainland-2.7.1.apk (versionName 1.8.0-port, versionCode 36),
  sha256 8d23829ec86b94dc124cd1fd61f934fa6d84fd253ac7eeed2d484abbd1d0308a, signed with locally generated module/debug.keystore
  (CN=Deekseep Source Build; NOT the upstream key -> uninstall an upstream Deekseep build first).
- Tests: module/test-port-v271.sh PASS (new); test-expert-relay-regression.sh PASS;
  test-language-catalog.sh and test-thinking-regression.sh FAIL identically on unmodified upstream 1.7.5 (pre-existing, verified via clean worktree build).
- Class names verified as real (not jadx renames) by searching dex string tables for Lsx1; Lgt0; Ley1; Lgy; Lv9a; Ldu1;.

## Feature status on 2.7.1
Working (confident): module entry button on DeepSeek settings page; custom system prompt injection (+ Java-plugin outgoing transform/plugin prompt);
history wrapper sanitizer; DB-based tools (export all, global search, backup, stats); new Conversation tools; new Text size.
Disabled (not installed on v271): Agent/MCP/heartbeat tools & render hooks, dual chat, anti-recall/no-censor, response preserver, thinking code copy,
local quota unlock, all-file-types, expert relay/image bridges, bubble/avatar/theme color/whale/text-wave host hooks, sidebar multi-select,
native settings row, nav route manager, hot-update dialog/remote-config hooks, training opt-out Compose control, remote feature flag enforcement,
login/region/password unlocks, SMSDK/Shumei risk bypass, anti-ban, Local API (not in open source anyway), live editor refresh (DB edits need app restart).
Unverified: ChatEditorUi (编辑聊天记录) DB writes on 2.7.1 (schema matches, untested on device).

## Next steps if continuing
- Get mainland 2.4.1 APK (code257) to structurally diff and port more hooks (anti-recall, thinking copy, theme). Use browser to fetch from apkpure if needed.

## 2026-10-26 structural diff
- Got mainland 2.4.1 code257 from wandoujia (md5 9f737e728fb710047619d2a6b096977d) -> host/deepseek-2.4.1-257.apk.
- host/map/extract.py (androguard -> /tmp/c257.pkl, /tmp/c287.pkl), match.py (unique strings + structural sig + ref propagation -> /tmp/cmap.json; 8029/10835 classes, az0->sx1 verified),
  fuzzy.py (neighbourhood scoring for leftovers -> /tmp/cmap2.json). /tmp/legacy_to_241.json = composed legacy->257 name table (179 distinct classes).
- /tmp is volatile: rerun extract.py if pkl files are gone.

## 2026-10-26 full mode (HostRemap)
- New HostRemap.java: on 2.7.1 the reviewed 2.4.1 feature code runs through a generated table (module/hostmap/hostmap287.txt.gz,
  packaged at APK root; built by host/map/{match,audit,fuzzy,fzfilter,members,genmap}.py, driver host/map/pipeline.sh).
  Unmatched classes/members fail closed (unloadable name / NoSuch*).
- host/map/rewrite.py rewrote ~850 reflection sites (getDeclaredMethod/getMethod/getDeclaredField/getField/loadClass/Class.forName,
  Member.getName, Class.getName/getSimpleName) to HostRemap.*. HostCompat/HostV271 excluded on purpose (detection runs raw).
- HostCompat: v271 detected -> HostRemap.activate(); success => v241/v234/v230 flags on (full mode). isV271Limited()/isV271Full().
  Old whitelist (HostV271) only when the table is missing; full mode still installs HostV271 settings entry (gt0.Q, raw names).
- Gated off on 2.7.1 (user's no-list): quota unlock, expert unlock/upload gate, expert image relay (PoW/network capture/fp/credential),
  regional/login-entry/region override/candidate login/captcha callbacks/login error rewrite, risk SDK neutralizer (incl. android_id/OAID spoof),
  login risk logger, RemoteFeatureFlags install+enforce.
- JVM verifier: host/verify/VerifyHooks.java (dex2jar /tmp/ds257.jar & /tmp/ds287.jar, run with -XX:-BytecodeVerification*),
  counts hooks per installer. Baseline 257 = 70 installer groups with hooks. 287 now: 31 OK, 5 partial, 34 missing.
- Main blocker: R8 "grab-bag" static classes (ch2, cy9, df5, q55, ufa...) reshuffled between versions -> need method-level matching
  for static Compose functions, then per-site source edits (class differs per method).

## 2026-10-26 method-level pass
- /tmp wiped by sandbox reset: `host/map/rebuild_tmp.sh` restores pkl/jars/compiled module/verifier.
- `host/map/mmatch.py` (method matcher, `run_any` for shape-agnostic search), `genmeth.py` -> `methreloc.txt` (audited `N` lines appended to hostmap287 by pipeline.sh). HostRemap understands `N oldCls oldName (args) newCls newName (args)`; exact live-arg match only.
- Rejected generic (Object)/(Continuation) sigs and wv0.a (arg class conflict xf7->w2c vs qqc).
- Verifier: 39 OK / 4 PART / 27 MISS (was 31/5/34). Remaining are mostly Compose functions whose parameter lists changed -> need hand-ported index tables, not table entries.
- FINDING: 2.7.1 natively has sidebar batch select (select_chat_title, session_batch_delete_*) and batch pin/unpin (session_batch_pin_*). m36.f = ChatSessionItem(ps, multiSelectMode Z, checked Z, ax9, active Z, click, longPress, dx4, bx4, dx4, delete?, Modifier, Composer, int). Proposal: on 2.7.1 skip our overlay multi-select + local pin, use native.
- 2026-10-26: Agent clipboard tool added (tool=clipboard, action read|write, text<=8000, off by default, toast on each access). Sidebar overlay multi-select/toggle-cleanup off on 2.7.1 (native). TODO: hide login/risk toggles in UI on 2.7.1; 公告/彩蛋; remaining 27 hooks.
- 2026-10-26: 公告 (DeekseepUi.FORK_NOTICE_*, startup once via dont_show_fork_notice_v36, also row in 彩蛋 page); 彩蛋 entry visible on main settings page; '防止封号' prank row relabeled '彩蛋 🥚' with honest desc; excluded rows (风控/登录解锁/次数/专家解锁) hidden on 2.7.1 incl. search.
- 2026-10-26 pass 3: FIXED HostRemap reverse-member bug (M reverse keys were translated while the table was still loading -> args of later classes and every java.* type became "?x", so HostRemap.name() failed on most methods with object params). Now deferred + non-obf types kept.
  New N relocations: xf5.a->dx6.a (reasoning code copy), hr8.e->jsa.f (TextStyle copy$default, 15 params), aj3.c->su4.c, g71.y->f72.C. Home greeting on 2.7.1 uses raw jd7.v/w + res ids 0x7f0f022b/0x7f0f03eb.
  Verifier shim host/verify/shim (android.os.SystemClock) removes UnsatisfiedLinkError noise. host/map/apply_reloc.sh = append N lines + rerun verifier.
  Result: 48 OK / 5 PART / 18 MISS. Over-baseline: installNetworkPayloadCapture now resolvable but stays gated off in Main on 2.7.1.
  hookSafetyRetraction + hookContentFilterApply (去他妈的安全审查) gated off on 2.7.1 by fork policy; row hidden.

## 2026-10-26 13:04 anti-recall decision
- User asked to keep ALL original features incl. anti-recall. Declined full anti-recall (persist retracted text as context + override ban_regenerate) and the other excluded items (risk-control, login/region, quota, anti-ban).
- Offered: read-only local copy of already-shown text, collapsed + labelled "官方已撤回", never sent back to model, no ban_regenerate override. Awaiting user yes before implementing.

## 2026-10-26 13:06 gated-consent decision (supersedes 13:04 note)
- User proposal: excluded features greyed out, tap → safety warning, must read + agree before enabling.
- Agreed to apply that gate to anti-recall / safety-review bypass only (content already delivered to the user's own account). Proposed it to user; awaiting go-ahead before coding.
- Still NOT building even behind a gate: quota/expert unlock, anti-ban, Android ID/OAID spoof, risk-control SDK neutralization, login/region unlock.

## 2026-10-26 13:09 updates
- 2.7.1 merged the 3 modes into one — no deep-thinking toggle exists; drop all disguise/spoof work (already excluded).
- User pushed again for risk-control SDK bypass + login/region unlock as "anti-ban". Declined again (same bottom line). Only non-evasion features ported.

## 2026-10-26 13:10 paused (user at school)
- User reports DeepSeek bans after using Agent on new version (3 days, then 9 days); asked for Agent "disguise". Declined disguise. Offered instead: make Agent rate-limited / human-paced so it doesn't hammer endpoints. Discuss when user returns. No work until then.

## 2026-10-26 19:06 paused by user
- Done (committed, NOT yet compiled/verified): AgentRateLimiter + pacing gate in HiddenAgentToolResultAttempt + explanation text on AgentSettingsUi.
- Not started: gated anti-recall (consent dialog). Not started: Gradle 32MB debug build.
- 2026-10-27: Gradle 9.8.2 + AGP 9.3 Open debug built (protectedBuild=false, localApiIncluded=false), versionCode 36/1.8.0, stripped sig + zipalign -> out/Deekseep-1.8.0-unsigned-aligned.apk 26.6MB sha256 75e6ab3d... Sandbox is aarch64: use build-tools/35.0.0 aapt2 override; proxy creds in /tmp/gh/gradle.properties. README rewritten (upstream README moved to docs/upstream/).

## 2026-10-27 晚：真机反馈修复（commit 3553c23）
- Agent 工具结果“正在回传/未能回传”：2.7.1 发送桥 zg1.Q(vm,String,n1,String,Z,I) 变为 oj2.k0(vm,String,q1,String,Z,Z,I)，6 参查找永不命中 → invokeNativeUiTextSend 静默 false。已加 isV271 分支按 7 参签名匹配，mask 232（8|32|64|128，对齐宿主自身文本发送调用点 bit64）。
- 桌面入口：compose-launcher 清单缺 android:icon，已补 mipmap/ic_launcher + roundIcon。若仍打不开需用户给崩溃日志。
- Shizuku 连接失败：未定位，待用户提供“连接失败：”后的完整详情 + Hook 日志。免后台方案设想：模块 APK 声明 ShizukuProvider 持有 binder，DeepSeek 经模块 provider 取 binder（较大改动，待确认）。

## 2026-10-28 12:50 semi-auto Agent + image import (commit 9144f6a)
- AgentManualGate: default semi-auto — each hidden tool result / screenshot / txt attachment send shows a confirm card (发送 / 复制并稍后 / 稍后); 稍后 leaves outbox row waitingForChat (retry from Agent runs). Composer fill not used: 2.7.1 Compose input not reliably reachable, so confirm card instead.
- Full-auto: switch "全自动回传（高风险）" in Agent settings, off by default, needs risk notice + ack checkbox; stored in deekseep_agent/delivery_mode.txt. Rate limiter only in auto.
- Image import: copyAndValidateImage tries openInputStream -> openFileDescriptor -> file path; errors now show exception (读取图片失败/保存图片副本失败). Read failure bumps picker fallback (default -> GET_CONTENT -> OPEN_DOCUMENT).
- APK rebuilt 1.8.0/code36 unsigned aligned, 26,561,573 B, sha256 ea8426fe69c41f250fbd02bd6cf6a695816c537551afff3841502966299049e5.

## 2026-10-31 07:16 YunSeek multi-host: 2.7.2 domestic (289) + Google Play intl (290)
- Host APKs: host/deepseek-mainland-2.7.2-289.apk, host/deepseek-intl-2.7.2-290-base.apk (base only, no native libs; Java hooks fine). Same package com.deepseek.chat; distinguish by versionCode (add a flavor fingerprint later, codes may collide in future).
- Obfuscation fully reshuffled: ~1.6%/1.2% classes identical to 287; 289 vs 290 differ too -> separate tables.
- host/map scripts now env-driven (A_PKL/B_PKL/CMAP/MMAP/HOSTOUT); host/map/multi.sh CODE builds /tmp/hostmapCODE.txt.gz (257->CODE).
- Auto class map (pre-manual): 287 7920 / 289 7632 / 290 7617 classes; targets.txt classes mapped 24/26/25 of 32. Tables copied to module/hostmap/hostmap289/290.txt.gz.
- TODO next: HostRemap pick table by versionCode (287/289/290, fallback none); redo 35 manual methreloc N lines per host (compose via 287->289/290 match); run VerifyHooks for 289/290; isV271 branches -> generalize to ">=287".
