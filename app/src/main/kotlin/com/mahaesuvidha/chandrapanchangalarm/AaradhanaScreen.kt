package com.mahaesuvidha.chandrapanchangalarm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahaesuvidha.chandrapanchangalarm.model.AaradhanaMaster
import com.mahaesuvidha.chandrapanchangalarm.alarm.AlarmScheduler
import com.mahaesuvidha.chandrapanchangalarm.alarm.AaradhanaVoiceSession
import com.mahaesuvidha.chandrapanchangalarm.model.BirthProfile
import com.mahaesuvidha.chandrapanchangalarm.model.LiveMoonCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.PanchangState
import com.mahaesuvidha.chandrapanchangalarm.settings.AaradhanaPrefs

@Composable
fun AaradhanaScreen(
    profile: BirthProfile,
    panchang: PanchangState,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { AaradhanaPrefs(context.applicationContext) }
    var special by remember { mutableStateOf(prefs.specialHourly) }
    var japaCountText by remember { mutableStateOf(prefs.specialJapaCount.toString()) }
    var intervalHoursText by remember { mutableStateOf(prefs.specialIntervalHours.toString()) }
    var speechRate by remember { mutableStateOf(prefs.speechRate) }
    var savedPopup by remember { mutableStateOf(false) }
    val moon = remember { LiveMoonCalculator.getCurrentMoonState() }
    val nakInfo = AaradhanaMaster.forNakshatra(moon.nakshatra.marathi)
    val yogaInfo = AaradhanaMaster.forYoga(panchang.yoga)
    val karanaInfo = AaradhanaMaster.forKarana(panchang.karana)

    Column(
        Modifier.fillMaxSize().background(Color(0xFF07111F)).verticalScroll(rememberScrollState()).padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("🕉️ नक्षत्र आराधना", color = Color(0xFFFFC83D), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("परत", color = Color.White) }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10253A))) {
            Column(Modifier.padding(14.dp)) {
                Text("👤 ${profile.name}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("जन्म नक्षत्र: ${profile.birthNakshatra.ifBlank { "—" }}", color = Color.LightGray)
                Text("चंद्र नक्षत्र: ${moon.nakshatra.marathi} • चरण ${moon.pada}", color = Color.LightGray)
                Text("गोचर नक्षत्र: ${moon.nakshatra.marathi}", color = Color.LightGray)
                Text("योग: ${panchang.yoga}", color = Color.LightGray)
                Text("करण: ${panchang.karana}", color = Color.LightGray)
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10253A))) {
            Column(Modifier.padding(14.dp)) {
                Text("🔔 बदलाची आराधना", color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("नक्षत्र / योग / करण बदलल्यावर संबंधित देवता व मंत्रासह ११ जप आपोआप होतील.", color = Color.LightGray, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                MantraRow("🌟 नक्षत्र", moon.nakshatra.marathi, nakInfo.deity, nakInfo.mantra)
                MantraRow("🕉️ योग", panchang.yoga, yogaInfo.deity, yogaInfo.mantra)
                MantraRow("🔱 करण", panchang.karana, karanaInfo.deity, karanaInfo.mantra)
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10253A))) {
            Column(Modifier.padding(14.dp)) {
                Text("🕉️ विशेष आराधना", color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("स्वयंचलित आराधना", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("ON असल्यास ठरलेल्या अंतराने जप सुरू होईल.", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Switch(checked = special, onCheckedChange = {
                        special = it
                        prefs.specialHourly = it
                        AlarmScheduler(context.applicationContext).scheduleAll()
                    })
                }

                Spacer(Modifier.height(12.dp))
                Text("⚙️ जपाची सेटिंग", color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = japaCountText,
                        onValueChange = { japaCountText = it.filter(Char::isDigit).take(3) },
                        label = { Text("जप संख्या") },
                        supportingText = { Text("1 ते 108") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFC83D),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFFFFC83D),
                            unfocusedLabelColor = Color.LightGray
                        )
                    )
                    OutlinedTextField(
                        value = intervalHoursText,
                        onValueChange = { intervalHoursText = it.filter(Char::isDigit).take(2) },
                        label = { Text("प्रत्येक किती तासांनी") },
                        supportingText = { Text("1 ते 24 तास") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFC83D),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFFFFC83D),
                            unfocusedLabelColor = Color.LightGray
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text("🐢 जपाचा आवाज किती संथ ठेवायचा", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("सध्याचा वेग: ${String.format(java.util.Locale.US, "%.2f", speechRate)}  • कमी = अधिक हळू", color = Color.LightGray, fontSize = 12.sp)
                Slider(
                    value = speechRate,
                    onValueChange = { speechRate = it },
                    valueRange = 0.35f..0.90f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("अधिक संथ", color = Color.LightGray, fontSize = 11.sp)
                    Text("जलद", color = Color.LightGray, fontSize = 11.sp)
                }

                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val count = japaCountText.toIntOrNull()?.coerceIn(1, 108) ?: 11
                    val hours = intervalHoursText.toIntOrNull()?.coerceIn(1, 24) ?: 1
                    japaCountText = count.toString()
                    intervalHoursText = hours.toString()
                    prefs.specialJapaCount = count
                    prefs.specialIntervalHours = hours
                    prefs.speechRate = speechRate

                    // Reconcile the saved schedule immediately.
                    AlarmScheduler(context.applicationContext).scheduleAll()

                    // Immediately play the currently active Aaradhana so the user can
                    // verify count, pronunciation and speech speed without waiting for an alarm.
                    val previewMantras = listOf(
                        nakInfo.mantra,
                        yogaInfo.mantra,
                        karanaInfo.mantra
                    ).filter { it.isNotBlank() }
                    AaradhanaVoiceSession.speakPreview(
                        context.applicationContext,
                        399,
                        previewMantras,
                        count
                    )
                    savedPopup = true
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("💾 जतन करा + आत्ताच आराधना ऐका", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(6.dp))
                Text("ON असल्यास: नक्षत्र मंत्र → योग मंत्र → करण मंत्र • प्रत्येक मंत्रासाठी निवडलेली जप संख्या.", color = Color.LightGray, fontSize = 12.sp)
                Text("या विशेष आराधनेत कोणतीही घोषणा केली जाणार नाही.", color = Color.LightGray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("📖 मार्गदर्शन स्वतंत्र Part मध्ये राहील.", color = Color.LightGray, fontSize = 13.sp)
    }

    if (savedPopup) {
        AlertDialog(
            onDismissRequest = { savedPopup = false },
            title = { Text("✅ आराधना सेटिंग जतन झाली") },
            text = {
                Column {
                    Text("जप संख्या: $japaCountText")
                    Text("अंतर: $intervalHoursText तास")
                    Text("आवाजाचा वेग: ${String.format(java.util.Locale.US, "%.2f", speechRate)}")
                    Spacer(Modifier.height(8.dp))
                    Text("🕉️ सध्याची नक्षत्र + योग + करण आराधना आत्ताच सुरू केली आहे.")
                    Text("आवाज/स्पीड तपासण्यासाठी ऐका. पुढील alarm सेव्ह केलेल्या setting प्रमाणे schedule झाला आहे.")
                }
            },
            confirmButton = {
                TextButton(onClick = { savedPopup = false }) { Text("ठीक आहे") }
            }
        )
    }
}

@Composable
private fun MantraRow(label: String, value: String, deity: String, mantra: String) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Text("$label: $value", color = Color.White, fontWeight = FontWeight.Bold)
        Text("🙏 अधिदेवता: $deity", color = Color(0xFFFFC83D), fontSize = 13.sp)
        Text("📿 $mantra", color = Color.White, fontSize = 13.sp)
    }
}
