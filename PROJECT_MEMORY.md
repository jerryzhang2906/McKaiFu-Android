# McKaiFu 项目记忆 (Project Memory)

> 本文档记录项目的目标、架构、关键进展、踩坑经验、验证方法和当前状态，
> 供后续会话快速恢复上下文。最后更新 2026-08-01 (视频制作进行中: 脚本已完成 docs/video_script.md, 录屏工具就绪)

---

## 一、项目概述

**项目名**: McKaiFu(开服大师)
**位置**: `I:\mckaifu`
**本质**: 安卓 App,让普通小米/红米手机 **不用 root** 运行 Minecraft 服务端(Paper 等)

**核心目标(已完成)**: 
绕过 MIUI SELinux 对 `exec` 的封锁,让 JVM 在 app 进程内通过 JNI 直接启动。
实测 Paper 1.20.4 已能在真机完整启动:`Done (11.021s)!`。

**支持核心**: PaperMC / Purpur / Pufferfish / Spigot / 原版 / Nukkit / PocketMine-MP

---

## 二、技术架构

### bionic JRE 进程内启动流程(核心成就)
```
assets 内 tar.xz (Zalith/Pojav 的 bionic JRE 17)
    ↓ JavaRuntimeManager 解压 + patch 到 filesDir
    ↓ ServerEngine.startServerBionic()
    ├─ loadJreLibraries(): dlopen 全部 lib/*.so (按优先级排序)
    ├─ VMLauncher.createPipe() × 2 (stdout / stdin)
    ├─ VMLauncher.setStdio(inReadFd, outWriteFd)
    ├─ VMLauncher.chdir(serverDir)
    └─ VMLauncher.launchJVM(args)  →  native 里 JLI_Launch()
         → JVM 在 app 进程内运行,不经过 exec,绕开 SELinux
```

### 关键文件
| 文件 | 作用 |
|---|---|
| `app/src/main/cpp/vmlauncher.c` | NDK native:dlopen/pipe/readFd/writeFd/closeFd/JLI_Launch/chdir |
| `app/src/main/java/com/mckaifu/app/jni/VMLauncher.kt` | JNI 封装(注意 readFd 等签名是 FileDescriptor,非 Long) |
| `app/src/main/java/com/mckaifu/app/service/JavaRuntimeManager.kt` | JRE 下载/解压/ensureRuntime/打 patch |
| `app/src/main/java/com/mckaifu/app/service/ServerEngine.kt` | 启动/管道读取/看门狗/停止 |
| `app/src/main/java/com/mckaifu/app/service/TunnelService.kt` | 内网穿透(Playit/Ngrok/NATAPP/樱花frp);CUSTOM/SAKURA 自动解析配置文件公网地址 |
| `app/src/main/java/com/mckaifu/app/service/TunnelBinaryManager.kt` | 内置隧道二进制(playit/ngrok/frpc):按类型+ABI 从 assets/tunnel/<type>/ 解压到 files/bin + chmod 755 |
| `app/src/main/java/com/mckaifu/app/data/repository/ServerRepository.kt` | 单例仓库:服务器列表/控制台消息/selectedServerId |
| `app/src/main/java/com/mckaifu/app/util/AppPrefs.kt` | SharedPreferences:onboarding_done 等 |
| `app/src/main/java/com/mckaifu/app/util/CompatibilityHelper.kt` | 兼容性 + 客户端版本提示 |

---

## 三、已攻克的坑(务必记住)

1. **`-DPaper.IgnoreJavaVersion=true` 大小写必须正确**(`Paper.` 大写 P)。
   Paper 的 `Main.class` 用 `Boolean.getBoolean("Paper.IgnoreJavaVersion")` 检查,
   系统属性**区分大小写**。反编译命令见下。
   `-Djava.version=...` 无效——HotSpot 编译期内建属性会覆盖 -D。
   release 文件补丁也无效,`java.version` 是编译期写死的(`17.0.10-internal`)。

2. **EULA 自动同意**: 所有 Java 系核心(含 Nukkit)都检查 `eula.txt`。
   `ensureEulaAccepted()` 每次启动强制写 `eula=true`(覆盖已存在的 false)。
   否则 Paper 会在 `eula.txt` 提示后退出。

3. **Android 15 tagged pointers 崩溃**: 需在 manifest 加
   `android:allowNativeHeapPointerTagging="false"`。

4. **PowerShell 写文件会转码破坏中文**: `Set-Content`/`Out-File` 会把 UTF-8 中文写坏。
   → 一律用 write/Edit API 全量重写 Kotlin 文件。

5. **Os.read/write 签名是 FileDescriptor**: 传 fd 数字会类型不匹配,
   native 里自实现 `readFd/writeFd/closeFd`。

6. **run-as 下测 bionic java 的坑**: 需要 `LD_LIBRARY_PATH`,且 run-as 进程
   没有 tagged-pointer flag 会直接崩(用 app 内跑则无此问题)。

7. **osh/JNA 报 `libc.so.6 not found`**: 无害,只影响 SystemReport 硬件详情,
   会优雅降级继续启动。**不要被误导以为启动失败**。

8. **底部导航控制台空白 bug(已修)**: 底部导航曾直接 `navigate("console/{serverId}")`
   用模式路由,serverId 变字面量 `"{serverId}"` → 控制台永远 0 条日志。
   修复: `ServerRepository` 增加单例 `selectedServerId`,
   `MainNavHost` 底部导航对 console/players/worlds/settings 用
   `createRoute(selectedServerId)` 构造真实路由。

9. **libfontmanager.so 依赖 libawt.so**: 仅字体相关,服务器不需要,失败可忽略。

10. **APK 构建缓存污染**: 曾出现 195MB 的 APK(正常压缩 ~40MB),
   但 install 成功说明安装文件正常。

---

## 四、构建 / 部署 / 测试

### 构建
```powershell
Remove-Item Env:ANDROID_SDK_ROOT -ErrorAction SilentlyContinue
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
$env:ANDROID_HOME='I:\Android\Sdk'
$env:GRADLE_USER_HOME='I:\Android\gradle-cache'
& 'I:\my project\beamng-mobile\gradle_temp\gradle-8.11.1\bin\gradle.bat' assembleDebug --console=plain
```

