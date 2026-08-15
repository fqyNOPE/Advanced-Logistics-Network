# Logistics Network — 项目上下文

> 从 OpenClaw 迁移（会话 a439c241-7b35-45c8-b030-aff16cffecaf，2026-08-14）。
> 完整对话转录：`openclaw-archive/logistics-network-session-2026-08-14.md`

## 是什么

Mindustry 无人机物流 mod（进阶物流网络 Advanced Logistics Network，GitHub: `fqyNOPE/Advanced-Logistics-Network`）——传送带之外的第二套物品运输方案，Mindustry 版"异星工厂物流机器人"。**4 建筑 + 1 单位**：

| 建筑 | 尺寸 | 机制 |
|---|---|---|
| 物流基站 logistics-base | 3x3 | 耗电 1200/s（`consumePower(1200f/60f)`）；每 60s 产一架无人机（produceTime 3600 ticks），默认上限 1 架；电力水平影响无人机速度；过载加速；**拆基站 → 绑定无人机瞬间消失** |
| 供应点 logistics-supply-point（红箱） | 2x2 | 被动供料，无人机从这里取货；容量 320/种（`separateItemCapacity = true`，每种物品独立 320） |
| 请求点 logistics-request-point（蓝箱） | 2x2 | 配置单种物品，无人机自动补满；缺口阈值 40 滞回防抖（缺口 < 40 不补，避免来回横跳）；自动外输周围 |
| 储存点 logistics-storage-point（黄箱） | 2x2 | 多余物品存储；取货**优先黄箱**，没了才去红箱 |
| 物流无人机 logistics-bot | — | 飞行、速度快、无武器、载货 40、不可玩家控制、不被敌人优先锁定 |

**动态派单**：蓝箱缺口 40 → 派 1 架；80 → 2 架；120 → 3 架（按缺口弹性派单，不浪费无人机）。

## 构建

```powershell
# ⚠️ 必须用本地 Gradle 发行版，不要用 gradlew wrapper：
#    wrapper 要从 services.gradle.org 下载 9.4.1，被墙（.part 永远 0 字节），会卡死
C:\dsh workspace\build-tools\gradle-9.4.1\bin\gradle.bat clean jar
```

- 产物：`build/libs/LogisticsNetworkDesktop.jar`
- 依赖走本地镜像：`local-repo\Anuken\Mindustry\v159.7\{Mindustry-v159.7.jar, dependencies.jar}`（build.gradle 的 file:// maven 仓库），无网络依赖
- ⚠️ **build.gradle 的 local-repo URL 已修复**：`url = file('../../local-repo').toURI().toString()`——旧写法手工拼字符串，工作区路径含空格（`C:\dsh workspace`）时 URI 非法、构建直接失败
- **部署三处**（构建后都要同步）：
  1. 项目根 `LogisticsNetwork.jar`
  2. `AppData\Roaming\Mindustry\mods\LogisticsNetwork.jar`（游戏实际加载）
  3. `headless-run\config\mods\LogisticsNetwork.jar`（headless 测试用）

## 请求点优先级（0–9，默认 5）— 2026-08-14 新增

- **语义**：仅影响**同一物品**请求点之间的服务顺序；严格分档（高优先级永远先于低一档，距离只做同档决胜）；不抢占在途无人机；40 阈值/名额上限/黄箱优先取货等机制不变
- **UI**：配置界面物品选择表下方 0–9 滑条 + 当前值（bundle 键 `logistics.priority`）；建筑右上角数字角标（单色，仅 ≠5 时显示）
- **存档**：`RequestBuild` version 1→2，新增 1 字节 priority；老存档读默认 5（行为与之前完全一致）
- **实现**：`RequestPoint.RequestBuild.priority`；`LogisticsAI` 的 `findPickupJob`/`findDeliverJob` 选点改为 `优先级降序 → 距离升序` 比较器
- **headless 验证**：`HeadlessTest priority` 场景 PASS（远端 pri=9 先于近端 pri=0 被服务，远箱 280 近箱 0）；默认场景回归 PASS（产机/送货/断电停机/恢复供电全部正常）

## headless 测试环境（2026-08-14 迁移后恢复）

