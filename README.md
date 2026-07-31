<div align="center">

# ⚡ McKaiFu 开服大师 · Android

**Run a Minecraft Server on Your Phone — No Root, No PC**
**在手机上运行 Minecraft 服务器 —— 无需 Root,无需电脑**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![minSdk](https://img.shields.io/badge/minSdk-26-orange?style=for-the-badge)](https://developer.android.com/studio)
[![Android 15](https://img.shields.io/badge/Android-15-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![Paper 1.20.4](https://img.shields.io/badge/Paper%201.20.4-%E2%9C%94%20%E7%9C%9F%E6%9C%BA%E9%AA%8C%E8%AF%81-green?style=for-the-badge)](https://papermc.io)
[![License](https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge)](LICENSE)

**English** | [**中文**](#中文-zh) · 一个仓库，双语文档。One repo, bilingual docs.

> The Android app that embeds a full **OpenJDK 17 (bionic)** into its own process and runs **Paper/Spigot/Bedrock servers** directly on your phone — verified working on a real **Xiaomi phone (Android 15)** with **Paper 1.20.4** booting to `Done (11.021s)!`.

---

## 中文 (ZH)

### 🚀 这是什么?

**McKaiFu(开服大师)** 是一个安卓应用,让任何普通手机用户都能轻松开一个 Minecraft 服务器。

国产系统(尤其是 MIUI/鸿蒙)用 **SELinux** 拦截常规的 `exec` 系统调用,导致普通的 `java -jar` 方式根本无法在手机上启动服务端。本项目给出了一套业界少见的大胆解法:

> 🧠 **进程内启动 JVM(In-process JVM bootstrap)**
> 通过 JNI `dlopen` 加载安卓原生 **bionic 版 OpenJDK 17**(来自 PojavLauncher / Zalith),
> 直接调用 `JLI_Launch()` 让整个 JVM 运行在 **App 进程内部**——
> **完全不走 `exec` 路径,绕开 SELinux 封锁,无需 Root、无需特权**。

✅ **真机验证**:小米手机(Android 15, MIUI)上 Paper 1.20.4 完整启动
`Done (11.021s)!`,局域网内用 MC 客户端可正常连接联机!

### ✨ 功能特性

| 功能 | 说明 |
|---|---|
| 🧩 **多核心支持** | Paper / Purpur / Pufferfish / Spigot / 原版 / Nukkit / PocketMine-MP |
| 📦 **内置 Java 17** | 开箱即用,自动解压、自动适配;也支持外部 JRE |
| 📟 **实时控制台** | 日志高亮、搜索、过滤、自动滚动,手机上直接输入命令 |
| 📊 **服务器监控** | TPS / 内存 / CPU / 玩家实时看板,图表展示 |
| 🛠 **完整管理** | 玩家 / 封禁 / 世界 / 插件商店 / 文件管理 / 配置编辑器 / 定时任务 |
| 🌐 **内网穿透** | Playit.gg / Ngrok / NATAPP / 樱花frp,含**国内节点**(华北/华东/华南/西部) |
| 🗺 **Web 地图** | 可视化服务器地图 |
| 🔌 **Geyser** | 基岩版客户端互通 |
| 💬 **聊天** | 服务器内聊天界面 |

### 🧩 各核心可加入的客户端版本

| 核心 Core | 类型 Type | 可加入客户端 Client Version |
|---|---|---|
| Paper / Purpur / Pufferfish / Spigot / 原版 | Java 版 | **Java 版同版本客户端**(如服务端 1.20.4 → Java 版 1.20.4) |
| Nukkit | 基岩版 Bedrock | **基岩版 1.20.x 客户端** |
| PocketMine-MP | 基岩版 Bedrock | **基岩版 1.20.x 客户端** |

### 🏗 技术亮点

#### 为什么能在手机上跑 JVM?
1. 服务端本质是 `java -jar paper.jar`,需要完整 JVM;
2. 安卓有 SELinux,常规 `exec` 被 MIUI 等国产系统拦截;
3. 本项目把 **bionic libc 版 OpenJDK** 直接打进 APK assets(压缩后约 20MB);
4. 运行时通过 JNI 把 JRE 全部 `.so` 按依赖优先级逐个 `dlopen`;
5. `JLI_Launch()` 进程内启动,stdout/stdin 重定向到管道 → 实时喂给 UI 控制台。

```
┌─────────────── APK ───────────────┐
│  JRE 17 (tar.xz, bionic)   → assets │
│  ServerEngine 解压 / 校验           │
│  dlopen(jvm/libs/*.so)             │
│  JLI_Launch(...)  ← 进程内启动     │
│        ↓                           │
│  Paper 服务端完整运行              │
└────────────────────────────────────┘
```

#### 攻克的经典难题 (Hard-won fixes)
- 🔑 **Paper 属性大小写陷阱**:`Boolean.getBoolean("Paper.IgnoreJavaVersion")` 区分大小写,`-Dpaper.` 无效,必须 `-DPaper.IgnoreJavaVersion=true`
- 🤝 **全核心 EULA 自动同意**:`eula.txt` 恒为 `eula=true`,覆盖 Paper/Spigot/Nukkit 等
- 🧷 **Android 15 tagged pointers 崩溃**:manifest 加 `android:allowNativeHeapPointerTagging="false"`
- 📡 **底部导航"控制台空白"bug**:模式路由 `console/{serverId}` 传字面量,改为 `selectedServerId` 构造真实路由

### 🛠 技术栈 Tech Stack

| 层 Layer | 技术 Tech |
|---|---|
| 语言 Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 导航 Navigation | Navigation Compose |
| JNI | NDK C (`vmlauncher.c`) |
| 运行时 Runtime | bionic OpenJDK 17 (Pojav/Zalith) |
| 数据 Data | Gson + SharedPreferences |
| 网络 Network | OkHttp / WebSocket |

### 📦 构建 Build

需要 JDK 17+ 与 Android SDK (compileSdk 34)。

```bash
# Debug
gradlew assembleDebug
# Release(有 local.properties 且配置 keystore 时自动签名)
gradlew assembleRelease
```

APK 输出: `app/build/outputs/apk/debug/`、`app/build/outputs/apk/release/`

> 提示:仓库不含 keystore。要签名请在 `local.properties` 配置 `keystorePath`、`keystorePassword`、`keyAlias`;未配置时自动退回 debug 签名。

### 📸 截图 Screenshots

> 即将补充 Coming soon…

### 🗺 路线图 Roadmap

- [x] 进程内 JVM + Paper 真机启动 (In-process JVM + Paper on real device)
- [x] 多核心支持 (Multi-core support)
- [ ] 打包 natapp/frpc 实现真正的国内隧道 (Bundle domestic tunnel clients)
- [ ] 在线皮肤 / 正版验证 (Online skin / auth)
- [ ] 插件市场云端化 (Cloud plugin store)

---

## English (EN)

### 🚀 What Is This?

**McKaiFu (开服大师 / "Server Master")** is an **Android app** that lets anyone run a Minecraft server right on their phone.

Chinese Android systems (especially **MIUI/HyperOS**) block the conventional `exec()` path via **SELinux**, so a normal `java -jar` launch never works on a phone. This project uses an unusual and powerful approach:

> 🧠 **In-process JVM bootstrap**
> We embed a native **bionic-libc OpenJDK 17** (from PojavLauncher / Zalith) into the APK assets.
> At runtime the app `dlopen()`s every JRE `.so` via JNI and calls **`JLI_Launch()`** so the
> entire JVM runs **inside the app process** — no `exec`, no SELinux trip, **no root required**.

✅ **Verified on real hardware**: Paper 1.20.4 fully boots on a Xiaomi phone (Android 15) —
`Done (11.021s)!` — and joins over LAN are confirmed working.

### ✨ Features

| Feature | Description |
|---|---|
| 🧩 **Multi-core support** | Paper / Purpur / Pufferfish / Spigot / Vanilla / Nukkit / PocketMine-MP |
| 📦 **Bundled Java 17** | Extracted & patched automatically; external JRE also supported |
| 📟 **Live console** | Highlighted logs, search, filter, auto-scroll, send commands from phone |
| 📊 **Monitoring** | TPS / memory / CPU / player dashboards with charts |
| 🛠 **Full management** | Players, bans, worlds, plugin store, file manager, config editor, scheduled tasks |
| 🌐 **Tunnels** | Playit.gg / Ngrok / NATAPP / SakuraFRP, incl. **China nodes** |
| 🗺 **Web map** | Visualize your world |
| 🔌 **Geyser** | Bedrock client interoperability |
| 💬 **Chat** | In-server chat UI |

### 🧩 Client Compatibility per Core

| Core | Type | Joinable Client |
|---|---|---|
| Paper / Purpur / Pufferfish / Spigot / Vanilla | Java | **Same-version Java client** (server 1.20.4 → Java 1.20.4) |
| Nukkit | Bedrock | **Bedrock 1.20.x client** |
| PocketMine-MP | Bedrock | **Bedrock 1.20.x client** |

### 🏗 How It Works

1. A Minecraft server needs a full JVM (`java -jar paper.jar`);
2. Android's SELinux (esp. MIUI) blocks normal `exec`;
3. We ship a **bionic-libc OpenJDK** in the APK assets (~20MB compressed);
4. At startup every JRE `.so` is loaded via JNI `dlopen` in dependency order;
5. `JLI_Launch()` boots the JVM **in-process**; stdout/stdin are piped back to the app's console UI.

### 📦 Build

JDK 17+ and Android SDK (compileSdk 34) required.

```bash
gradlew assembleDebug     # debug
gradlew assembleRelease   # release (auto-signed if local.properties has keystore)
```

### 🙏 Credits

- [PojavLauncher](https://github.com/PojavLauncherTeam) / **Zalith** — bionic OpenJDK builds
- [PaperMC](https://papermc.io) / SpigotMC / Purpur — awesome server cores
- Playit.gg / Ngrok / NATAPP / 樱花frp — tunnel services

### ⚖️ License & Disclaimer

Bundled JRE is based on OpenJDK (GPLv2 + CE). Please respect the Minecraft EULA and each service's ToS.
This project is for learning and communication. **Not affiliated with Mojang or Microsoft.**

---

<div align="center">

⭐ If this project helps you, give it a star! ⭐
如果这个项目帮到了你,欢迎点亮 Star!

</div>
