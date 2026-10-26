# 包名与图标迁移

2026-10-20：0.3.1 已由 com.gamepad.ps 迁入 com.gamepad.gamepadps；本轮用户要求再次参考 NS/PC 调整，0.3.2 / 5 改为 `com.gamepad.gps`。NS 实际为 com.gamepad.gamepad，PC 为 com.gamepad.pc。

方形显示不能只靠包名：0.3.1 仍使用 adaptive icon，0.3.2 与 NS 一样让 icon 和 roundIcon 都直接引用完整 drawable。

更换身份时直接迁移上一版当前配置/内部素材；共享 GamePad PS 的游戏、记忆卡、即时存档继续原位使用。不做每次修改前的常规备份，不用历史存档覆盖当前进度。旧身份保持停止。

2026-10-20 追加：用户指定下一次使用 `com.gamepad.pspad`；0.3.3 / 6 仅更换产品身份与版本号，完整 drawable 图标声明保持 0.3.2。直接从 com.gamepad.gps 迁移当前配置，共享游戏与存档不搬动。图标形状以真机桌面为准，不预先断言包名可解决。

真机结果：com.gamepad.pspad / 0.3.3 / 6 安装成功，主页两款游戏正常。桌面最新 PS 图标实际显示为圆角方形，点击后前台包名确认 com.gamepad.pspad；与旁边旧身份圆形图标不同。后续固定此身份。证据见 stage4c/launcher-after.png 和 validation.json。