### 部署
```powershell
$adb='I:\Android\Sdk\platform-tools\adb.exe'
& $adb -s 90d69b9 install -r 'I:\mckaifu\app\build\outputs\apk\debug\app-debug.apk'
```

### 真机验证流程(小米,landscape 2880x1800)
```powershell
# 启动 app
& $adb -s 90d69b9 shell am force-stop com.mckaifu.app
& $adb -s 90d69b9 shell am start -n com.mckaifu.app/.MainActivity
Start-Sleep 6
# 服务器列表"启动"按钮
& $adb -s 90d69b9 shell input tap 182 654
# 控制台 tab
& $adb -s 90d69b9 shell input tap 860 1700
# 看服务器输出
& $adb -s 90d69b9 shell "logcat -d -s mcserver:*"
# UI dump(注意: 中文需 GBK 解码, 或 --windows 抓下拉菜单)
& $adb -s 90d69b9 shell uiautomator dump /sdcard/ui.xml
```

### 关键坐标(landscape 2880x1800)
- 引导页"跳过": (133, 1660)
- 服务器列表"启动": (182, 654)  "停止": (360, 654)  "重启": (538, 654)
- 底部导航: 服务器(280,1700) 控制台(860,1700) 玩家(1440,1700) 世界(2020,1700) 设置(2600,1700)
- 服务器详情: 内网穿透 (1792,1038)

### 端口/握手验证
```powershell
(Test-NetConnection 192.168.1.18 -Port 25565).TcpTestSucceeded
# MC 状态握手 → 期望 {"version":{"name":"Paper 1.20.4","protocol":765},...}
```

### 拉取服务器真实 jar
```powershell
# 注意用 exec-out 走 cmd,PowerShell '>' 会损坏二进制
cmd /c "$adb -s 90d69b9 exec-out run-as com.mckaifu.app cat files/servers/<uuid>/versions/1.20.4/paper-1.20.4.jar > out.jar"
# 真实大小 22,831,573 字节
```

---

## 五、Paper 版本检查逻辑(反编译结论)

`org/bukkit/craftbukkit/Main.class`(从真实 jar 反汇编):
```
ignoreJavaVersion = Boolean.getBoolean("Paper.IgnoreJavaVersion")   # 属性名大写 P!
tooOld = Float.parseFloat(System.getProperty("java.class.version")) < 61.0
javaVersion = System.getProperty("java.version")                     # 编译期内建,不可覆盖
hasDash = javaVersion.contains("-")                                  # "17.0.10-internal" → true
if (!ignore && tooOld) { print "requires at least Java 17"; return }
if (!ignore && hasDash)  { print "unsupported non official"; return }
if (ignore && (tooOld || hasDash)) print warning(继续)
```

---

## 六、已完成功能(2026-07-31 会话)

1. **bionic JRE 全流程打通 + 真机验证 Paper 1.20.4 完整启动** (核心)
2. **`-DPaper.IgnoreJavaVersion=true` 大小写修复** → 跳过 Java 检查
3. **EULA 全核心自动同意**(eula.txt 恒为 true)
4. **引导页只显示一次**: AppPrefs 持久化 onboarding_done,
   MainNavHost 按标志选 startDestination
5. **内网穿透国内节点**: TunnelType 增加 NATAPP / 樱花frp;
   TunnelRegion 增加 中国华北/华东/华南/西部(下拉顶部高亮);
   TunnelScreen/TunnelService 同步更新
6. **客户端版本标明**: getClientVersionHint()/formatMcVersion(),
   修复 "MC 1.1.20.4" 显示 bug;创建页/下载卡片/详情页均显示可加入客户端版本
7. **底部导航控制台空白修复**(见坑 8)

---

## 六·二、全部功能真实化(2026-07-31 会话 2)

把此前所有假数据/死代码接成真实功能,真机全部验证通过:

