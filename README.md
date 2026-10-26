# GamePad PS

> **把 PS2 游戏库，带进随身的 Android 游戏空间。**  
> Bring your PS2 library into a handheld Android play space.

<p align="center"><a href="https://gamefold.pages.dev"><img src="https://gamefold.pages.dev/assets/product/fold-gameplay.webp" alt="GameFold 折叠屏游戏体验视觉" width="88%"></a></p>
<p align="center"><sub>GameFold 产品家族视觉｜GameFold family artwork · GamePad PS 暂无独立官网，暂用家族官网介绍品牌。</sub></p>

<p align="center"><a href="https://gamefold.pages.dev">产品官网 / Official site</a> · <a href="https://github.com/lavande16/gamepad-ps-private/releases/tag/gamepad-ps-v0.4.1-8-baseline">下载 V0.4.1 / Download</a></p>

## 当前基线 · Current baseline

| 项目 | 信息 |
|---|---|
| 基线版本 | **GamePad PS V0.4.1 · versionCode 8** |
| Android 包名 | **com.gamepad.pspad** |
| 方向与显示 | 横屏游玩；默认 4× 渲染、LGFS 2×、Stretch 全屏、OSD 关闭 |
| 存储目录 | **GamePadPS** |
| 下载 | [GitHub Release：V0.4.1 / 8](https://github.com/lavande16/gamepad-ps-private/releases/tag/gamepad-ps-v0.4.1-8-baseline) |

## 为玩家准备的体验 · Made for players

- **整洁的游戏入口：**以卡带式游戏库进入 PS2 游戏，并支持自动装配与自动存档读档流程。  
  **A focused library:** launch from a cartridge-inspired collection with automatic game setup and save/load flow.
- **横屏掌机布局：**控制与内容围绕横屏握持方式安排，适合随身游玩。  
  **Landscape handheld play:** controls and content are arranged for a comfortable handheld grip.
- **首次设置简单，之后由你掌控：**提供统一的图像与显示默认值；初始化后保留玩家后续的设置和进度。  
  **Simple defaults, your preferences:** graphics and display defaults are applied during setup, while later changes and progress are preserved.
- **兼容多种游戏目录：**支持 `game/game.iso`、`game/游戏名/game.iso` 和旧版 `game/游戏名/ISO/game.iso` 结构。  
  **Flexible game folders:** supported layouts include `game/game.iso`, `game/Title/game.iso` and the legacy `game/Title/ISO/game.iso` structure.

> 本项目不附带游戏 ROM、BIOS 或商业游戏素材。请只导入你有权使用的文件。GamePad PS 为独立爱好项目，与 PlayStation、Sony 或其他权利人没有隶属或授权关系。  
> Game files, BIOS files and commercial game assets are not included. Import only files you are entitled to use. This independent project is not affiliated with PlayStation, Sony or other rights holders.

## 从基线继续开发 · Continue from the baseline

本仓库保留 V0.4.1 源码基线、运行时、默认配置与构建工具。建议从 [实现计划](IMPLEMENTATION_PLAN.md)、[源码边界](SOURCE_LOCK.md)、[素材导入规则](MATERIALS.md)、[复用盘点](REUSE_INVENTORY.md) 和 [后续迁移说明](NEXT_PACKAGE_MIGRATION.md) 开始。APK 与版本号以 [V0.4.1 Release](https://github.com/lavande16/gamepad-ps-private/releases/tag/gamepad-ps-v0.4.1-8-baseline) 为准。

This repository preserves the V0.4.1 source baseline, runtime, defaults and build tools. Start with the [implementation plan](IMPLEMENTATION_PLAN.md), [source boundaries](SOURCE_LOCK.md), [materials guide](MATERIALS.md), [reuse inventory](REUSE_INVENTORY.md) and [migration notes](NEXT_PACKAGE_MIGRATION.md). The [V0.4.1 Release](https://github.com/lavande16/gamepad-ps-private/releases/tag/gamepad-ps-v0.4.1-8-baseline) is the version source of truth.

Build command / 构建命令: `bash tools/build_runtime.sh`

## 官网与关联产品 · Website & related products

GamePad PS 暂无独立官网，现阶段先复用 [GameFold 官网](https://gamefold.pages.dev)。下面列出六条产品线各自的私有仓库与官网入口。  
GamePad PS does not yet have a dedicated website, so it currently uses the [GameFold website](https://gamefold.pages.dev). The table links all six product lines to their private repositories and websites.

| 产品线 / Product | 仓库 / Repository | 官网 / Website |
|---|---|---|
| GameFold NS | [gamefold-ns-private](https://github.com/lavande16/gamefold-ns-private) | [GameFold](https://gamefold.pages.dev) |
| GamePad NS | [gamepad-ns-private](https://github.com/lavande16/gamepad-ns-private) | [GameFold](https://gamefold.pages.dev) |
| GameFold PC | [gamefold-pc-private](https://github.com/lavande16/gamefold-pc-private) | [GameFold](https://gamefold.pages.dev) |
| GamePad PC | [gamepad-pc-private](https://github.com/lavande16/gamepad-pc-private) | [GameFold](https://gamefold.pages.dev) |
| GameFold FC | [gamefold-fc-private](https://github.com/lavande16/gamefold-fc-private) | [GameFold FC](https://game-fold.pages.dev) |
| GamePad PS | [gamepad-ps-private](https://github.com/lavande16/gamepad-ps-private) | [GameFold](https://gamefold.pages.dev) |

---

<p align="center"><sub>私有源码与基线备份 · Private source and baseline backup · 游戏文件由使用者自行提供 · Game files are supplied by the user</sub></p>
