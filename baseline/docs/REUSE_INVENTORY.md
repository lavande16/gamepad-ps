# GamePad PS 全范围复用盘点

## 范围与结论

本次不是根据首页、按钮、卡带、设置页几个例子制作复刻清单。固定输入覆盖：

- NS V1.8 / 10030 对应提交的整个 Android 工程：736 个文件，其中代码文件251个、Android资源453个；包括页面、组件、布局、状态、导航和公共逻辑。
- PC V2.4 / 10033 对应提交的整个 main Java 与资源目录：1365个文件，作为公共实现的补充。
- 已识别126个与NS内容完全相同的PC文件。相同哈希仅表示字节相同，不等于126项独立功能，也不代表全部都应迁入。
- 每个文件都记录来源提交、原路径、大小、SHA-256、功能类别、复用模式、资源引用及已识别的后端导入依赖。
- 四张NS已接受版本的截图作为视觉参考，不将其当作PS版本的运行验证。

完整文件清单在 `stage1/locks/ns-reuse-inventory.json` 和 `pc-reuse-inventory.json`。这是完整文件级盘点与功能级审查，不声称已经逐个编译接入所有组件；真正的编译依赖和运行行为在后续阶段验证。

## 功能级复盘

下表中的路径均指固定NS工程 `src/android/app/src/main/` 下的原文件或文件组。不得只移入图片而重新实现已有布局与行为。

| 能力 | 已有实现和材料 | 开发时的处理 |
|---|---|---|
| 整体壳体与画面开口 | `views/GameFoldShellLayout`、`GameFoldViewportLayout`、`GameFoldViewportCalculator`及controller资源 | 保留绘制与几何代码，替换运行事件接收端 |
| 首页和游戏库 | `ui/main/MainActivity`、`ui/GamesFragment`、`layout/fragment_games.xml`及方向变体 | 保留结构、位置和交互；游戏数据绑定PS2模型 |
| 卡带与封面 | `adapters/GameAdapter`、`card_game_grid*`、`card_game_list`、`card_game_carousel`、卡带边框与添加游戏素材 | 保留卡带造型、封面裁切、尺寸及焦点效果；不改成新设计的光盘界面 |
| 顶栏、HOME与入口按钮 | 首页布局、`pixel_header*`、`pixel_icon_*`、HOME素材、`GamePadNsHomeGeometry` | 直接复用适用结构和素材；替换名称与业务动作 |
| 设置首页 | `HomeSettingsFragment`、`HomeSettingAdapter`、`fragment_home_settings.xml` | 保留页面容器、列表结构和内边距，绑定PS2类别 |
| 设置子页与导航 | `SettingsActivity`、`SettingsFragment`、`SettingsSubscreenActivity`、`SettingsDialogFragment`和settings navigation | 保留已有页面结构；剥离Switch设置枚举和原生读写绑定 |
| 设置行和控件 | `SettingsAdapter`、`ui/viewholder/*`、`list_item_setting*`、`list_item_settings_header` | 开关、单选、滑块、数值、路径、分组等优先直接迁入；新PS2选项映射到现有控件 |
| 设置间距与缩进 | `values/pixel_dimens.xml`、`pixel_styles.xml`、`pixel_peripheral_styles.xml`、现有布局属性 | 原值保留，不按截图重新估算；例如设置首页内容横向内边距48dp来自原XML，不能误当所有页面统一缩进 |
| 全局视觉系统 | `pixel_colors.xml`、`pixel_attrs.xml`、`themes.xml`、`styles.xml`、品牌资源和pixel drawable | 整体保留主题及资源引用关系；只修改产品身份和不适用文案 |
| 字形和文字表现 | `PixelNumberView`、`PixelWordmarkView`及现有文字样式 | 保留现有字形绘制、排版和数字显示实现 |
| 按钮状态与焦点 | `pixel_button*`、`pixel_list_item*`、卡带selector和focus ring | 普通、按下、聚焦、禁用等状态沿用原定义 |
| 弹窗和页面容器 | `StageDialogFragment`、`GameFoldDialogSizing`、现有dialog布局与主题 | 保留容器、尺寸、边距和按钮规则，换入PS2内容 |
| 首次启动与导入进度 | `GameFoldAssemblyActivity`、启动/加载布局、进度条和状态资源 | 直接复用页面与进度反馈；步骤内容连接PS2装配器 |
| 空库、缺失、错误和成功状态 | `pixel_state_*`、加载/错误面板、提示类Fragment | 使用现成状态组件；新增文案，不另画状态页 |
| 菜单、返回和焦点导航 | `GameFoldUiNavigationBridge`、相关host接口、现有navigation和菜单 | 保留可复用导航规则，替换Switch业务目标 |
| 触控按钮与双摇杆 | `GameFoldControllerInput`、shell触控处理、按下态素材 | 保留多指、命中、摇杆与滑动逻辑；语义事件转接ARMSX2 |
| 振动、显隐与交互反馈 | `GameFoldControllerInput`中的反馈实现、`GamePadControllerVisibilityState`及shell状态 | 保留已有体验；暂停和焦点丢失时补入PS2输入复位 |
| 布局、自适应和系统栏 | `GameFoldViewport*`、`GamePadNsWindowPolicy`、`InsetsHelper`、`FullscreenHelper`及相关View | 直接复用独立策略，保留现成尺寸分支，连接新的Surface宿主 |
| 分辨率切换 | `GamePadNsDisplaySizeController`、`GamePadNsDisplayMode`及配套几何 | 沿用已有开关与模式；首次启动不擅自改变系统尺寸。PC独立实现作为补充，不重写同类功能 |
| 游戏属性与信息页 | `GamePropertiesFragment/Adapter`、`GameInfoFragment`和属性布局 | 保留页面与组件，替换游戏序列号、CRC和PS2属性数据 |
| LSFG管理与每游戏设置 | `LosslessManagerFragment`、装配材料发现和属性设置流程 | 保留可复用界面与操作顺序；实际加载和生成仍调用ARMSX2 |
| 文件目录、选择和授权 | 文件/目录工具、Folder组件、装配预检查与状态记录 | 复用通用文件和授权代码；根目录改为`GamePad PS`，正确处理空格 |
| 增量检查和装配状态 | `GameFoldAssembler`、`GameFoldAssemblyState`、`GameFoldPackage` | 保留状态与指纹思路；以PS2策略替换keys、firmware、NAND和DLC操作 |
| 存档管理、导入导出 | 游戏属性/文件工具/装配导入流程 | 保留适用容器和文件操作，改为PS2记忆卡；运行卡存在时不因材料变化自动覆盖 |
| 驱动管理 | Driver系列页面、选择列表和加载弹窗 | 先列入候选复用；绑定ARMSX2驱动能力，不把NS的T26/T28作为PS2强制材料 |
| 关于、许可、系统信息、语言等工具页 | Licenses、SystemInfo、语言和相关列表/弹窗组件 | 保留通用页面结构；内容与后端换为GamePad PS及实际依赖 |
| 集合、列表与基础控件 | Abstract系列adapter、SpacingItemDecoration、AutofitGridLayoutManager、SquareImageView等 | 直接使用兼容组件，避免各页重复实现 |
| 暂停、后台和恢复 | NS生命周期流程与ARMSX2运行接口 | 保留产品操作约定；VM、Surface、音频及输入清理由ARMSX2接管，不能搬入Switch核心调用 |

