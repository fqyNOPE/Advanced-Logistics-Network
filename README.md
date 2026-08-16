# Logistics Network — Mindustry Java Mod

更高效，更具泛用性的物流网络系统
## 代码部分由ai编写。
## 内容
| 建筑 | 作用 |
|---|---|
| **物流基站** (logistics-base, 3x3) | 贴图采用**原版单位物流装载点（Unit Cargo Loader）**。耗电 **1200/s**。每 **60 秒**生产一架物流无人机，默认最多 1 架（可改 `maxBots`）。**无人机与生产它的基站绑定**：拆基站对应无人机全部消失；**基站供电水平直接影响对应无人机的移动速度**。**超频会使基站对应机器人移速加快**； |
| **供应点** (logistics-supply-point, 2x2) | 被动供应：无人机从这里取货。传送带/玩家可以直接放入物品。容量 400。 |
| **请求点** (logistics-request-point, 2x2) | 贴图采用**原版单位物流卸载点（Unit Cargo Unload Point）**，并**像原版一样用所选物品的颜色显示在方块上**，选中的物品会被无人机持续运来直到装满（容量 400，可调为 10%–100%）。**只接受无人机/玩家投递，不接受传送带输入**。**会自动向周围输出物品**。**可以设置优先级（1–9）**，货物会优先运往高优先级请求点 |
| **储存点** (logistics-storage-point, 2x2) | 储存：无人机把多余的物品存进来；有请求时优先从这里取货。容量 400。 |
| **物流无人机** (logistics-bot) | 飞行单位，速度快（移速 2）、无武器，可携带 40 个物品。由基站生成，不可被玩家操控/逻辑控制，不会被敌人优先锁定（但仍会被范围伤害打死）。 |

## 配置项（Java 源码内可调）

- `LogisticsBase.maxBots` — 每座基站最多无人机数（默认 **1**）
- `LogisticsBase.produceTime` — 生产一架所需 tick（默认 3600 = **60 秒**）
- `LogisticsAI.retargetTime` / `moveRange` / `transferRange` — AI 寻路参数
- `LogisticsAI.requestThreshold` — 蓝箱需求阈值（默认 40，缺口≥该值才补货）
