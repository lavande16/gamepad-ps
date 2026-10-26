# GamePad PS V0.4.1 固定基线

用户于 2026-10-21 明确批准。包名 com.gamepad.pspad，版本 0.4.1 / 8。

APK 为已安装且哈希一致的交付版本。208 项单元测试通过；最后的首次焦点保护改动已构建安装，但最终端到端日志确认未完成。此前候选的手柄路由、读档、退出保存和连续启动验证记录均保留；实体手柄完整验收及长时间实玩不在自动验证范围内。

源码恢复：先解压 ../GamePad-PS-V0.4/source/GamePad-PS-V0.4-source.tar.gz，再叠加 source/GamePad-PS-V0.4.1-source-delta.tar.gz；无删除项。两份归档的哈希见 BASELINE.json，差异源码逐文件校验已通过。V0.4 历史基线须保留，供完整源码恢复使用。

configuration 为默认配置材料，evidence 为原始验证记录；空的 final-runtime.log 表示最终日志未获取，不代表完成验证。未收录设备游戏、BIOS、DLL 或个人存档。

后续开发从此基线继续，不覆盖此目录。全局与现有游戏四项默认执行一次强制迁移，后续手动修改仍保留。
