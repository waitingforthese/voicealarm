package com.mahaesuvidha.chandrapanchangalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.mahaesuvidha.chandrapanchangalarm.model.BirthProfileStore
import com.mahaesuvidha.chandrapanchangalarm.model.LiveMoonCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.LivePanchangCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.LiveSunCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.NakshatraGuidanceCalculator
import com.mahaesuvidha.chandrapanchangalarm.settings.AlarmPrefs
import com.mahaesuvidha.chandrapanchangalarm.settings.LocationPrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "चंद्र सूर्य अलार्म"
        val message = intent.getStringExtra("message") ?: "पंचांग बदल झाला आहे."
        val id = intent.getIntExtra("id", 1)
        val eventAt = intent.getLongExtra("eventAt", 0L)

        val firedPrefs = context.getSharedPreferences(
            "life_alarm_fired_events", Context.MODE_PRIVATE
        )
        val firedKey = "fired_$id"
        if (eventAt > 0L && firedPrefs.getLong(firedKey, -1L) == eventAt) return
        if (eventAt > 0L) firedPrefs.edit().putLong(firedKey, eventAt).apply()

        val isGuidanceNotification = id == 2 || id == 121 || id == 122
        if (isGuidanceNotification) {
            showNakshatraGuidanceNotification(context, id)
        } else {
            showNotification(context, title, message, id)
        }

        val prefs = AlarmPrefs(context)
        if (prefs.voiceAnnouncement) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            Thread {
                try {
                    VoiceAnnouncement.speakEvent(appContext, id, message, pendingResult)
                } catch (t: Throwable) {
                    android.util.Log.e("LifeAlarm", "Voice announcement failed", t)
                    pendingResult.finish()
                }
            }.start()
        }

        if (id in 1..3 || id in 11..13 || id in 21..27 || id == 121 || id == 122) {
            Thread {
                try {
                    AlarmScheduler(context.applicationContext).scheduleAll()
                } catch (t: Throwable) {
                    android.util.Log.e("LifeAlarm", "Failed to schedule next alarm after id=$id", t)
                }
            }.start()
        }
    }

    private fun showNakshatraGuidanceNotification(context: Context, id: Int) {
        val profile = BirthProfileStore.load(context.applicationContext) ?: return
        if (profile.birthNakshatra.isBlank()) return

        val guidance = runCatching {
            NakshatraGuidanceCalculator.currentGuidance(profile.birthNakshatra)
        }.getOrNull() ?: return

        val title = "🌙 नक्षत्र मार्गदर्शन — ${guidance.nakshatra}"
        val taraLine = "तारा: ${guidance.tara.marathi}"
        val text = "$taraLine\n\nकाय करावे: ${guidance.doText}\n\nकाय टाळावे: ${guidance.avoidText}"
        val bigText = NotificationCompat.BigTextStyle()
            .bigText(text)
            .setBigContentTitle(title)
            .setSummaryText("जन्म नक्षत्र: ${profile.birthNakshatra}")

        val channelId = "life_alarm_nakshatra_guidance_voice_v1"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "नक्षत्र मार्गदर्शन", NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "चालू नक्षत्र, तारा, काय करावे आणि काय टाळावे"
            channel.enableVibration(true)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(
            context, com.mahaesuvidha.chandrapanchangalarm.MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 7000 + id, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("$taraLine • जन्म नक्षत्र: ${profile.birthNakshatra}")
            .setStyle(bigText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setSilent(true)
            .build()

        notificationManager.notify(7000 + id, notification)
    }

    private fun showNotification(context: Context, title: String, message: String, id: Int) {
        val channelId = "life_alarm_voice_v1"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Life Alarm Voice", NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "राशी, नक्षत्र, चरण आणि पंचांग बदलांसाठी Voice Announcement"
            channel.enableVibration(true)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSilent(true)
            .build()

        notificationManager.notify(id, notification)
    }
}

private object VoiceAnnouncement {
    private const val PREFS = "life_alarm_voice_state"

    fun speakEvent(
        context: Context,
        id: Int,
        fallbackMessage: String,
        pendingResult: BroadcastReceiver.PendingResult
    ) {
        val prefs = AlarmPrefs(context)
        if (!prefs.voiceAnnouncement) {
            pendingResult.finish()
            return
        }

        val text = buildAnnouncement(context, id, fallbackMessage)
        val tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                pendingResult.finish()
                return@TextToSpeech
            }

            val mr = Locale("mr", "IN")
            val result = tts.setLanguage(mr)
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale("hi", "IN"))
            }

            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            tts.setSpeechRate(0.92f)
            tts.setPitch(1.06f)

            val music = if (prefs.backgroundMusic) {
                runCatching {
                    MediaPlayer.create(context, com.mahaesuvidha.chandrapanchangalarm.R.raw.voice_background)
                }.getOrNull()
            } else null

            music?.setVolume(0.16f, 0.16f)
            music?.isLooping = true
            music?.start()

            tts.setOnUtteranceProgressListener(object :
                android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) {
                    music?.release()
                    tts.shutdown()
                    pendingResult.finish()
                }
                override fun onDone(utteranceId: String?) {
                    music?.release()
                    tts.shutdown()
                    pendingResult.finish()
                }
            })

            val utteranceId = "life_alarm_${System.currentTimeMillis()}"
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private fun buildAnnouncement(context: Context, id: Int, fallback: String): String {
        val now = System.currentTimeMillis()
        val timeZone = TimeZone.getTimeZone("Asia/Kolkata")

        fun timeOnly(millis: Long): String {
            val f = SimpleDateFormat("h:mm a", Locale.ENGLISH)
            f.timeZone = timeZone
            val raw = f.format(Date(millis))
            return raw.replace("AM", "am").replace("PM", "pm")
        }

        fun until(millis: Long): String =
            if (millis > now) "${timeOnly(millis)} वाजेपर्यंत राहील." else ""

        return runCatching {
            when (id) {
                1 -> {
                    val s = LiveMoonCalculator.getCurrentMoonState()
                    "नमस्कार! चंद्र राशीमध्ये बदल झाला आहे. आता ${s.rashi.marathi} राशी सुरू झाली आहे. ${until(s.nextRashiMillis)}"
                }
                2, 121 -> {
                    val s = LiveMoonCalculator.getCurrentMoonState()
                    "नमस्कार! नक्षत्रामध्ये बदल झाला आहे. आता ${s.nakshatra.marathi} नक्षत्र सुरू झाले आहे. ${until(s.nextNakshatraMillis)}"
                }
                122 -> {
                    val s = LiveMoonCalculator.getCurrentMoonState()
                    val base = "नमस्कार! नक्षत्र मार्गदर्शनाची सूचना आहे. सध्या ${s.nakshatra.marathi} नक्षत्र सुरू आहे. ${until(s.nextNakshatraMillis)}"
                    val profile = BirthProfileStore.load(context)
                    if (profile != null && profile.birthNakshatra.isNotBlank()) {
                        val g = NakshatraGuidanceCalculator.currentGuidance(profile.birthNakshatra, now)
                        "$base ${g.tara.marathi} तारा. काय करावे: ${g.doText} काय टाळावे: ${g.avoidText}"
                    } else base
                }
                3 -> {
                    val s = LiveMoonCalculator.getCurrentMoonState()
                    "नमस्कार! नक्षत्र चरणामध्ये बदल झाला आहे. आता ${s.pada} वा चरण सुरू झाला आहे. ${until(s.nextCharanMillis)}"
                }
                11 -> {
                    val s = LiveSunCalculator.getCurrentSunState()
                    "नमस्कार! सूर्य राशीमध्ये बदल झाला आहे. आता ${s.rashi.marathi} राशी सुरू झाली आहे. ${until(s.nextRashiMillis)}"
                }
                12 -> {
                    val s = LiveSunCalculator.getCurrentSunState()
                    "नमस्कार! सूर्य नक्षत्रामध्ये बदल झाला आहे. आता ${s.nakshatra.marathi} नक्षत्र सुरू झाले आहे. ${until(s.nextNakshatraMillis)}"
                }
                13 -> {
                    val s = LiveSunCalculator.getCurrentSunState()
                    "नमस्कार! सूर्य नक्षत्र चरणामध्ये बदल झाला आहे. आता ${s.pada} वा चरण सुरू झाला आहे. ${until(s.nextCharanMillis)}"
                }
                21, 22, 23, 24, 26, 27 -> {
                    val p = LivePanchangCalculator.getCurrentPanchangState(
                        LocationPrefs(context).latitude, LocationPrefs(context).longitude
                    )
                    when (id) {
                        21 -> "नमस्कार! तिथीमध्ये बदल झाला आहे. आता ${p.tithi} तिथी सुरू झाली आहे. ${until(p.nextTithiMillis)}"
                        22 -> "नमस्कार! योगामध्ये बदल झाला आहे. आता ${p.yoga} योग सुरू झाला आहे. ${until(p.nextYogaMillis)}"
                        23 -> "नमस्कार! करणामध्ये बदल झाला आहे. आता ${p.karana} करण सुरू झाले आहे. ${until(p.nextKaranaMillis)}"
                        24 -> "नमस्कार! पक्षामध्ये बदल झाला आहे. आता ${p.paksha} पक्ष सुरू झाला आहे. ${until(p.nextPakshaMillis)}"
                        26 -> "नमस्कार! प्रहरामध्ये बदल झाला आहे. आता ${p.prahar} प्रहर सुरू झाला आहे. ${until(p.nextPraharMillis)}"
                        else -> "नमस्कार! लग्नामध्ये बदल झाला आहे. आता ${p.lagna} लग्न सुरू झाले आहे. ${until(p.nextLagnaMillis)}"
                    }
                }
                else -> "नमस्कार! $fallback"
            }
        }.getOrElse {
            "नमस्कार! $fallback"
        }
    }
}
