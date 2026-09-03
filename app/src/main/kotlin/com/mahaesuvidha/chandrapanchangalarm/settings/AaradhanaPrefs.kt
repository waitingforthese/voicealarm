package com.mahaesuvidha.chandrapanchangalarm.settings

import android.content.Context
import com.mahaesuvidha.chandrapanchangalarm.model.BirthProfileStore

class AaradhanaPrefs(private val context: Context) {
    private val prefs get() = context.getSharedPreferences("life_alarm_aaradhana", Context.MODE_PRIVATE)
    private fun key(suffix: String): String {
        val p = BirthProfileStore.load(context.applicationContext)
        val id = p?.let { "${it.name}|${it.birthDate}|${it.birthTime}|${it.birthPlace}" } ?: "default"
        return "${id.hashCode()}_$suffix"
    }
    var specialHourly: Boolean
        get() = prefs.getBoolean(key("special_hourly"), false)
        set(value) = prefs.edit().putBoolean(key("special_hourly"), value).apply()

    /** Number of repetitions for each mantra in special Aaradhana. */
    var specialJapaCount: Int
        get() = prefs.getInt(key("special_japa_count"), 11).coerceIn(1, 108)
        set(value) = prefs.edit().putInt(key("special_japa_count"), value.coerceIn(1, 108)).apply()

    /** Repeat interval in hours for special Aaradhana. */
    var specialIntervalHours: Int
        get() = prefs.getInt(key("special_interval_hours"), 1).coerceIn(1, 24)
        set(value) = prefs.edit().putInt(key("special_interval_hours"), value.coerceIn(1, 24)).apply()

    /** TTS speech rate for Aaradhana; lower values produce slower, clearer chanting. */
    var speechRate: Float
        get() = prefs.getFloat(key("speech_rate"), 0.72f).coerceIn(0.35f, 0.90f)
        set(value) = prefs.edit().putFloat(key("speech_rate"), value.coerceIn(0.35f, 0.90f)).apply()
}
