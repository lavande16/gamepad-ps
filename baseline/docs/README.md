# GamePad PS

当前用户批准基线：**GamePad PS V0.4.1**（2026-10-21），见 `../CURRENT_GAMEPAD_PS_BASELINE.md`。后续开发从该基线继续，固定归档不得覆盖。

当前已构建并覆盖安装 GamePad PS 0.4.1 / 8，包名固定 `com.gamepad.pspad`。NS 首页、卡带、设置布局、手柄和像素控件已接入；自动装配、自动存读档、分辨率切换和横屏限制通过本轮自动验证，完整人工实玩验收仍待用户确认。

- 工作计划：`IMPLEMENTATION_PLAN.md`。
- 全范围直接复用盘点：`REUSE_INVENTORY.md`。
- 固定版本、材料和数据边界：`SOURCE_LOCK.md`。
- NS完整Android及PC前端原始归档：`stage1/references/`。
- 逐文件来源、依赖、校验和材料清单：`stage1/locks/`。
- 当前ARMSX2全局及游戏默认配置：`defaults/`。
- 底座构建、安装与实机证据：`stage2/VALIDATION.md`。
- 自动装配验收和固定安装包：`stage3/VALIDATION.md`、`stage3/artifact.json`。
- 材料目录、导入规则和冲突处理：`MATERIALS.md`。
- 可复跑构建：`bash tools/build_runtime.sh`。
- 默认项设备读回：`python3 tools/verify_stage2_defaults.py --serial 403G7R006F310V0`。
- 可重跑盘点：`python3 tools/audit_reuse.py`。

内存储目录名称固定为 `GamePadPS`，旧 `GamePad PS` 已一次性迁移。支持 `game/game.iso`、`game/游戏名/game.iso` 和旧 `game/游戏名/ISO/game.iso`。

开发约束：能直接使用的代码、素材和组件必须优先直接使用；不能为复刻已有外观重新实现一套页面。只在缺失或确实不能适配时新写，并记录原因。NS优先，PC补充，ARMSX2承载原生运行和PS2功能。

默认项只在初始化时应用，保留后续用户修改及进度。`${ROOT}`须解析到已授权目录；不能沿用旧应用私有路径。原始INI为模板，实际程序还需正确同步应用配置层。

第二步的单游戏初始化已由第三步通用装配器替换。第五阶段使用统一全局默认：4 倍渲染、LGFS 2 倍、Stretch 全屏、OSD Off；全程横屏，显示尺寸在 2304×1296 与原始尺寸之间切换，卡带尺寸保持一致。旧游戏专属修正仅在本轮迁移时清除一次，后续手动调整保留。

本轮验收与交付：`stage5/validation.json`、`stage5/artifact.json`、`stage5/installed-artifact.json`，APK 在 `../artifacts/gamepad-ps-stage5-20261021/GamePad-PS-0.4.0.apk`。201 项单元测试通过，设备 iPA2691 / 403G7R006F310V0 上安装包哈希与归档一致。

由Codex按计划分步实现，不提供人天预算；外接屏首版暂缓。

V0.4.1 基线修复：共享弹窗手柄路由/焦点、列表确认、启动暂停与自动读档生命周期。四项默认（RAISR 4x、预缓存纹理包、GS 多线程、60 FPS 显示上限）一次强制迁移至全局和各游戏。证据与构建产物见 `stage6/validation.json`、`stage6/artifact.json`；V0.4 历史基线独立保留。

V0.4.1 验证边界：四项迁移、208 项单测及前一候选的核心流程已验证；最后追加的首次焦点保护已构建安装，其最终端到端日志确认未完成，详见 stage6/validation.json。
