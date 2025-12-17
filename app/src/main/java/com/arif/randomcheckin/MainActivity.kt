package com.arif.randomcheckin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// MainActivity: Uygulama açılınca çalışan ana Android Activity (ekranın giriş kapısı)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ için bildirim izni isteme
        // İzin verilmezse bildirimler görünmeyebilir
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        // Jetpack Compose ile UI çizimi burada başlar
        setContent {
            RandomCheckInApp()
        }
    }
}

@Composable
fun RandomCheckInApp() {
    // MaterialTheme: Uygulamanın genel UI temasını (renkler, tipografi) uygular
    MaterialTheme {
        GoalListScreen()
    }
}

@Composable
fun GoalListScreen() {

    // context: Android'in uygulama ortamı. WorkManager, DataStore, bildirim gibi şeylerde gerekir
    val context = LocalContext.current

    // GoalStore: hedefi ve ayarları DataStore'a kaydedip okuyan sınıf
    val goalStore = com.arif.randomcheckin.data.model.GoalStore(context)

    // scope: suspend fonksiyonları (saveGoal gibi) UI'ı dondurmadan çalıştırmak için coroutine scope
    val scope = rememberCoroutineScope()

    // showAddScreen: hedef ekleme ekranı mı açık olacak yoksa ana ekran mı
    // remember + mutableStateOf: değer değişince UI otomatik güncellenir
    var showAddScreen by remember { mutableStateOf(false) }

    // goalState: Kaydedilmiş hedefi DataStore'dan canlı şekilde okur
    // null ise hedef yok, dolu ise Triple(title, desc, endDate) gelir
    val goalState = goalStore.goalFlow()
        .collectAsState(initial = null).value

    // Eğer hedef ekleme ekranı açıksa, AddGoalScreen göster
    if (showAddScreen) {
        com.arif.randomcheckin.ui.theme.AddGoalScreen(
            // onSave: kullanıcı kaydete basınca burası çalışır
            onSave = { title, desc, endDate ->
                scope.launch {
                    // 1) Hedefi telefona kaydet (kalıcı)
                    goalStore.saveGoal(title, desc, endDate)

                    // 2) GÜNLÜK bildirim işini başlat (tekil bir WorkManager işi)
                    // Bu ilk planlama: random saat hesabını Worker zaten her seferinde tekrar planlayacak
                    // Burada delay'ı TimeUtils ile random hesaplıyoruz (varsayılan 09:00-21:00 aralığı)
                    val delayMillis =
                        com.arif.randomcheckin.utils.TimeUtils.nextRandomDelayMillis(9 * 60, 21 * 60)

                    val request =
                        androidx.work.OneTimeWorkRequestBuilder<com.arif.randomcheckin.notifications.DailyCheckInWorker>()
                            .setInitialDelay(
                                delayMillis,
                                java.util.concurrent.TimeUnit.MILLISECONDS
                            )
                            .build()

                    // enqueueUniqueWork: aynı isimle sadece 1 job olur, tekrar çağrılırsa eskisini değiştirir
                    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                        "daily_checkin_work",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        request
                    )

                    // 3) Hedef ekleme ekranını kapat, ana ekrana dön
                    showAddScreen = false
                }
            },
            // onCancel: kullanıcı iptal ederse sadece geri döner
            onCancel = {
                showAddScreen = false
            }
        )
        return
    }

    // Ana ekran UI düzeni (dikey kolon)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Başlık
        Text(
            text = "Goals",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hedef yoksa mesaj göster, varsa kart içinde göster
        if (goalState == null) {
            Text("No goal yet.")
        } else {
            val (title, desc, endDate) = goalState

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Bitiş: $endDate")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hedef ekleme ekranını açan buton
        Button(onClick = { showAddScreen = true }) {
            Text("Add goal")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Test butonu: anında bildirim gönderir
        // (Geliştirme/test için kullanışlı. İstersen daha sonra kaldırırız.)
        Button(onClick = {
            com.arif.randomcheckin.notifications.NotificationHelper
                .showCheckInNotification(context)
        }) {
            Text("Test notification")
        }
    }
}
