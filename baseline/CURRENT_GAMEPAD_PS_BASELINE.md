# GamePad PS 当前基线

当前基线：**GamePad PS V0.4.1**。用户于 2026-10-21 明确批准。

- 冻结目录：`baselines/GamePad-PS-V0.4.1/`
- 固定 APK：`baselines/GamePad-PS-V0.4.1/GamePad-PS-V0.4.1.apk`
- 包名：`com.gamepad.pspad`；versionName `0.4.1`；versionCode `8`。
- APK SHA-256：`7ac5df49b1089ea7bd67e7233db8103dfdb9a3c40cc99c52d5057973354415c5`，与已安装版本一致。
- 源码：V0.4 完整源码归档加 V0.4.1 差异归档，恢复方法与哈希见冻结目录 `BASELINE.json`。V0.4 历史基线独立保留。
- 本轮仅归档，不重新打包、安装或修改设备数据。

修复范围：弹窗手柄确认与焦点、自动读档生命周期；RAISR 4x、纹理预缓存、GS 多线程和 60 FPS 显示上限一次强制应用至全局及现有游戏，后续手动修改保留。

既有默认保持：横屏、Stretch 全屏、4 倍渲染、LGFS 2 倍、OSD Off；目录 GamePadPS；2304×1296 与原始分辨率切换时保持卡带尺寸。

验证边界：208 项单元测试通过，安装哈希一致；此前候选的核心流程验证已归档。最后追加的首次焦点保护已构建安装，但最终端到端日志确认未完成；不将其记为完整通过，详见 `baselines/GamePad-PS-V0.4.1/evidence/validation.json`。

后续修改以此基线为起点，不覆盖冻结目录；包名固定。
