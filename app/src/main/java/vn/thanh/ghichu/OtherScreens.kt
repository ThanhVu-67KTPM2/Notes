package vn.thanh.ghichu

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import vn.thanh.ghichu.data.EventEntity
import vn.thanh.ghichu.settings.ThemeMode
import java.text.SimpleDateFormat
import java.util.*

@Composable fun CalendarScreen(events: List<EventEntity>, onSelect: (EventEntity) -> Unit, firstDayOfWeek: String) {
    var month by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var year by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    val cal = Calendar.getInstance().apply { set(year, month, 1) }
    val mondayFirst = firstDayOfWeek == "Thứ Hai"
    val offset = if (mondayFirst) (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 else cal.get(Calendar.DAY_OF_WEEK) - 1
    Column(Modifier.fillMaxSize().padding(top = 22.dp)) {
        Text("Lịch sự kiện", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (month == 0) { month = 11; year-- } else month-- }) { Text("‹", fontSize = 24.sp) }
            Text(SimpleDateFormat("MMMM yyyy", Locale("vi", "VN")).format(cal.time).replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            TextButton(onClick = { if (month == 11) { month = 0; year++ } else month++ }) { Text("›", fontSize = 24.sp) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { (if (mondayFirst) listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN") else listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")).forEach { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(8.dp)) } }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cells: List<Int?> = List(offset) { null } + (1..maxDay).map { it }
        cells.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { week.forEach { day -> val matches = day?.let { d -> events.filter { e -> val c = Calendar.getInstance().apply { timeInMillis = e.dateTimeMillis }; c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month && c.get(Calendar.DAY_OF_MONTH) == d } }.orEmpty(); Box(Modifier.size(40.dp), Alignment.Center) { if (day != null) TextButton(onClick = { matches.firstOrNull()?.let(onSelect) }, contentPadding = PaddingValues(0.dp)) { Text(day.toString() + if (matches.isNotEmpty()) " •" else "", color = if (matches.isNotEmpty()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp) } } } } }
        Text("Sự kiện trong tháng", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        val monthly = events.filter { e -> val c = Calendar.getInstance().apply { timeInMillis = e.dateTimeMillis }; c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month }.sortedBy { it.dateTimeMillis }
        if (monthly.isEmpty()) Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) { Text("Tháng này chưa có sự kiện", color = MaterialTheme.colorScheme.onSurfaceVariant) } else LazyColumn(Modifier.weight(1f)) { items(monthly) { e -> Card(onClick = { onSelect(e) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(e.emoji, fontSize = 23.sp); Column(Modifier.padding(start = 12.dp)) { Text(e.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("vi", "VN")).format(Date(e.dateTimeMillis)) + " · " + e.displayCategory, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } } } } }
    }
}

