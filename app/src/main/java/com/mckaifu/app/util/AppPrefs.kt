package com.mckaifu.app.util

import android.content.Context
import com.mckaifu.app.data.model.TunnelInfo
import kotlinx.serialization.json.Json

object AppPrefs {
    private const val NAME = "mckaifu_prefs"
    const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val PREFIX_TUNNEL = "tunnel_info_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isOnboardingDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    fun saveTunnelInfo(context: Context, serverId: String, info: TunnelInfo) {
        try {
            val json = Json.encodeToString(TunnelInfo.serializer(), info)
            prefs(context).edit().putString(PREFIX_TUNNEL + serverId, json).apply()
        } catch (_: Exception) {}
    }

    fun loadTunnelInfo(context: Context, serverId: String): TunnelInfo? {
        val raw = prefs(context).getString(PREFIX_TUNNEL + serverId, null) ?: return null
        return try {
            Json.decodeFromString(TunnelInfo.serializer(), raw)
        } catch (_: Exception) {
            null
        }
    }

    fun clearTunnelInfo(context: Context, serverId: String) {
        prefs(context).edit().remove(PREFIX_TUNNEL + serverId).apply()
    }
}
