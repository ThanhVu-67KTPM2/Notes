package vn.thanh.ghichu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import vn.thanh.ghichu.data.EventEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable fun CalendarScreen(events: List<EventEntity>, onSelect: (EventEntity) -> Unit) {
    var month by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var year by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    val cal = Calendar.getInstance().apply { set(year, month, 1) }
    Column(Modifier.fillMaxSize().padding(top = 22.dp)) {
        Text("Lịch sự kiện", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (month == 0) { month = 11; year-- } else month-- }) { Text("‹", fontSize = 24.sp, color = Purple) }
            Text(SimpleDateFormat("MMMM yyyy", Locale("vi", "VN")).format(cal.time).replaceFirstChar { it.uppercase() }, color = Color.White, fontWeight = FontWeight.Bold)
            TextButton(onClick = { if (month == 11) { month = 0; year++ } else month++ }) { Text("›", fontSize = 24.sp, color = Purple) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { Text(it, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(8.dp)) } }
        val firstDay = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cells = List(firstDay) { null } + (1..maxDay).map { it }
        cells.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { week.forEach { day -> val match = day?.let { d -> events.firstOrNull { val c = Calendar.getInstance().apply { timeInMillis = it.dateTimeMillis }; c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month && c.get(Calendar.DAY_OF_MONTH) == d } }; Box(Modifier.size(40.dp), Alignment.Center) { if (day != null) TextButton(onClick = { match?.let(onSelect) }, contentPadding = PaddingValues(0.dp)) { Text(day.toString() + if (match != null) " •" else "", color = if (match != null) Pink else Color.White, fontSize = 14.sp) } } } } }
        Text("Sự kiện trong tháng", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        val monthly = events.filter { e -> val c = Calendar.getInstance().apply { timeInMillis = e.dateTimeMillis }; c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month }
        if (monthly.isEmpty()) Text("Tháng này chưa có sự kiện", color = Muted, modifier = Modifier.padding(16.dp)) else LazyColumn { items(monthly) { e -> Card(onClick = { onSelect(e) }, colors = CardDefaults.cardColors(containerColor = Card), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(e.emoji, fontSize = 23.sp); Column(Modifier.padding(start = 12.dp)) { Text(e.title, color = Color.White, fontWeight = FontWeight.Bold); Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("vi", "VN")).format(Date(e.dateTimeMillis)), color = Muted, fontSize = 12.sp) } } } } }
    }
}

@Composable fun ReminderScreen(events: List<EventEntity>, onSelect: (EventEntity) -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 22.dp)) {
        Text("Nhắc nhở", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Lịch nhắc cho các sự kiện sắp tới", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
        val upcoming = events.filter { it.dateTimeMillis > System.currentTimeMillis() }.sortedBy { it.dateTimeMillis }
        if (upcoming.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Chưa có nhắc nhở nào", color = Muted) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(upcoming) { event -> Card(onClick = { onSelect(event) }, colors = CardDefaults.cardColors(containerColor = Card), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("🔔", fontSize = 24.sp); Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(event.title, color = Color.White, fontWeight = FontWeight.Bold); Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("vi", "VN")).format(Date(event.dateTimeMillis)), color = Muted, fontSize = 12.sp); Text("Nhắc trước " + event.reminderMinutes + " phút", color = Purple, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) } } } } }
    }
}

@Composable fun SettingsScreen() {
    var notifications by remember { mutableStateOf(true) }
    var firstDay by remember { mutableStateOf("Thứ Hai") }
    Column(Modifier.fillMaxSize().padding(top = 22.dp)) {
        Text("Cài đặt", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Tùy chỉnh trải nghiệm của bạn", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Card), modifier = Modifier.fillMaxWidth()) { Column {
            SettingRow("🔔", "Thông báo", "Nhận nhắc nhở sự kiện") { Switch(notifications, { notifications = it }) }
            HorizontalDivider(color = Bg)
            SettingRow("📅", "Ngày đầu tuần", firstDay) { var show by remember { mutableStateOf(false) }; TextButton(onClick = { show = true }) { Text(firstDay, color = Purple) }; if (show) AlertDialog(onDismissRequest = { show = false }, title = { Text("Ngày đầu tuần") }, text = { Column { listOf("Thứ Hai", "Chủ Nhật").forEach { TextButton(onClick = { firstDay = it; show = false }) { Text(it) } } } }, confirmButton = {}) }
            HorizontalDivider(color = Bg)
            SettingRow("🎨", "Giao diện", "Tối · Tím mộng mơ") { Text("Tối", color = Purple, modifier = Modifier.padding(12.dp)) }
            HorizontalDivider(color = Bg)
            SettingRow("ℹ️", "Về ứng dụng", "Đếm ngược sự kiện · Phiên bản 1.0") { }
        } }
    }
}
@Composable private fun SettingRow(emoji: String, title: String, subtitle: String, trailing: @Composable () -> Unit) { Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(emoji, fontSize = 21.sp); Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 11.sp) }; trailing() } }
