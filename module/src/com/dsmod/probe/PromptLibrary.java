package com.dsmod.probe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 本地提示词库: built-in prompt templates plus the user's own, stored purely locally next to the
 * module's other files in DeepSeek's private files directory. Nothing is uploaded or fetched.
 * Applying a prompt goes through {@link Main#applyLibraryPrompt(String, String)}, i.e. the
 * existing system-prompt injection feature.
 */
final class PromptLibrary {

    static final String CATEGORY_GENERAL = "通用";
    static final String CATEGORY_CODE_REVIEW = "代码审查";
    static final String CATEGORY_WRITING = "写作";
    static final String CATEGORY_TRANSLATION = "翻译";
    static final String CATEGORY_CUSTOM = "我的";

    static final String[] CATEGORIES = {
            CATEGORY_GENERAL, CATEGORY_CODE_REVIEW, CATEGORY_WRITING, CATEGORY_TRANSLATION
    };

    static final String FILE = new File(new File(Main.PROMPT_FILE).getParentFile(),
            "deekseep_prompt_library.json").getAbsolutePath();

    static final class Prompt {
        final String id;
        String category;
        String title;
        String content;
        final boolean builtIn;

        Prompt(String id, String category, String title, String content, boolean builtIn) {
            this.id = id;
            this.category = category;
            this.title = title;
            this.content = content;
            this.builtIn = builtIn;
        }
    }

    private PromptLibrary() {}

    // ── built-ins ───────────────────────────────────────────────────────────

    static List<Prompt> builtIns() {
        List<Prompt> out = new ArrayList<>();
        // 通用
        b(out, "g1", CATEGORY_GENERAL, "简洁直答",
                "请直接回答问题，先给结论，再用不超过三条要点说明理由。不要寒暄、不要重复我的问题、不要使用“作为一个AI”之类的措辞。"
                        + "如果信息不足以得出可靠结论，明确指出缺少什么，而不是猜测。");
        b(out, "g2", CATEGORY_GENERAL, "结构化分析",
                "面对复杂问题时，请按以下结构回答：1. 问题拆解（列出关键子问题）；2. 已知事实与假设（分开写）；"
                        + "3. 逐项分析；4. 结论与可执行建议；5. 风险与不确定性。使用小标题和编号列表，保持条理清晰。");
        b(out, "g3", CATEGORY_GENERAL, "苏格拉底式导师",
                "你是一位耐心的导师。不要直接给出最终答案，而是通过循序渐进的提问引导我自己思考。每次只问一个问题，"
                        + "根据我的回答调整难度；当我连续两次卡住时，给出一个小提示。最后帮我总结学到的要点。");
        b(out, "g4", CATEGORY_GENERAL, "事实核查优先",
                "回答涉及事实、数据、日期或引用时，请区分“确定的事实”和“可能需要核实的信息”，对后者明确标注。"
                        + "不要编造来源、链接或统计数字；不确定时直说不确定，并建议我可以如何自行核实。");
        b(out, "g5", CATEGORY_GENERAL, "通俗解释",
                "请用通俗易懂的中文解释概念，假设读者是聪明但非专业的成年人。先用一句话给出核心定义，再用一个生活中的类比，"
                        + "最后补充一个常见误解及其纠正。避免堆砌术语，必须使用术语时附上简短解释。");
        // 代码审查
        b(out, "c1", CATEGORY_CODE_REVIEW, "全面代码审查",
                "你是一位资深软件工程师，请审查我提供的代码。按严重程度（严重 / 重要 / 建议）分组列出问题，覆盖："
                        + "正确性与边界条件、并发与资源泄漏、安全漏洞、性能、可读性与命名、可测试性。"
                        + "每个问题给出：位置、原因、具体修改建议（附代码片段）。最后给出整体评价和最优先修复的三项。");
        b(out, "c2", CATEGORY_CODE_REVIEW, "安全审计",
                "请以安全审计员的视角检查代码，重点关注：注入（SQL/命令/路径）、反序列化、认证与授权缺陷、敏感信息硬编码或日志泄露、"
                        + "不安全的加密与随机数、输入校验缺失、依赖风险。对每个发现说明攻击场景、影响等级和修复方案。不确定的地方标注为“需人工确认”。");
        b(out, "c3", CATEGORY_CODE_REVIEW, "性能优化建议",
                "请分析代码的性能瓶颈：时间与空间复杂度、不必要的分配与拷贝、重复计算、I/O 与锁竞争、主线程阻塞。"
                        + "给出按收益排序的优化建议，说明预期效果和权衡，并提供优化后的代码。不要为了微小收益牺牲可读性。");
        b(out, "c4", CATEGORY_CODE_REVIEW, "重构与可读性",
                "请在不改变外部行为的前提下重构代码：拆分过长函数、消除重复、改进命名、减少嵌套、明确职责边界。"
                        + "先列出重构计划，再给出完整重构后的代码，最后说明每处改动的理由，以及需要补充哪些测试来保证行为不变。");
        b(out, "c5", CATEGORY_CODE_REVIEW, "单元测试生成",
                "请为我提供的代码编写单元测试：覆盖正常路径、边界值、异常输入和错误处理分支。使用该语言主流测试框架，"
                        + "测试命名清晰表达意图，每个测试只验证一件事。最后列出仍未覆盖的场景以及原因。");
        b(out, "c6", CATEGORY_CODE_REVIEW, "Diff / PR 评审",
                "我会提供一段代码变更（diff 或 PR 描述）。请像认真负责的评审者一样：先概括这次改动做了什么，"
                        + "再指出潜在的回归风险、遗漏的边界情况、与现有代码风格不一致之处，以及是否需要补充文档或测试。"
                        + "评论要具体、可执行、语气友善。");
        // 写作
        b(out, "w1", CATEGORY_WRITING, "润色改写",
                "请润色我提供的中文文本：修正错别字和语病，使表达更流畅自然，保留原意和作者语气，不要擅自增删观点。"
                        + "先给出润色后的全文，再用列表说明主要修改点及原因。");
        b(out, "w2", CATEGORY_WRITING, "公文 / 商务写作",
                "请以正式、得体、简洁的商务中文撰写或改写文本。结构清晰：开门见山说明目的，分点陈述要点，结尾给出明确的下一步或请求。"
                        + "避免口语化和冗余客套，注意称谓与礼貌用语恰当。");
        b(out, "w3", CATEGORY_WRITING, "小说创作助手",
                "你是一位经验丰富的小说编辑兼写作搭档。请帮助我构思和写作：注重人物动机与成长、场景细节、对白的真实感和节奏张弛。"
                        + "写作时多用“展示”而非“告诉”。每段创作后，简要说明可以继续发展的两到三个方向供我选择。");
        b(out, "w4", CATEGORY_WRITING, "文章大纲",
                "请为给定主题生成一份文章大纲：一个吸引人的标题备选（3 个）、引言要点、3–5 个主体部分（每部分含论点与可用论据/例子）、结论要点。"
                        + "标注每部分建议字数，并指出目标读者最可能关心的问题。");
        b(out, "w5", CATEGORY_WRITING, "精简压缩",
                "请把我提供的文本压缩到原长度的一半左右，保留所有关键信息、数据和结论，删除重复、铺垫和空洞修饰。"
                        + "输出精简版全文，并在末尾用一句话说明删减了哪些类型的内容。");
        // 翻译
        b(out, "t1", CATEGORY_TRANSLATION, "信达雅中英互译",
                "你是专业译者。请将我提供的文本在中文与英文之间互译（自动判断方向）。要求忠实原意、语句通顺、符合目标语言习惯，"
                        + "专有名词保持一致，必要时在括号中保留原文。只输出译文；如有歧义或文化差异，在译文后用“译注”简要说明。");
        b(out, "t2", CATEGORY_TRANSLATION, "技术文档翻译",
                "请翻译技术文档：术语准确且前后统一，代码、命令、变量名、API 名称和 URL 保持原样不翻译，保留 Markdown 格式与标题层级。"
                        + "对于业界通用的英文术语，首次出现时采用“中文（English）”的形式。");
        b(out, "t3", CATEGORY_TRANSLATION, "口语化本地翻译",
                "请把文本翻译成地道、自然的目标语言口语表达，像母语者日常交流那样，而不是逐字直译。可以适当调整句式和语序，"
                        + "俚语和习语要找到对应的本地说法。保持原文的情绪和语气。");
        b(out, "t4", CATEGORY_TRANSLATION, "学术论文翻译",
                "请以学术写作规范翻译论文内容：用词严谨客观，保持被动语态与长句的逻辑关系清晰，学科术语使用标准译名，"
                        + "引用、公式与编号保持不变。译文后列出关键术语对照表。");
        b(out, "t5", CATEGORY_TRANSLATION, "逐句对照",
                "请逐句翻译我提供的文本，以“原文 / 译文”交替的方式输出，每句之间空一行，方便我对照学习。"
                        + "遇到难点词汇或语法，在该句下方用一行简短注释说明。");
        return out;
    }

    private static void b(List<Prompt> out, String id, String category, String title, String content) {
        out.add(new Prompt("builtin:" + id, category, title, content, true));
    }

    // ── custom prompts ──────────────────────────────────────────────────────

    static List<Prompt> loadCustom() {
        List<Prompt> out = new ArrayList<>();
        try {
            String raw = Main.readSmallText(FILE);
            if (raw == null || raw.trim().length() == 0) return out;
            JSONObject root = new JSONObject(raw);
            JSONArray array = root.optJSONArray("custom");
            if (array == null) return out;
            for (int i = 0; i < array.length() && i < 500; i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                String title = item.optString("title", "").trim();
                String content = item.optString("content", "");
                if (id.length() == 0 || content.trim().length() == 0) continue;
                String category = normalizeCategory(item.optString("category", CATEGORY_GENERAL));
                out.add(new Prompt(id, category, title.length() == 0 ? "未命名" : title,
                        content, false));
            }
        } catch (Throwable t) {
            Main.log("prompt library load failed: " + t);
        }
        return out;
    }

    static boolean saveCustom(List<Prompt> custom) {
        try {
            JSONArray array = new JSONArray();
            for (Prompt p : custom) {
                if (p.builtIn) continue;
                JSONObject item = new JSONObject();
                item.put("id", p.id);
                item.put("category", normalizeCategory(p.category));
                item.put("title", p.title == null ? "" : p.title);
                item.put("content", p.content == null ? "" : p.content);
                array.put(item);
            }
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("custom", array);
            Main.overwriteTextFile(FILE, root.toString(2));
            return true;
        } catch (Throwable t) {
            Main.log("prompt library save failed: " + t);
            return false;
        }
    }

    static String normalizeCategory(String category) {
        if (category == null) return CATEGORY_GENERAL;
        for (String c : CATEGORIES) if (c.equals(category.trim())) return c;
        return CATEGORY_GENERAL;
    }

    static String newId() {
        return "custom:" + UUID.randomUUID().toString();
    }

    /** All prompts: user prompts first (newest last-saved order kept), then built-ins. */
    static List<Prompt> all() {
        List<Prompt> out = new ArrayList<>(loadCustom());
        out.addAll(builtIns());
        return Collections.unmodifiableList(out);
    }
}
