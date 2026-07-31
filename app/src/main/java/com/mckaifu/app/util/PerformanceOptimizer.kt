package com.mckaifu.app.util

import com.mckaifu.app.data.model.PerformanceConfig
import com.mckaifu.app.data.model.ServerInstance

class PerformanceOptimizer {

    data class OptimizationSuggestion(
        val category: String,
        val title: String,
        val description: String,
        val impact: ImpactLevel,
        val configKey: String = "",
        val configValue: String = "",
        val isJavaArg: Boolean = false
    )

    enum class ImpactLevel { HIGH, MEDIUM, LOW }

    fun analyzeJavaArgs(server: ServerInstance): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()
        val args = server.javaArgs.lowercase()

        if (!args.contains("g1gc")) {
            suggestions.add(OptimizationSuggestion(
                "JVM参数", "启用G1GC垃圾回收器",
                "G1GC能显著减少服务端卡顿，推荐所有Minecraft服务端使用",
                ImpactLevel.HIGH, isJavaArg = true
            ))
        }

        if (!args.contains("parallelrefproc")) {
            suggestions.add(OptimizationSuggestion(
                "JVM参数", "启用ParallelRefProcEnabled",
                "并行处理引用对象，减少GC暂停时间",
                ImpactLevel.MEDIUM, isJavaArg = true
            ))
        }

        if (!args.contains("maxgcpausemillis")) {
            suggestions.add(OptimizationSuggestion(
                "JVM参数", "设置MaxGCPauseMillis=200",
                "控制GC最大暂停时间在200毫秒以内",
                ImpactLevel.MEDIUM, isJavaArg = true
            ))
        }

        if (server.memoryMax < 1024) {
            suggestions.add(OptimizationSuggestion(
                "内存分配", "增加最大内存到2048MB",
                "当前分配内存偏小，可能影响服务器性能",
                ImpactLevel.HIGH
            ))
        }

        if (server.memoryMax > 4096 && !args.contains("aikar")) {
            suggestions.add(OptimizationSuggestion(
                "JVM参数", "使用Aikar's Flags优化",
                "Aikar推荐的JVM参数适用于4GB+内存的服务端",
                ImpactLevel.HIGH, isJavaArg = true
            ))
        }

        return suggestions
    }

    fun generateStartupArgs(config: PerformanceConfig): String {
        val args = mutableListOf(
            "-XX:+UseG1GC",
            "-XX:+ParallelRefProcEnabled",
            "-XX:MaxGCPauseMillis=200",
            "-XX:TargetSurvivorRatio=90",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:G1NewSizePercent=30",
            "-XX:G1MaxNewSizePercent=40",
            "-XX:G1HeapRegionSize=8M",
            "-XX:G1ReservePercent=20",
            "-XX:G1HeapWastePercent=5",
            "-XX:G1MixedGCCountTarget=4",
            "-XX:InitiatingHeapOccupancyPercent=15",
            "-XX:G1MixedGCLiveThresholdPercent=90",
            "-XX:G1RSetUpdatingPauseTimePercent=5",
            "-XX:SurvivorRatio=32",
            "-XX:+PerfDisableSharedMem",
            "-XX:MaxTenuringThreshold=1",
            "-Dusing.aikars.flags=true",
            "-Daikars.new.flags=true"
        )

        if (config.useAikarFlags) {
            args.addAll(listOf(
                "-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled",
                "-XX:MaxGCPauseMillis=200", "-XX:+UnlockExperimentalVMOptions",
                "-XX:+DisableExplicitGC", "-XX:+AlwaysPreTouch",
                "-XX:+ThreadLocalHandshakes"
            ))
        }

        return args.joinToString(" ")
    }

    fun calculateOptimalMemory(playerCount: Int, isModded: Boolean): Pair<Int, Int> {
        val base = 1024
        val perPlayer = if (isModded) 200 else 100
        val recommended = base + (playerCount * perPlayer)
        val min = if (isModded) 2048 else 512
        return Pair(min, recommended.coerceAtMost(8192))
    }
}
