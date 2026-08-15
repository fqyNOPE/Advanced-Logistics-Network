# Logistics Network — Mindustry Java Mod

Factorio 风格的物流网络系统（Logistics Network）。代码完全由ai编写。

## 内容（4 个建筑 + 1 个单位）

| 建筑 | 作用 |
|---|---|
| **物流基站** (logistics-base, 3x3) | 贴图采用**原版单位物流装载点（Unit Cargo Loader）**。耗电 **1200/s**。每 **60 秒**生产一架物流无人机，默认最多 1 架（可改 `maxBots`）。**无人机与生产它的基站绑定**：拆基站对应无人机全部消失；**基站供电水平直接影响对应无人机的移动速度**（满电全速、低电慢速、断电停飞）。**超频会使基站对应机器人移速加快**； |
| **供应点** (logistics-supply-point, 2x2) | 被动供应：无人机从这里取货。传送带/玩家可以直接放入物品。容量 240。 |
| **请求点** (logistics-request-point, 2x2) | 贴图采用**原版单位物流卸载点（Unit Cargo Unload Point）**，并**像原版一样用所选物品的颜色显示在方块上**，选中的物品会被无人机持续运来直到装满（容量 240）。**会自动向周围输出物品** |
| **储存点** (logistics-storage-point, 2x2) | 储存：无人机把多余的物品存进来；有请求时优先从这里取货。容量 240。 |
| **物流无人机** (logistics-bot) | 飞行单位，速度快、无武器，可携带 40 个物品。由基站生成，不可被玩家操控/逻辑控制，不会被敌人优先锁定（但仍会被范围伤害打死）。 |

## 配置项（Java 源码内可调）

- `LogisticsBase.maxBots` — 每座基站最多无人机数（默认 **1**）
- `LogisticsBase.produceTime` — 生产一架所需 tick（默认 3600 = **60 秒**）
- `LogisticsAI.retargetTime` / `moveRange` / `transferRange` — AI 寻路参数
- `LogisticsAI.requestThreshold` — 蓝箱需求阈值（默认 40，缺口≥该值才补货）

## sfire-mod（饱和火力）兼容

当 **sfire-mod**（`饱和火力`，mod 名 `sfire-mod`）已加载时，物流基站的造价自动改为 **120 silisteel**（研究费用同步）；未加载 sfire-mod 时保持原造价（硅 60 + 超合金 120）不变。

实现方式：`mod.json` 声明 `softDependencies: ["sfire-mod"]`（软依赖，缺失时本 mod 照常加载，仅提示），`LogisticsBlocks.applySFireCompat()` 在内容加载阶段检测 sfire-mod 是否加载、`silisteel` 物品是否注册，命中则改写 `requirements`/`researchCost`。
