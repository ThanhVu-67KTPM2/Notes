package vn.thanh.ghichu

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import vn.thanh.ghichu.data.EventEntity
import java.text.SimpleDateFormat
import java.util.*

internal val Bg = Color(0xFF0E1020)
internal val Card = Color(0xFF1B1D35)
internal val Muted = Color(0xFF9CA3C8)
internal val Purple = Color(0xFF7C5CFF)
internal val Pink = Color(0xFFFF6B9D)
internal val categories = listOf("Tất cả", "Công việc", "Gia đình", "Du lịch", "Sinh nhật", "Học tập", "Lễ hội", "Khác")
private fun dateText(millis: Long) = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("vi", "VN")).format(Date(millis))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { CountdownApp() } }
}

@Composable private fun CountdownApp(vm: EventViewModel = viewModel()) {
    val events by vm.events.collectAsState()
    val message by vm.message.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf("Tất cả") }
    var editor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EventEntity?>(null) }
    var detail by remember { mutableStateOf<EventEntity?>(null) }
    var deleting by remember { mutableStateOf<EventEntity?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { if (android.os.Build.VERSION.SDK_INT >= 33) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    LaunchedEffect(message) { if (message != null) { delay(1800); vm.clearMessage() } }
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, secondary = Pink, background = Bg, surface = Card)) {
        Scaffold(containerColor = Bg, floatingActionButton = { if (tab == 0) FloatingActionButton(onClick = { editing = null; editor = true }, containerColor = Purple, contentColor = Color.White, shape = RoundedCornerShape(22.dp)) { Icon(Icons.Default.Add, "Thêm sự kiện") } }, bottomBar = {
            NavigationBar(containerColor = Color(0xFF191B31)) {
                listOf("Đếm ngược", "Lịch", "Nhắc nhở", "Cài đặt").forEachIndexed { index, label ->
                    val icons = listOf(Icons.Default.HourglassBottom, Icons.Default.CalendarMonth, Icons.Default.Notifications, Icons.Default.Settings)
                    NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(icons[index], label) }, label = { Text(label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Purple, selectedTextColor = Purple, indicatorColor = Color(0xFF252344), unselectedIconColor = Muted, unselectedTextColor = Muted))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                when (tab) {
                    0 -> HomeScreen(events, category, { category = it }, { detail = it }, { editing = it; editor = true })
                    1 -> CalendarScreen(events, { detail = it })
                    2 -> ReminderScreen(events, { detail = it })
                    else -> SettingsScreen()
                }
            }
        }
        if (editor) EventEditor(editing, { editor = false }, { vm.save(it); editor = false })
        detail?.let { EventDetail(it, { detail = null }, { editing = it; detail = null; editor = true }, { deleting = it; detail = null }) }
        deleting?.let { event -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Xóa sự kiện?") }, text = { Text("Bạn có chắc muốn xóa “" + event.title + "”? ") }, confirmButton = { TextButton(onClick = { vm.delete(event.id); deleting = null }) { Text("Xóa", color = Pink) } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Hủy") } }, containerColor = Card) }
        message?.let { Text(it, Modifier.padding(16.dp), color = Color.White) }
    }
}

@Composable private fun HomeScreen(events: List<EventEntity>, category: String, onCategory: (String) -> Unit, onSelect: (EventEntity) -> Unit, onEdit: (EventEntity) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sort by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val next = events.firstOrNull { it.dateTimeMillis > now }
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(18.dp))
        Text(SimpleDateFormat("EEEE, d MMMM", Locale("vi", "VN")).format(Date(now)).replaceFirstChar { it.uppercase() }, color = Muted, fontSize = 12.sp)
        Text("Đếm ngược", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        if (next != null) CountdownHero(next, now) else EmptyHero()
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("TẤT CẢ SỰ KIỆN", color = Muted, fontSize = 12.sp); TextButton(onClick = { sort = !sort }) { Text("SẮP XẾP ⌄", color = Muted, fontSize = 11.sp) } }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { categories.forEach { c -> FilterChip(selected = category == c, onClick = { onCategory(c) }, label = { Text(c, fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Purple, selectedLabelColor = Color.White, containerColor = Card, labelColor = Muted), border = null) } }
        val visible = events.filter { category == "Tất cả" || it.category == category }.let { if (sort) it.sortedByDescending { e -> e.dateTimeMillis } else it }
        if (visible.isEmpty()) EmptyList() else LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(visible, key = { it.id }) { e -> EventCard(e, now, { onSelect(e) }, { onEdit(e) }) } }
    }
}
@Composable private fun CountdownHero(event: EventEntity, now: Long) {
    val delta = ((event.dateTimeMillis - now).coerceAtLeast(0)) / 1000
    val numbers = listOf(delta / 86400 to "Ngày", delta % 86400 / 3600 to "Giờ", delta % 3600 / 60 to "Phút", delta % 60 to "Giây")
    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Purple, Pink)), RoundedCornerShape(24.dp)).padding(18.dp)) {
        Text("SẮP TỚI GẦN NHẤT", color = Color.White.copy(.85f), fontSize = 11.sp)
        Text(event.emoji + " " + event.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { numbers.forEach { pair -> Column(Modifier.weight(1f).background(Color.White.copy(.2f), RoundedCornerShape(14.dp)).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(pair.first.toString().padStart(2, '0'), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp); Text(pair.second, color = Color.White.copy(.8f), fontSize = 9.sp) } } }
        Text(dateText(event.dateTimeMillis), color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
    }
}
@Composable private fun EmptyHero() { Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Purple, Pink)), RoundedCornerShape(24.dp)).padding(22.dp)) { Text("SỰ KIỆN ĐANG CHỜ", color = Color.White.copy(.8f), fontSize = 11.sp); Text("✨ Bắt đầu đếm ngược", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 10.dp)); Text("Thêm sự kiện đầu tiên của bạn", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) } }
@Composable private fun EmptyList() { Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("📅", fontSize = 34.sp); Text("Chưa có sự kiện nào", color = Color.White, fontWeight = FontWeight.Bold); Text("Nhấn nút + để tạo sự kiện mới", color = Muted, fontSize = 12.sp) } }
@Composable private fun EventCard(event: EventEntity, now: Long, onClick: () -> Unit, onLongClick: () -> Unit) {
    val days = ((event.dateTimeMillis - now) / 86_400_000L).coerceAtLeast(0)
    Row(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(20.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(Color(0xFF252743), RoundedCornerShape(16.dp)), Alignment.Center) { Text(event.emoji, fontSize = 22.sp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(event.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(dateText(event.dateTimeMillis).substringBefore(" · ") + " · " + event.category, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(7.dp)); Box(Modifier.fillMaxWidth(.75f).height(3.dp).background(Purple.copy(.24f), CircleShape))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(days.toString(), color = if (days < 30) Pink else Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("ngày", color = Muted, fontSize = 9.sp) }
    }
}