## 明确不整套移植的部分

- Switch密钥、固件安装、NAND、Update/DLC、Amiibo、Applet、UltraCam及Switch补丁执行逻辑不进入PS2运行层；其中通用列表或弹窗仍可复用。
- PC的Wine、FEX、XServer、Windows容器和PC输入后端不进入新产品。
- 外接屏、后屏、背触等功能不进入本次首版；相关文件保留在参考归档中，不代表启用。
- NS/PC中存在的网络、用户配置等页面不自动扩大PS首版范围；需要的公共组件可以提取。

## 允许修改和禁止重复开发的边界

允许：包名和资源命名空间调整、依赖补齐、数据模型适配、设置类型映射、存储根路径和模拟器桥接、平台不适用内容删除。

禁止仅为复刻外观而：重新画卡带或按钮、重新搭相同页面、重新估算内边距、把原XML/View全部重写成Compose、凭感觉调整已确认的触控几何或重新做焦点状态。

ARMSX2采用的UI框架与NS不一致时，应先用AndroidView、Fragment或独立Activity等桥接方式承载原组件；只有真实编译或运行障碍证明无法适配时，才记录最小替代实现。接口不同本身不构成重写整页的理由。

## 后续每个组件的迁移记录

每项实际迁移都记录：原项目和提交、原文件路径及哈希、目标文件、复用方式、修改点、替换的后端绑定、截图或交互验证结果。新写项额外记录检查过的现有实现、不可复用原因和所使用的原组件。

先验证已有组件宿主和依赖，再移入同类页面；出现差异时定位具体绑定或布局差异，不创建一系列独立复刻版本试画。原始参考归档保持不变。

## 固定清单与验证边界

- NS全Android归档：`stage1/references/ns-v1.8-10030-android.tar`。
- PC全前端参考归档：`stage1/references/pc-v2.4-10033-frontend.tar`。
- 逐文件清单与依赖线索：`stage1/locks/*-reuse-inventory.json`。
- 相同文件去重参考：`stage1/locks/identical-ns-pc-files.json`。
- 统计与范围：`stage1/locks/reuse-summary.json`。
- 视觉参考及校验：`stage1/locks/visual-baseline.json`。
- 可重跑文件盘点：`python3 tools/audit_reuse.py`。

文件标签是接入前的候选分类，不是自动批准批量移入所有文件，也不代表编译验证已通过。实际直接复用程度以迁移记录和构建结果为证据。