| 功能 | 实现方式 |
|---|---|
| **玩家列表** | ServerEngine `trackPlayers()` 日志解析(join/left/lost/UUID/list 正则),`players` StateFlow 暴露;玩家管理/聊天页改 collect,不再有假 Steve |
| **插件管理** | 读真实 `plugins/` 目录;开关 = 重命名 `.jar` ↔ `.jar.disabled`;刷新按钮 |
| **封禁列表** | 解析 `banned-players.json` / `banned-ips.json`(JSONArray),解封后刷新 |
| **世界管理** | 按 server.properties 的 level-name 扫描 world/_nether/_the_end 真实目录+大小;立即备份(BackupManager)、备份列表、恢复 |
| **定时任务** | 持久化到 `scheduled_tasks.json`;MainViewModel.startScheduler() 每 30s 检查,执行重启/备份/命令/停止/启动;autoBackup 开关直接生效 |
| **配置编辑器** | 扫描根目录+plugins/*/ 真实可编辑文件;.properties 键值编辑,其他文本编辑,真实读写 |
| **内网穿透** | TunnelService 接真实进程(Playit/ngrok/natapp/frpc 二进制路径+token),日志解析公网地址,单例放 McKaiFuApp |
| **前台服务** | startServer 时启动 ServerService(通知"x/y 个服务器在线"),全停后停止 |
| **Geyser** | 创建/开关时真实下载 geyser-spigot jar + 写 config.yml |
| **性能优化** | PerformanceOptimizer 真实分析 JVM 参数,设置页一键应用 |
| **插件商店** | 去掉 25s 假超时;真实失败+重试按钮 |
| **社区分享** | 局域网地址(WifiManager)、剪贴板复制、系统分享;社区列表从 `community_servers.json`(仓库 raw)拉取 |

**修复的崩溃**: 控制台 LazyColumn key `timestamp_hashCode` 同毫秒重复 → `IllegalArgumentException: Key already used`。
改为 `timestamp_hashCode_index` 唯一 key(ConsoleScreen.kt:204)。

**验证结论**(真机): 服务器 Done 4.1s;控制台日志/命令/玩家追踪全通;前台服务 startForegroundCount=1;
世界/配置/封禁/插件页均显示真实数据。

**注意**: adb 无线调试会话会掉线(端口 5555 拒绝),重连方式:
`adb connect 192.168.1.18:5555` 失败时需手机上重新开启"无线调试";`adb devices` 也可能直接显示 `90d69b9 device`。

---

## 六·三、内网穿透配置文件导入(2026-07-31 会话 3)

**需求**: 穿透支持导入用户自己的配置文件(ngrok.yml / natapp.ini / frpc.toml / frpc.ini 等)

**已实现**:
1. `TunnelInfo.kt` 新增 `configPath: String = ""` 字段
2. `TunnelService.kt`:
   - 命令构建支持配置文件:NGROK `start --config <path>`、NATAPP `-config <path>`、SAKURA/CUSTOM `-c <path>`;无配置时走原有 authToken 逻辑
   - **修复**: CUSTOM 类型原来 `else -> return@launch` 直接不启动,现可启动
   - CUSTOM 类型日志解析公网地址(`[\\w.-]+:\\d+` 正则),且 CUSTOM 判断移到 sakura/frpc 判断之前(否则 frpc 输出的行会被 sakura 分支抢先匹配)
3. `TunnelScreen.kt` 新增"配置文件(可选)"卡片:
   - SAF(OpenDocument)选择文件 → 复制到 `files/tunnel_configs/<文件名>` → configPath 写入 TunnelInfo
   - 显示已导入文件名 + "移除"按钮

**踩坑(重要)**:
- MIUI SAF 文件选择器点击有默认关联程序的文件(xml 等)会直接打开预览而不返回 URI;`.txt`/`.toml` 无关联时会正常返回。测试用 `.ini.txt` 后缀最稳
- MIUI 的"最近文件"分类不索引新 push 的文件(MediaStore 延迟),需手动触发 `MEDIA_SCANNER_SCAN_FILE` 广播或等刷新
- **`CursorIndexOutOfBoundsException: Index -1 requested, with a size of 1`**: `query()` 返回的 cursor 必须先 `moveToFirst()` 再 `getString()`!之前只判断 `idx >= 0` 就 getString,position 仍在 -1 → 崩溃。已修复:`if (idx >= 0 && c.moveToFirst())`
- logcat 抓堆栈:`adb logcat -c` 清空后再操作,然后 `logcat -d -s mckaifu-tunnel:E` 看完整堆栈

**验证状态(已完成,2026-07-31 会话 4)**: 在 **MuMu 模拟器**(adb 序列号 `emulator-5558`,机型 V2166A,横屏 1600x900)上全流程验证通过:
1. install -r 安装最新 APK(含 moveToFirst 修复)
2. push 测试配置到 `/sdcard/Download/frpc.ini.txt`(175B,SAF "最近文件"未索引 → 侧栏进"下载"目录)
3. 穿透页 → "选择并导入配置文件" → 选中文件
4. 结果: 回到穿透页 UI 显示文件名 `frpc.ini.txt` + "移除"按钮;`files/tunnel_configs/frpc.ini.txt` 存在(175B);logcat `mckaifu-tunnel:E` 无任何异常 → **CursorIndexOutOfBoundsException 已修复,导入流程完全正常**。

**模拟器测试坐标(横屏 1600x900)**:
- 穿透页"选择并导入配置文件"按钮: (800, 615)
- SAF 选择器侧栏"下载"目录: (210, 330);文件卡片 frpc.ini.txt: (158, 509)
- 其他参考坐标见会话 4 记录

**MuMu 模拟器测试要点**:
- adb 连接: `adb devices` 显示 `emulator-5558`(MuMu 自动注册,无需手动 connect)
- 分辨率 900x1600 竖屏 / 1600x900 横屏;坐标不可复用真机 2880x1800 的比例,一律用 uiautomator dump 现抓
- 模拟器上无 bionic JRE/JRE assets 问题(未测启动服务器,仅验证 UI 功能)

---

## 六·四、内置 frpc 开箱即用(2026-07-31 会话 5)

**需求**: 把 frpc 二进制打包进 APK,用户只要导入合格的配置文件就能穿透,不再需要自备二进制。

**已实现**:
1. **交叉编译 frpc v0.51.3**(选 v0.51.3 是因为它是最后一个支持 `.ini` 格式的版本,兼容 ChmlFrp/樱花等国内服务的旧 ini 配置):
   - arm64-v8a:`GOOS=android GOARCH=arm64 CGO_ENABLED=0 go build -ldflags '-s -w'`(16MB)
   - x86_64(给模拟器):`GOOS=android GOARCH=amd64 CGO_ENABLED=1` + NDK `x86_64-linux-android21-clang.cmd`(17MB,amd64 必须 cgo)
   - 源码 clone:`gitee.com/mirrors/frp` tag `v0.51.3`;GOPROXY=`https://goproxy.cn,direct`
   - 产物放 `app/src/main/assets/frpc/<abi>/frpc`
2. **FrpManager.kt**(service 包): `pickAbi()` 优先 x86_64(模拟器)否则 arm64-v8a;`ensureFrpc()` 首次从 assets 解压到 `files/bin/frpc` 并 `chmod 755`;`isBundled()/isExtracted()`
3. **TunnelScreen.kt**:
   - `CUSTOM`/`SAKURA` 类型时,`客户端程序路径` 留空 → 启动按钮自动 `FrpManager.ensureFrpc(context)` 兜底
   - 占位符改为"留空使用内置 frpc",提示"已内置 frpc v0.51.3,留空将自动使用(需导入配置文件);也可填写自定义路径"
4. **配置文件持久化**(此前 TunnelInfo 纯内存,重启即丢): `AppPrefs.saveTunnelInfo/loadTunnelInfo` 按 `tunnel_info_<serverId>` 存 TunnelInfo JSON(kotlinx-serialization);TunnelScreen 打开时加载、`LaunchedEffect(tunnelInfo)` 保存
5. **TunnelService**: CUSTOM/SAKURA 且带配置文件时,启动后从配置文件解析公网地址(`server_addr:remote_port`,支持 ini 与 toml)直接显示,因为 frpc 日志不打印地址

**验证结论**(MuMu 模拟器,全部通过):
- UI 显示"留空使用内置 frpc" + "已内置 frpc v0.51.3"
- 导入 frpc.ini.txt → 重启 app → 类型(自定义隧道)+ 配置文件名都恢复(持久化生效)
- 启动穿透 → `files/bin/frpc` 解压成功(17MB, x86_64,可执行)→ logcat:
  `start frpc service for config file [..frpc.ini.txt]` → 正确解析配置并尝试连接
  (测试配置用假域名 `cn-bj.nodechmlfrp.cn`,`no such host` 退出——配置合格时即正常连上)
- UI 无崩溃,穿透断开后状态复位

**注意**:
- APK 从 ~40MB 增至 54MB(内置两个 ABI frpc 压缩后 ~14MB)
- `androidResources { noCompress.clear() }` 已存在,assets 会压缩打包、运行时解压
- 真机(arm64)会取 arm64-v8a 的 frpc;x86 32 位设备无内置(极少见,忽略)
- ChmlFrp/樱花等国内 frp 服务通常给的是 ini 格式,与本内置 v0.51.3 兼容;若用户配的是新版 toml 也支持

---

## 六·五、会话 6: 底栏导航修复 + RCON 玩家数据 + 停止/重启崩溃修复(2026-07-31)

### 1. 控制台页底部导航栏失效(bug 修复,真机验证通过)

**症状**: 服务器详情 → "控制台"快捷操作 → 控制台页底部导航栏点击无效/跳错页(如点"服务器"tab 却停在控制台,或跳到字面量路由)。

**根因**: `MainNavHost.kt` 底栏点击用 `popUpTo(start){saveState}+restoreState` 多返回栈机制,从详情页 push 进入的页面与 tab 栈混乱;且 `selectedServerId` 为空时会导航到字面量路由 `console/{serverId}`。

**修复**: 
- serverId 改为从当前路由参数派生(`navBackStackEntry.arguments.getString("serverId")`),不再依赖可能过期的 `selectedServerId`
- 点击已选中的 tab 直接 no-op(避免 popUpTo 清状态)
- 移除 `restoreState = true`(多返回栈混乱元凶)
- `LaunchedEffect(currentDestination)` 同步 `vm.selectServer`

### 2. 玩家数据接 RCON(health/food/level/坐标,真机验证链路)

**新增 `RconPlayerProvider.kt`**(service 包):
- `ensureRcon(serverDir)`: 若 server.properties 已开 rcon 则用用户配置;否则注入 `enable-rcon=true` + `rcon.port=25575` + 16 位随机密码(**开箱即用,每次启动服务器自动注入**)
- `fetchPlayers(serverId, config, onUpdate)`: RCON 连 `127.0.0.1` 执行 `list` 拿在线名单,再对每人 `data get entity <name>` 解析 NBT:
  - `Health: 20.0f`、`foodLevel`、`foodSaturationLevel`、`XpLevel`、`XpP`、`Pos: [x,y,z]`、`Dimension`(-1 地狱/1 末地/0 主世界)
  - maxHealth 从 `Attributes: [{Name: "minecraft:generic.max_health", Base: 20.0d}]` 解析
- **注意**: ping 无 vanilla 数据源,保持 0,UI 改为显示世界+坐标

**接入**: ServerEngine.startServer 时 `rconConfigs[id]=ensureRcon(dir)`;新增 `updatePlayer` 合并;MainViewModel 加 `refreshPlayersViaRcon`;PlayerManagementScreen 每 4 秒轮询 + 顶部刷新按钮;XP 条改为 `(xp%100)/100f`、显示 `Lv ${level}`。

**验证**(真机): `ss -tlnp` 显示 `*:25575 LISTEN`;PS 脚本走 RCON 协议(auth→list→summon→data get entity)全部通过,`data get entity` 输出标准 NBT JSON。

### 3. 停止/重启服务器导致 App 崩溃(SIGABRT)——**fork 子进程方案**(重要!)

**症状**: 点"停止"或"重启"→ 服务器正常保存退出(Closing Server)→ 0.3 秒后整个 App 崩溃:
```
FORTIFY: pthread_mutex_lock called on a destroyed mutex (0x7d8180e408)
Fatal signal 6 (SIGABRT) in tid 31407 (hwuiTask0), pid com.mckaifu.app
```

**根因**: Pojav 风格 **in-process JVM**(JLI_Launch 直接在当前进程跑)的已知坑: JVM 退出路径与 Android 宿主线程(RenderThread/hwuiTask0)的 linker/全局 mutex 冲突 → SIGABRT。重启流程旧实现: stop 发送后立即 return(bionic 分支),watchdog 只 join 不置 OFFLINE(状态永远卡"停止中");restart 只等 3 秒就 startServer,旧 JVM 未退出 → `isRunning` 为 true → **重启静默失败**。

**修复**(native + Kotlin):
- `vmlauncher.c` 新增:
  - `launchJvmChild(args, inFd, outFd)`: **fork 子进程** → 子进程 dup2 管道 stdio → `JLI_Launch` → `_exit(res)`(绕过 atexit/全局析构);父进程返回 pid。argv 构造在 fork 前完成(JNI 调用不能跨 fork)
  - `isProcessAlive(pid)`(kill 0 / ESRCH)、`killProcess(pid, sig)`
- `ServerEngine`:
  - `startServerBionic` 改用 `launchJvmChild`,存 `childPids[id]`;宿主不再 setStdio/restoreStdio
  - `stopServer`: 发送 stop 后**轮询等待真实退出**(15s 超时 SIGKILL),再 cleanup + 置 OFFLINE
  - `restartServer`: 等 `isRunning` false(30s 上限)再 startServer,不再固定 3 秒
  - watchdog 改为轮询子进程存活(1s 间隔),死亡后清理
  - `isRunning` = `childPids 存活 || processes 存活`
- **验证**(真机全通过): 启动 Done→停止(子进程 32674 SIGABRT,宿主 29653 存活,状态"离线")→重启(自动拉起新子进程 pid 1002,Done 4.9s,"在线")。崩溃被完全隔离在子进程,宿主永不崩。

**native 编译命令**(NDK 27.1.12297006):
```
aarch64-linux-android21-clang.cmd -shared -fPIC -O2 -o libmckaifu_vm.so vmlauncher.c -llog
```
- **源码已备份到 `app/src/main/jniLibs/vmlauncher.c`**(解决"仓库无 native 源码"问题)
- 仅 arm64-v8a;模拟器 x86_64 无此 .so(模拟器不测启动服务器)

### 4. playit/ngrok 内置(未完成,会话 6 进行中)

- **playit**: 官方下载页 URL 已失效(返回 HTML),改用 **GitHub release**: `github.com/playit-cloud/playit-agent/releases/download/v1.0.10/playit-cli-linux-aarch64`(5.6MB,已下载 → `assets/tunnel/playit/arm64-v8a/playit`,ELF 魔数已验证)
- **ngrok**: `bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-arm64.tgz`(~11MB)下载超时(CDN 极慢,PowerShell 与 curl 均失败),只下到 2.2MB 部分文件 → 未完成
- **natapp**: 下载页需登录+购买隧道才能下载(`download.natapp.cn/...` 403)→ **无法内置**,UI 保持"自备二进制"
- 下一步: 完成 ngrok 下载(换镜像/挂代理)→ 泛化 FrpManager(按类型解压 assets/tunnel/<type>/)→ TunnelScreen 对 PLAYIT/NGROK 留空路径兜底

---

## 六·六、会话 7: ngrok 内置完成 + TunnelBinaryManager 泛化(2026-08-01)

**目标**: 完成会话 6 未完成的 playit/ngrok 内置,泛化单类型 frpc 管理器。

**已实现**:
1. **ngrok 下载成功**: 重试 `bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-arm64.tgz`(10.5MB,之前超时)下载成功;解压 30.9MB ELF(e_machine=183=EM_AARCH64,已验证)→ `assets/tunnel/ngrok/arm64-v8a/ngrok`
2. **统一资产结构**: `assets/tunnel/<type>/<abi>/<name>`,frpc 从 `assets/frpc/` 迁入 `assets/tunnel/frpc/`(arm64-v8a + x86_64)
   - playit → `tunnel/playit/arm64-v8a/playit`(5.6MB,会话 6 已下载)
   - ngrok → `tunnel/ngrok/arm64-v8a/ngrok`(30.9MB)
   - frpc → `tunnel/frpc/<abi>/frpc`(16/17MB)
   - natapp 仍无法内置(官方需登录)
3. **FrpManager → TunnelBinaryManager**(service 包):
   - `ensureBinary(context, type)` 按 TunnelType 映射 assets 路径(`TYPE_DIR`/`BINARY_NAME` map),解压到 `files/bin/<name>` + chmod 755
   - `isBundled(context, type)`/`isExtracted`/`deleteBinary` 全部按类型泛化;`FRPC_VERSION = "v0.51.3"`
4. **TunnelScreen.kt**:
   - `supportsBundled = type != NATAPP`;`bundledReady = isBundled(context, type)`
   - 帮助文字按类型: PLAYIT "已内置 playit 客户端" / NGROK "已内置 ngrok 客户端" / frpc 系 "已内置 frpc v0.51.3,留空将自动使用(需导入配置文件)"
   - 启动按钮: 留空路径时 `TunnelBinaryManager.ensureBinary(context, type)` 兜底
   - 错误提示细化: frpc 系无配置报"需要先导入配置文件",其余报"内置二进制解压失败"
5. **模拟器验证**(MuMu x86_64,全部通过):
   - NGROK 类型: x86_64 无内置 → 正确显示"需自行下载对应平台的客户端可执行文件"
   - CUSTOM 类型: 显示"已内置 frpc v0.51.3"→ 启动穿透 → logcat `start frpc service for config file [frpc.ini.txt]`(新资产路径解压成功),`files/bin/frpc` 17MB x86_64 权限 rwxr-xr-x ✓;连接假域名失败退出(预期),状态复位
   - 配置文件持久化正常(frpc.ini.txt 恢复)

**注意**:
- APK 67.8MB(ngrok arm64 压缩后 +~14MB)
- playit/ngrok 仅内置 arm64-v8a(真机);x86_64 模拟器上显示"需自行下载"
- 真机(arm64)上 playit/ngrok 的内置提示 + 实际启动验证待真机上线后补(命令已就绪: playit `--secret <token> --port <port>`;ngrok `tcp <port> --authtoken <token>`)
- 未尝试下载 x86_64 版 playit/ngrok(用户要求不再下载,APK 体积也已偏大)

---

## 六·七、会话 8: 核心版本自动爬取修复 + 服务器删除(2026-08-01)

**需求**: 创建服务器要能选版本(如 Paper 1.20.4)并下载;版本要自动爬取(不硬编码,不怕新版本);用镜像站(国内);服务器要能删除。

### 1. 关键 bug: Paper 版本列表为空(用户"不能选版本"的根因)

**现状**: `fetchPaperVersions()` 用 `https://fill.papermc.io/v3/projects/paper/versions/{ver}/builds`,
该镜像**返回 JSON 数组**(`[{...}]`),而代码按对象解析 `JSONObject(buildsJson).getJSONArray("builds")` → 抛异常 → 被 catch 吞掉 → **Paper 版本列表永远为空**!

**修复**(DownloadManager.kt):
- 用 `/versions` 端点拿版本列表(标准 `{"versions":[{"version":{"id":"26.2"}}]}`),跳过 RETIRED/LEGACY
- **只对最近 25 个版本并行**拉 builds(`coroutineScope { async(Dispatchers.IO) }`),避免数百次串行请求(之前会等 >90 秒)
- builds 按 **JSONArray** 解析,取第一个 STABLE(数组最新在前)
- 下载 URL 是绝对路径 `https://fill-data.papermc.io/v1/objects/<sha>/paper-<ver>-<build>.jar`(CDN,国内可用,已验证 42MB 200 OK)

**验证**(MuMu 模拟器): 创建向导 → 下载核心对话框 → Paper 显示 **26.2(推荐)、26.1.2、26.1.1、26.2-rc-2、1.21.11** ✓ 自动爬取最新版成功

**注意**: fill 镜像可用性(2026-08 实测):
- `fill.papermc.io` OK(镜像,Paper)
- `api.papermc.io` **403 被拒**(官方,国内不可用)
- `api.fastmirror.net` DNS 解析失败
- `bmclapi2.bangbang93.com` 无 spigot/paper 端点
- `api.purpurmc.org` OK、`launchermeta.mojang.com` OK、`api.github.com` OK
- `api.getbukkit.org` / `download.getbukkit.org` **DNS 不通**(Spigot 下载源失效)

### 2. 各核心版本来源改造

| 核心 | 改造前 | 改造后 |
|---|---|---|
| Paper | fill 镜像(但 builds 解析 bug → 空) | **修复 + 并行 + 限 25 个最近版本**(fill 镜像) |
| Purpur | 官方 API ✓ | 保留(国内可用) |
| Vanilla | launchermeta ✓ | 保留 |
| Pufferfish | 硬编码 5 个(≤1.20.4) | 硬编码更新到 1.21.1(无公开 API) |
| Spigot | 硬编码 4 个 | 硬编码更新到 1.21.1(下载源 getbukkit 国内失效,建议用 Paper) |
| Nukkit | 硬编码 1 个 | **Jenkins CI API 自动拉最新构建**(ci.opencollab.dev) |
| PocketMine | 硬编码 1 个 | **GitHub releases API 自动爬取 20 个**(api.github.com/pmmp) |

### 3. 服务器删除(新增 UI + 连文件删)

- `ServerRepository.removeServer`: 递归删除 `files/servers/{id}/`(jar/世界/plugins/backups)+ 清理定时任务/控制台消息/tunnel 配置
- `MainViewModel.deleteServer`: 停隧道 + `engine.cleanup` 杀进程 + remove + 更新前台服务
- `ServerListScreen`: 卡片右上角加**删除图标**(状态标签右侧)→ `ConfirmDeleteDialog` 确认("将同时删除其世界、插件、备份等所有文件,且不可恢复")→ 删除
- **验证**(MuMu 模拟器): 点删除图标 → 确认框弹出(标题"删除服务器"+ 删除/取消)→ 确认后列表 3→2,`files/servers/` 目录清空 ✓

### 4. 待注意
- 创建向导里版本 ExposedDropdownMenu **在 MuMu 模拟器上点击不展开** → 会话 10 已定位: 是 `adb input tap` 对 Compose 点击组件时序 bug(非模拟器渲染),且版本选择已改为对话框交互,此问题已消除

---

## 六·八、会话 9: 全核心版本自动爬取(Spigot/Pufferfish 去硬编码)(2026-08-01)

**需求**: 所有服务器核心都要自动爬取版本(不硬编码、不怕新版本)。会话 8 遗留 Spigot/Pufferfish 硬编码,本次补齐。

### 1. Pufferfish: Jenkins CI 自动爬取(官方,无版控 API)

- 官方 Jenkins `https://ci.pufferfish.host/api/json?tree=jobs[name,url]` → job 名 `Pufferfish-1.17`~`Pufferfish-1.21`(排除 `Pufferfish-Purpur` 前缀)
- 各 job 并行 `lastSuccessfulBuild/api/json?tree=number,url,artifacts[fileName,relativePath]`
- mcVersion 用正则 `paperclip-(\d+\.\d+(?:\.\d+)?)` 从 artifact 文件名提取;下载 URL = `{buildUrl}artifact/{relativePath}`
- **验证**(MuMu): 显示 **5 个版本,最新 1.21.10(Build #39,推荐)**,jar 57.8MB 200 OK ✓

### 2. Spigot: getbukkit.org 页面 HTML 解析(官方站)

- 官方站 `https://getbukkit.org/download/spigot`,解析 51 个 `class="download-pane"` 块:
  正则 `class="download-pane".*?<h2>{ver}</h2>.*?<h3>{sizeMB}</h3>.*?href="https://getbukkit\.org/get/{token}"`
  → `fileSize = sizeMB*1024*1024`;下载 URL = `getbukkit.org/get/{token}`(**302 跳转**到真实 jar,不能用 download.getbukkit.org 直接下载——DNS 不通)
- **验证**(MuMu): 显示 **51 个版本,最新 26.2(推荐)**,下载 302→jar 30.2MB 200 OK ✓

### 3. 崩溃修复: LazyColumn key 重复(重要!)

**症状**: 打开 PocketMine 页 app 崩溃("屡次停止运行")。
**根因**: `CoreDownloadScreen.kt` LazyColumn key = `"${it.mcVersion}_${it.buildNumber}"`,
PocketMine 20 个版本 mcVersion 全是"基岩版"、buildNumber 全 0 → **key 全部 `基岩版_0` 重复** →
`IllegalArgumentException: Key "基岩版_0" was already used`。
**修复**: key 改为 `"${it.coreType}_${it.version}_${it.buildNumber}"`(`version` 字段对 PocketMine 是 tag、Spigot 是 mcVer、Paper 是 `26.2-87`,各核心均唯一)。
**验证**: 修复后 PocketMine 显示 **20 个版本**不再崩溃 ✓

### 4. 全核心自动爬取验证汇总(MuMu 模拟器,全部通过)

| 核心 | 数据源 | 验证结果 |
|---|---|---|
| Paper | fill.papermc.io 镜像 | 26.2(推荐)+ 历史版 |
| Purpur | api.purpurmc.org | 可用 |
| Pufferfish | ci.pufferfish.host Jenkins | **5 个版本,1.21.10** |
| Spigot | getbukkit.org HTML | **51 个版本,26.2** |
| 原版/Vanilla | launchermeta | 可用 |
| Nukkit | ci.opencollab.dev Jenkins | **1 个版本(最新构建)** |
| PocketMine | api.github.com releases | **20 个版本** |

调试日志 tag: `mckaifu-core`(`$coreType -> ${versions.size} versions, first=...`)

---

## 六·九、会话 10: 创建向导版本选择改对话框 + 剩余待办收尾(2026-08-01)

### 1. 创建向导版本下拉改为"版本选择对话框"(原 MuMu 点击不展开的真相)

**发现根因**: 版本字段原来是 `ExposedDropdownMenuBox`,在 MuMu 模拟器上点击不展开,
**真正原因不是模拟器渲染问题,而是 `adb shell input tap` 对 Compose 可点击组件的时序 bug**——
`input tap` 会触发 TextField 焦点但不触发 click;改用 `input swipe x y x y 100`(短距滑动)
则正确触发 click。会话 8 记录"疑似模拟器问题"被证实。

**改造**(CreateServerScreen.kt):
- 版本字段: `ExposedDropdownMenuBox` + `DropdownMenuItem` 列表 → `OutlinedTextField(enabled=false)` + 外层 `Box(clickable)`,
  点击弹出 `VersionSelectDialog`(AlertDialog + LazyColumn,key = `coreType_version_buildNumber`,推荐标签,高度 360dp 可滚动)
- **验证**(MuMu): 点版本字段 → 对话框弹出显示 **PaperMC 25 个版本**(26.2 带推荐)→ 选中 1.21.10 → 字段更新为 "Minecraft 1.21.10" ✓
- 核心类型下拉(7 项,短)保留 ExposedDropdownMenuBox(真机正常)

### 2. 剩余待办处理结论

- **CommunityScreen 离线重试**: 已实现(`loadFailed` 时显示"社区列表加载失败(需联网),点击重试")→ 完成
- **oshi/JNA glibc 警告**: 代码库中无 oshi/JNA 引用,仅 JRE 启动时打印的无害警告,不影响启动 → 无需处理
- **APK 构建缓存污染**: 本次构建 APK 65.2MB(ngrok 内置后正常体积),195MB 问题未复现 → 已随构建环境稳定消失
- **ensureRuntime release 补丁**: 已由 `-DPaper.IgnoreJavaVersion` 绕过,无需验证
- **截图/uiautomator 乱码**: 已有绕开方法(ASCII 脚本提取 + cmd 重定向)
- **视频宣传**: 用户需求,待规划(非代码)

---



---

## 六·十一、会话 11: JRE 21/25 bionic 缺失修复 + 真机 1.21.11 验证通过 (2026-08-01)

### 1. 问题根因
- 模拟器上 Paper 1.21.11 服务器报错"加载内置Java运行时失败(JRE库缺失或损坏)"。
- **根因**: 服务器 `javaVersion: 21` 需要 JRE 21,但 assets 里只有 `jdk/17.tar.xz`(bionic)。
  模拟器上残留的 `files/jdk/21` 是旧 APK 遗留的 **glibc 版**(依赖 `libdl.so.2`/`libc.so.6`/`libpthread.so.0`),
  在 Android bionic 上 dlopen 全失败 → `loadJreLibraries()` 返回 false → 报错。
- 判定方法: 读取 `lib/libjli.so` 的依赖字符串,含 `libc.so.6`/`libdl.so.2` = glibc(无效);
  含 `libc.so`/`libdl.so` 无版本号后缀 = bionic(有效)。

### 2. 解决
- **来源**: Amethyst 1.1.7 APK 只内置 jre8,但它的 OpenJDK 构建仓库 `AngelAuraMC/angelauramc-openjdk-build`
  有 release tag `download_jre21` / `download_jre25`,提供 **bionic 版** JRE tar.xz:
  `https://github.com/AngelAuraMC/angelauramc-openjdk-build/releases/download/download_jre21/jre21-android-arm64.tar.xz` (27.3MB)
  `https://github.com/AngelAuraMC/angelauramc-openjdk-build/releases/download/download_jre25/jre25-android-arm64.tar.xz` (36.3MB)
- 下载(用户迅雷,`D:\迅雷\`)后复制为 `app/src/main/assets/jdk/21.tar.xz` 和 `25.tar.xz`。
- **关键兼容点**: 现有 17.tar.xz 顶层是 `jre17/`,而 Amethyst 的 tar 顶层是 `./`;
  `JavaRuntimeManager.extractTarXz` 用 `name.substringAfter('/')` 剥离第一层,
  两者提取后都落在 `files/jdk/<ver>/bin`、`lib` 下,结构一致,无需重打包。
- **代码修复** (JavaRuntimeManager.kt): `ensureRuntime` 增加 `isRuntimeValid` 校验,
  已存在的 runtime 若检测到 glibc 特征则 `deleteRuntime` 后重新解压 assets 内的 bionic 版。

### 3. 真机验证 (小米 90d69b9, arm64-v8a, Android 15)
- 安装新 debug APK (127.8MB, 含 21/25.tar.xz)。
- 手动创建 1.21.11 服务器: push `paper-1.21.11-132.jar` → `files/servers/c9e308f0-4ac2-4b3b-b157-8f5f8e0aed9d/paper-1.21.11.jar`,
  在 servers.json 追加 `javaVersion:21` 的配置 (port 25566)。
- 启动后日志显示:
  - `[03:56:47 INFO]: Done (26.910s)! For help, type "help"` → **Paper 1.21.11 完整启动成功**。
  - bionic JRE 21 解压、dlopen 全部成功(无 glibc 依赖错误)。
- 次要警告: spark 插件 async-profiler 原生库需 `libdl.so.2`(glibc)加载失败,仅影响 profiler,不影响服务器运行。

### 4. 环境备注
- MuMu 模拟器 adb 端口非 7555:`adb_debug.mode=0`(adb 调试关闭),MuMu 主进程监听 20496 但连接 offline。
  模拟器验证需先开启 MuMu 的 adb 调试(设置→adb调试)或用真机。真机为 arm64,比模拟器(ARM 翻译层)更可靠。
- D 盘 `OpenJDK*U-jre_aarch64_linux_hotspot_*.tar.gz` 均为 **glibc 版,不可用**。
- Amethyst APK 运行时 JRE 下载源逻辑在 `NewJREUtil.java` 的 `getJreSource()`。




---

## 六·十二、视频宣传制作 (2026-08-01, 进行中)

### 1. 用户需求
- 目标平台 B站,横屏 16:9,2-3 分钟,**纯 BGM + 全屏大字幕,无口播/无配音**
- 风格: 前半段"装X/对比"抓人,中段"功能快剪"节奏拉满,结尾落地 GitHub 链接
- 用户选择: **对比装X向 + 功能介绍快剪**

### 2. 已完成
- **脚本**: `docs/video_script.md`(4 幕时间轴分镜 + 14 个录屏镜头 A-N + 剪辑/BGM/发布文案建议)
- **工具**: `D:\mckaifu_video\rec.py`(adb 录屏驱动工具: tap/swipe/text/screenshot/record)
- **环境**: 真机 90d69b9(小米,arm64)横屏已锁定(2880x1800);uiautomator dump 可用;
  首页 UI 坐标已摸清(创建服务器按钮~2810,200;1.21.11 卡启动~160,966;底部导航: 服务器280/控制台860/玩家1413/世界1993/设置2573, y=1660)
- **App 状态**: 已装 1.20.4(我的服务器) + 1.21.11(Test 1.21.11)两个服务器,均已配好可启动

### 3. 遇到的问题 (录屏)
- `adb shell screenrecord` 直接跑 OK;rec.py 的 Popen 方式跑出的文件 0 字节显示(实际 27-30KB 有内容)。
- 现象: rec.record 里 `rec` 局部变量会遮蔽模块名 → actions() 里 `rec.swipe` 可能引用错对象。
  需改用 `as recmod` 或全局引用。**注意**: a_home.mp4 / a2.mp4 实际已生成(见 clips 目录)。
- screenrecord 用 `--time-limit N` 自动结束,wait(timeout) 后 pull。

### 4. 待办 (下次继续)
- [ ] 修复 rec.py 的 rec 遮蔽 bug,重新录制全部镜头 A-N(参考 docs/video_script.md 的"素材拍摄清单")
- [ ] 下载 ffmpeg 到 D:\mckaifu_video(不放 C 盘)。线索: gyan.dev 慢;华为云镜像 ffmpeg 目录重定向;
      BtbN GitHub release 可达 (ffmpeg-master-latest-win64-gpl.zip 169MB, HEAD 200 1.1s);
      winget 有 Gyan.FFmpeg 8.1.2 / BtbN 系列(但默认装 C 盘,需指定 D 盘或用 zip 解压)
- [ ] 剪辑: ffmpeg 拼接分镜 + 加全屏字幕 + BGM(需无版权 BGM 素材)
- [ ] 导出成片交付

### 5. 关键坐标备忘 (2880x1800 横屏)
- 首页顶栏: "创建服务器" 按钮 center ~ (2810,200)
- 服务器卡片 1.21.11 (Test 1.21.11): 卡片区 [40,764][2840,1046], 启动按钮 center ~ (160,966)
- 底部导航 (y≈1660): 服务器280 / 控制台860 / 玩家1413 / 世界1993 / 设置2573
- UI dump 法: `adb shell uiautomator dump /sdcard/ui.xml` + `cat`(Compuose 可点节点 content-desc 有中文标签)
- 录屏: 横屏锁 `settings put system accelerometer_rotation 0; settings put system user_rotation 1`


## 七、待办 / 已知问题

- [x] 内网穿透配置文件导入: 真机验证 `moveToFirst` 修复后的导入流程(**会话 4 在 MuMu 模拟器验证通过**)
- [x] 内置 frpc(会话 5): CUSTOM/樱花frp 导入配置即可用,不再需要自备二进制
- [x] 底栏导航失效(会话 6): 已修,真机验证
- [x] 停止/重启崩溃(会话 6): fork 子进程方案已修,真机验证(启动/停止/重启全流程)
- [x] 玩家 health/hunger/xp/坐标接 RCON(会话 6): 已实现,真机验证链路
- [x] playit/ngrok 内置(会话 7): ngrok arm64 下载成功;TunnelBinaryManager 泛化;模拟器验证内置 frpc 回归;playit/ngrok 真机内置验证待真机上线
- [x] Paper 版本列表为空 bug(会话 8): fill 镜像 builds 返回 JSONArray 而代码按对象解析 → 版本永远为空;已修复(JSONArray + 并行 + 限最近 25 版),模拟器验证显示 26.2/1.21.11 等
- [x] 版本自动爬取(会话 8): Nukkit(Jenkins CI)/PocketMine(GitHub releases)自动爬取;Paper/Purpur/Vanilla 已网络爬取;Spigot/Pufferfish 无公开 API 保留硬编码(更新到 1.21.x)
- [x] 服务器删除(会话 8): 列表卡片删除图标 + 确认框 + 连文件递归删除,模拟器验证通过
- [x] Spigot/Pufferfish 自动爬取(会话 9): Pufferfish 用官方 Jenkins CI、Spigot 用 getbukkit 页面 HTML 解析,均已在模拟器验证(Pufferfish 5 版含 1.21.10 / Spigot 51 版含 26.2)
- [x] PocketMine 自动爬取验证 + LazyColumn key 重复崩溃修复(会话 9): key 改为 `coreType_version_buildNumber`,模拟器显示 20 个版本不再崩溃
- [x] 创建向导版本下拉点击不展开(会话 10): 根因是 `adb input tap` 对 Compose 点击组件时序 bug(非模拟器渲染);改为 VersionSelectDialog 对话框,模拟器验证弹出版本列表+选中生效
- [x] CommunityScreen 离线重试: 已实现(loadFailed 显示"点击重试")
- [x] oshi/JNA glibc 警告: 代码库无引用,JRE 启动无害警告,无需处理
- [x] APK 构建缓存污染: 65.2MB 正常,195MB 未复现
- [ ] 真机验证: 创建向导/版本选择对话框、核心下拉、版本自动爬取在真机小米上的表现
- [ ] 视频宣传(用户需求): B 站宣传视频,热门配音(不用 AI),GitHub 链接,实拍演示,流畅剪辑——待规划

---

## 八、环境

- 真机: 小米(Xiaomi),adb 序列号 `90d69b9`,WiFi IP `192.168.1.18`,系统 Android 15
- 屏幕: 2880x1800 landscape(测试时横屏);真机也曾出现 1800x2880 竖屏
- JRE assets: Zalith/Pojav 的 bionic JRE 17(bionic libc,非 glibc)
- 手机数据目录: `files/servers/<uuid>/` (server.jar, eula.txt, server.properties, tmp/, versions/)
- 服务器目录: `files/servers/289292cf-0096-4728-b6ea-fd2602f71515/`
- 模拟器: MuMu(`emulator-5558`,曾断开后真机 90d69b9 上线;重连 MuMu 用 `adb connect 127.0.0.1:7555`)
- RCON: 注入后端口 25575,密码存 server.properties(每次启动注入,用户已开启则尊重原配置)

### 注意: 原生库是预编译的
`app/src/main/jniLibs/arm64-v8a/libmckaifu_vm.so` 是 NDK 编译产物,**源码已备份在 `app/src/main/jniLibs/vmlauncher.c`**(会话 6 拷回)。
编译命令见上文六·五第 3 节;仅 arm64-v8a 已编译(模拟器 x86_64 未编译,模拟器上不测启动服务器)。
