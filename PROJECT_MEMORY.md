# McKaiFu 项目记忆 (Project Memory)

> 本文档记录项目的目标、架构、关键进展、踩坑经验、验证方法和当前状态，
> 供后续会话快速恢复上下文。最后更新: 2026-07-31

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
| `app/src/main/java/com/mckaifu/app/service/TunnelService.kt` | 内网穿透(Playit/Ngrok/NATAPP/樱花frp) |
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

## 七、待办 / 已知问题

- [ ] APK 构建缓存污染问题(195MB vs 40MB)未根治
- [ ] oshi/JNA glibc 警告未消除(无害,可加白名单或忽略)
- [ ] 内网穿透目前是 UI 模拟 + 调用户自备二进制,未真正打包 natapp/frpc
- [ ] `ensureRuntime` 的 release 文件补丁未验证(已由 IgnoreJavaVersion 绕过,可留)
- [ ] 截图验证需用 cmd 重定向(`screencap -p`),PS 会坏文件
- [ ] uiautomator dump 中文乱码: 读文件用 UTF8,或 GBK 转码技巧

---

## 八、环境

- 真机: 小米(Xiaomi),adb 序列号 `90d69b9`,WiFi IP `192.168.1.18`,系统 Android 15
- 屏幕: 2880x1800 landscape(测试时横屏)
- JRE assets: Zalith/Pojav 的 bionic JRE 17(bionic libc,非 glibc)
- 手机数据目录: `files/servers/<uuid>/` (server.jar, eula.txt, server.properties, tmp/, versions/)
- 服务器目录: `files/servers/289292cf-0096-4728-b6ea-fd2602f71515/`

### 注意: 原生库是预编译的
`app/src/main/jniLibs/arm64-v8a/libmckaifu_vm.so` 是**预编译产物**(NDK 编译 vmlauncher.c 得到),
仓库里暂无 vmlauncher.c 源码(上次会话在临时目录编译后未拷回项目)。
如需改 native 代码,需重建 NDK 工程并替换该 .so;仅 arm64-v8a 已编译。
