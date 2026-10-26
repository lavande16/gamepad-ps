# GamePad PS

> **把 PS2 游戏库，带进随身的 Android 游戏空间。**  
> Bring your PS2 library into a handheld Android play space.

<p align="center"><a href="https://gamefold.pages.dev"><img src="https://gamefold.pages.dev/assets/product/fold-gameplay.webp" alt="GameFold 折叠屏游戏体验视觉" width="88%"></a></p>
<p align="center"><sub>GameFold 产品家族视觉｜GameFold family artwork · GamePad PS 暂无独立官网，暂用家族官网介绍品牌。</sub></p>

<p align="center"><a href="https://gamefold.pages.dev">产品官网 / Official site</a> · <a href="https://github.com/lavande16/gamepad-ps-private/releases/tag/gamepad-ps-v0.4.1-8-baseline">下载 V0.4.1 / Download</a> · <a href="#product-family">GameFold 产品矩阵 / Product family</a></p>

## 当前基线 · Current baseline

| 项目 | 信息 |
|---|---|
| 基线版本 | **GamePad PS V0.4.1 · versionCode 8** |
| Android 包名 | **com.gamepad.pspad** |
| 方向与显示 | 横屏游玩；默认 4× 渲染、LGFS 2×、Stretch 全屏、OSD 关闭 |
| 存储目录 | **GamePadPS** |
| 下载 | [GitHub Release：V0.4.1 / 8](https://github.com/lavande16/gamepad-ps-private/releases/tag/gamepad-ps-v0.4.1-8-baseline) |

## 为玩家准备的体验 · Made for players

- **整洁的游戏入口：**以卡带式游戏库进入 PS2 游戏，支持自动装配与自动存档读档流程。  
  **A focused library:** launch through a cartridge-inspired collection with automatic game setup and save/load flow.
- **横屏掌机布局：**以横屏作为主要游玩方向，按掌机握持方式呈现控制与内容。  
  **Landscape first:** controls and content are arranged for a handheld grip.
- **适配性能的图像默认值：**提供统一的渲染、LGFS 与画面比例初始设置；首次初始化时应用，保留之后玩家自己的修改与进度。  
  **Practical graphics defaults:** rendering, LGFS and aspect settings are initialized once, while later preferences and progress are preserved.
- **灵活导入自己的游戏：**支持 game/game.iso、game/游戏名/game.iso 和旧版 game/游戏名/ISO/game.iso 目录结构。  
  **Bring your own library:** supported layouts include game/game.iso, game/Title/game.iso and the legacy game/Title/ISO/game.iso structure.

> 本项目不附带游戏 ROM、BIOS 或商业游戏素材。请只导入你有权使用的文件。GamePad PS 为独立爱好项目，与 PlayStation、Sony 或其他权利人没有隶属或授权关系。  
> Game files, BIOS files and commercial game assets are not included. Import only files you are entitled to use. This is an independent project and is not affiliated with or endorsed by PlayStation, Sony, or other rights holders.

## 从基线继续开发 · Continue from the baseline

本仓库保留当前 V0.4.1 的源码基线、运行时、默认配置与构建工具。建议先阅读 [实现计划](IMPLEMENTATION_PLAN.md)、[源码边界](SOURCE_LOCK.md)、[素材导入规则](MATERIALS.md)、[复用盘点](REUSE_INVENTORY.md) 和 [后续迁移说明](NEXT_PACKAGE_MIGRATION.md)。

This repository preserves the V0.4.1 source baseline, runtime, defaults and build tools. Start with the [implementation plan](IMPLEMENTATION_PLAN.md), [source boundaries](SOURCE_LOCK.md), [materials guide](MATERIALS.md), [reuse inventory](REUSE_INVENTORY.md) and [migration notes](NEXT_PACKAGE_MIGRATION.md).

构建命令 / Build command: **bash tools/build_runtime.sh**

V0.4.1 已覆盖关键配置迁移、208 项单元测试及前序核心流程；首次焦点保护的最终端到端日志尚未完成，详见仓库内的阶段验证记录。  
V0.4.1 includes the verified settings migration, 208 unit tests and earlier core flows. The final end-to-end log for first-focus protection is still outstanding; see the stage validation records in this repository.

## 官网与关联产品 · Website & related products

GamePad PS 暂无独立官网，先复用 [GameFold 官网](https://gamefold.pages.dev)。以下各仓库分别维护独立产品线与基线。  
GamePad PS does not yet have a dedicated website; it currently uses the [GameFold website](https://gamefold.pages.dev). Each repository below maintains its own product line and baseline.

<div id="product-family"></div>

| 产品线 / Product | 仓库 / Repository | 官网 / Website |
|---|---|---|
| GameFold NS | [gamefold-ns-private](https://github.com/lavande16/gamefold-ns-private) | [gamefold.pages.dev](https://gamefold.pages.dev) |
| GamePad NS | [gamepad-ns-private](https://github.com/lavande16/gamepad-ns-private) | [gamefold.pages.dev](https://gamefold.pages.dev) |
| GameFold PC | [gamefold-pc-private](https://github.com/lavande16/gamefold-pc-private) | [gamefold.pages.dev](https://gamefold.pages.dev) |
| GamePad PC | [gamepad-pc-private](https://github.com/lavande16/gamepad-pc-private) | [gamefold.pages.dev](https://gamefold.pages.dev) |
| GameFold FC | [gamefold-fc-private](https://github.com/lavande16/gamefold-fc-private) | [game-fold.pages.dev](https://game-fold.pages.dev) |
| GamePad PS | [gamepad-ps-private](https://github.com/lavande16/gamepad-ps-private) | [gamefold.pages.dev](https://gamefold.pages.dev) |

---

<p align="center"><sub>本仓库为私有源码备份。版本号以 Release 与基线记录为准。｜Private source backup; use the Release and baseline records as the version source of truth.</sub></p>
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
