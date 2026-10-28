# Troubleshooting

- If the module is not active, confirm the installed DeepSeek version, LSPosed
  scope, and Xposed API compatibility, then force-stop and reopen DeepSeek.
- If a display modification is incorrect, disable that appearance option and
  test both light and dark mode after restarting the host.
- If an Agent action is not executed, verify its individual tool switch,
  confirmation policy, and Root/Shizuku permission.
- 用 Agent 后账号被强制下线 / 要求重新验证：这是 DeepSeek 2.4.1 起服务端的行为，模块不做任何规避。
  请保持默认的半自动模式（每步手动点“发送”），不要在重要账号上开启全自动回传。
- 导入背景 / 头像提示“无法读取所选图片（…ENOENT…）”：多见于免 root 虚拟机（如 FPA）的存储重定向。
  再点一次“选择图片”会换用另一种选择器，尽量从“文件”而不是相册里选。
- Shizuku 显示“连接失败”：已知问题，排查中。提 Issue 时请附“连接失败：”后的完整内容和模块日志。
- If an experimental hook crashes the host, disable the feature and attach the
  module crash record plus the exact DeepSeek version/channel to the issue.

Do not post credentials, account tokens, private conversations, or unredacted
device data in a public issue.