@Composable fun ReminderScreen(events: List<EventEntity>, onSelect: (EventEntity) -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 22.dp)) {
        Text("Nhắc nhở", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Lịch nhắc cho các sự kiện sắp tới", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
        val upcoming = events.filter { it.dateTimeMillis > System.currentTimeMillis() }.sortedBy { it.dateTimeMillis }
        if (upcoming.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Chưa có nhắc nhở nào", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(upcoming) { event -> Card(onClick = { onSelect(event) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("🔔", fontSize = 24.sp); Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(event.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("vi", "VN")).format(Date(event.dateTimeMillis)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text("Nhắc trước " + event.reminderMinutes + " phút", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) } } } } }
    }
}

@Composable fun SettingsScreen(theme: ThemeMode, setTheme: (ThemeMode) -> Unit, notifications: Boolean, setNotifications: (Boolean) -> Unit, firstDay: String, setFirstDay: (String) -> Unit, soundUri: String?, setSoundUri: (String?) -> Unit) {
    var showTheme by remember { mutableStateOf(false) }
    var showWeek by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showSound by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            setSoundUri(uri.toString())
        }
    }
    val soundName = if (soundUri == null) "Dùng mặc định" else "Tệp âm thanh đã chọn"
    val themeName = when (theme) { ThemeMode.SYSTEM -> "Theo hệ thống"; ThemeMode.LIGHT -> "Sáng"; ThemeMode.DARK -> "Tối" }
    Column(Modifier.fillMaxSize().padding(top = 22.dp)) {
        Text("Cài đặt", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Tùy chỉnh trải nghiệm của bạn", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) { Column {
            SettingRow("🔔", "Thông báo", "Nhận nhắc nhở sự kiện", { Switch(checked = notifications, onCheckedChange = setNotifications) }, { setNotifications(!notifications) })
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            SettingRow("📅", "Ngày đầu tuần", firstDay, { Text(firstDay, color = MaterialTheme.colorScheme.primary) }, { showWeek = true })
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            SettingRow("🎨", "Giao diện", themeName, { Text(themeName, color = MaterialTheme.colorScheme.primary) }, { showTheme = true })
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            SettingRow("🔊", "Âm thanh thông báo", soundName, { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp) }, { showSound = true })
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            SettingRow("ℹ️", "Về ứng dụng", "Thông tin phiên bản và góp ý", { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp) }, { showAbout = true })
        } }
    }
    if (showTheme) ChoiceDialog("Giao diện", listOf("Theo hệ thống", "Sáng", "Tối"), themeName, { label -> setTheme(when (label) { "Sáng" -> ThemeMode.LIGHT; "Tối" -> ThemeMode.DARK; else -> ThemeMode.SYSTEM }); showTheme = false }, { showTheme = false })
    if (showWeek) ChoiceDialog("Ngày đầu tuần", listOf("Thứ Hai", "Chủ Nhật"), firstDay, { setFirstDay(it); showWeek = false }, { showWeek = false })
    if (showSound) AlertDialog(onDismissRequest = { showSound = false; player?.release(); player = null }, containerColor = MaterialTheme.colorScheme.surface, title = { Text("Âm thanh thông báo") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Âm thanh áp dụng cho tất cả lời nhắc.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (soundUri == null) "Đang dùng âm thanh mặc định của Android" else "Đang dùng tệp âm thanh từ thiết bị", color = MaterialTheme.colorScheme.onSurface)
        OutlinedButton(onClick = { picker.launch(arrayOf("audio/mpeg", "audio/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Chọn tệp MP3") }
        OutlinedButton(onClick = { player?.release(); player = null; setSoundUri(null) }, modifier = Modifier.fillMaxWidth()) { Text("Dùng mặc định") }
        OutlinedButton(onClick = {
            player?.release()
            player = null
            val uri = soundUri?.let(Uri::parse)
                ?: Uri.parse("android.resource://${context.packageName}/${R.raw.ringtone}")
            val preview = runCatching { MediaPlayer.create(context, uri) }.getOrNull()
            if (preview == null) {
                android.media.RingtoneManager.getRingtone(context, uri)?.play()
            } else {
                player = preview
                preview.setOnCompletionListener { completed ->
                    completed.release()
                    if (player === completed) player = null
                }
                preview.start()
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Nghe thử") }
        if (soundUri != null) {
            Text("Nếu Android đã tạo kênh thông báo, hệ thống có thể giữ âm thanh cũ.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, ReminderWorker.CHANNEL_ID)) }) { Text("Mở cài đặt kênh thông báo") }
        }
    } }, confirmButton = { TextButton(onClick = { showSound = false; player?.release(); player = null }) { Text("Đóng") } })
    if (showAbout) AlertDialog(onDismissRequest = { showAbout = false }, containerColor = MaterialTheme.colorScheme.surface, title = { Text("Đếm ngược sự kiện", color = MaterialTheme.colorScheme.onSurface) }, text = { Column { Text("Phiên bản 1.0", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Tác giả: Đội ngũ phát triển", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)); TextButton(onClick = { showAbout = false }) { Text("Gửi góp ý qua email", color = MaterialTheme.colorScheme.primary) } } }, confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Đóng") } })
}
@Composable private fun ChoiceDialog(title: String, options: List<String>, selected: String, choose: (String) -> Unit, dismiss: () -> Unit) { AlertDialog(onDismissRequest = dismiss, containerColor = MaterialTheme.colorScheme.surface, title = { Text(title, color = MaterialTheme.colorScheme.onSurface) }, text = { Column { options.forEach { option -> TextButton(onClick = { choose(option) }, modifier = Modifier.fillMaxWidth()) { Text((if (option == selected) "✓  " else "") + option, color = MaterialTheme.colorScheme.onSurface) } } } }, confirmButton = {}) }
@Composable private fun SettingRow(emoji: String, title: String, subtitle: String, trailing: @Composable () -> Unit, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(emoji, fontSize = 21.sp); Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }; trailing() } }
