# McKaiFu 开服大师 (Android)

[English](#english) | [中文](#中文)

在你的手机上开一个 Minecraft 服务器,不需要 Root,不需要电脑,不需要懂 Java。

这个项目的核心想法很简单:**安卓系统(尤其 MIUI)会用 SELinux 拦截 `exec`**,所以直接在手机上执行 `java -jar` 会失败。我们绕开了这条路——把一份针对安卓原生库编译的 OpenJDK 17 打包进 APK,启动时通过 JNI 把 JVM 的 `.so` 逐个加载进 App 进程,直接调用 `JLI_Launch()`,让整个服务器跑在 App 里面。没有 `exec`,自然就没有 SELinux 的问题。

已经在小米手机(Android 15)上跑通了 Paper 1.20.4,启动到 `Done (11.021s)!`,局域网内可以正常联机。

## 支持的核心

| 核心 | 客户端 |
|---|---|
| Paper / Purpur / Pufferfish / Spigot / 原版 | Java 版同版本客户端 |
| Nukkit / PocketMine-MP | 基岩版 1.20.x 客户端 |

## 功能

- 多核心支持,开箱即用
- 内置 Java 17,自动解压配置
- 实时控制台:看日志、输命令
- 服务器监控:TPS、内存、CPU
- 玩家、封禁、世界、插件、文件、配置、定时任务管理
- 内网穿透:Playit.gg / Ngrok / NATAPP / 樱花frp,含国内节点

## 构建

需要 JDK 17+ 和 Android SDK。

```bash
gradlew assembleDebug
```

输出在 `app/build/outputs/apk/`。Release 有签名需求时,在 `local.properties` 配 keystore 即可。

## 致谢

- PojavLauncher / Zalith 提供的 bionic 版 OpenJDK
- PaperMC、SpigotMC、Purpur 等服务端核心

请遵守 Minecraft EULA。本项目与 Mojang / Microsoft 无关。

---

## English

Run a Minecraft server on your phone. No root, no PC, no Java knowledge needed.

The core idea: Android systems (especially MIUI) block `exec` via SELinux, so running
`java -jar` on a phone normally fails. Instead, this app bundles an OpenJDK 17 built for
Android's native libc. At startup it `dlopen`s every JVM `.so` into the app process via JNI
and calls `JLI_Launch()` directly — the whole server runs inside the app, never touching
`exec`, so SELinux never comes into play.

Verified on a Xiaomi phone (Android 15): Paper 1.20.4 boots to `Done (11.021s)!` and joins over LAN work.

### Cores

- Paper / Purpur / Pufferfish / Spigot / Vanilla — same-version Java client
- Nukkit / PocketMine-MP — Bedrock 1.20.x client

### Features

- Multi-core support, works out of the box
- Bundled Java 17, auto-extracted and configured
- Live console: view logs, send commands
- Monitoring: TPS, memory, CPU
- Manage players, bans, worlds, plugins, files, config, scheduled tasks
- Tunnels: Playit.gg / Ngrok / NATAPP / SakuraFRP, incl. China nodes

### Build

JDK 17+ and Android SDK required.

```bash
gradlew assembleDebug
```

Output in `app/build/outputs/apk/`. For signed releases, configure a keystore in `local.properties`.

### Credits

- PojavLauncher / Zalith for the bionic OpenJDK builds
- PaperMC, SpigotMC, Purpur for the server cores

Respect the Minecraft EULA. Not affiliated with Mojang or Microsoft.