- **完整游戏 jar**：`mdt\Mindustry.jar`（86.6MB 官方 v159.7，从 `S:\mdt\` 复制；local-repo 的 15MB jar 无资源，跑不起来）
- 编译 classpath：先解压 `dependencies.jar` 并剔除内嵌 `.java`（两处 jar 都嵌了源码，直接 -cp 会让 javac 编译游戏源码并级联报错）→ `tmp\mdt-classpath`
- 编译：`javac -encoding UTF-8 -cp tmp\mdt-classpath -d headless-build headless\*.java`
- 运行：`cd headless-run; java -cp "C:\dsh workspace\mdt\Mindustry.jar;..\headless-build" headless.HeadlessTest [priority]`
- ⚠️ **测试布局电源位置**：3x3 基站以放置瓦片为**中心**（占地 [x-1,x+1]×[y-1,y+1]，`Tile.setBlock` 两遍法会先清掉占地内方块）。电源必须放 `(18,24)`（基站 20,24 的左侧紧邻）——旧布局 `(19,24)` 在基站占地内会被覆盖，基站永远没电
- 场景：`HeadlessTest`（默认，生产/断电/恢复）+ `HeadlessTest priority`（优先级派单）

## 双端包（桌面 + Android）

gradle 的 `deploy` 任务**不可用**：本机无 Android SDK（无 `ANDROID_HOME`、无 `platforms/android.jar`）。手动 d8 流程（2026-08-14 已验证通过）：

```powershell
$d8 = "C:\dsh workspace\build-tools\android\android-14\d8.bat"
$mi = "C:\dsh workspace\local-repo\Anuken\Mindustry\v159.7\Mindustry-v159.7.jar"
# 1. dex 化（classpath 只给 Mindustry jar！dependencies.jar 里的 jline 类会让 R8 8.2.2-dev 崩 NPE）
& $d8 --min-api 21 --output dexout --classpath $mi build\libs\LogisticsNetworkDesktop.jar
# 2. 合并：Desktop.jar（.class 给 JVM）+ classes.dex（给 Android）
Copy-Item build\libs\LogisticsNetworkDesktop.jar LogisticsNetwork.jar
jar uf LogisticsNetwork.jar -C dexout classes.dex
# 3. 验证：jar tf 应含 classes.dex + 14 个 .class + mod.json
```

## sfire-mod 兼容（当前未提交改动）

需求：**饱和火力**（`sfire-mod`，`AppData\Roaming\Mindustry\mods\饱和火力-4.0.5.jar`）加载时，物流基站造价改为 **120 silisteel**；未加载时保持原价（硅 60 + 超合金 120）。

实现（**未提交**，`git status` 显示 M：`README.md`、`gen-sprites.ps1`、`mod.json`、`src/logistics/LogisticsBlocks.java`）：

1. `mod.json` 加 `"softDependencies": ["sfire-mod"]` — 软依赖：缺失时 mod 照常加载（仅 mods 界面提示），存在时游戏保证 sfire-mod **先加载**（`Mods.resolveDependencies`），我们的 `loadContent()` 运行时 `silisteel` 已注册。
2. `LogisticsBlocks.applySFireCompat()`，在 `load()` 末尾（科技树创建**之前**）调用：
   - `Vars.mods.locateMod("sfire-mod") != null` 且 `Vars.content.item("silisteel") != null`（双保险，item 缺失静默跳过不崩）
   - 命中 → `requirements(Category.distribution, ItemStack.with(silisteel, 120))` + `researchCost` 同步（研究费用与建造一致）
   - 未命中 → 完全不动

当前部署的 `LogisticsNetwork.jar`（48,958 字节，双端包）已含此兼容，游戏下次启动生效。

## 相关资产

- `mindustry/` — Mindustry v8 源码仓库克隆（`Mods.java`/`ContentLoader.java` 等加载时序参考）
- `D:\Users\Administrator\.dsh\skills\mindustry` — mindustry 知识技能（构建、模组开发、注解生成等）
- `build-tools\` — gradle-9.4.1 发行版 + android-14（d8.bat）工具链
- 会话转录：`openclaw-archive/logistics-network-session-2026-08-14.md`
- 视频介绍分镜（2026-08-14 会话产出）：3 分钟主线 + 45s 竖屏引流方案，见转录末尾
