package vn.thanh.ghichu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
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
import vn.thanh.ghichu.settings.ThemeMode
import java.text.SimpleDateFormat
import java.util.*

internal val Purple = Color(0xFF7C5CFF)
internal val Pink = Color(0xFFFF6B9D)
internal val TodayGreen = Color(0xFF2E9D64)
internal val categories = listOf("Tất cả", "Công việc", "Gia đình", "Du lịch", "Sinh nhật", "Học tập", "Lễ hội", "Khác")
private fun dateText(millis: Long) = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("vi", "VN")).format(Date(millis))

class MainActivity : ComponentActivity() {
    private val notificationRequest = mutableStateOf<Pair<Long, Long>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        publishNotificationIntent(intent)
        setContent { CountdownApp(notificationRequest = notificationRequest.value) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        publishNotificationIntent(intent)
    }

    private fun publishNotificationIntent(intent: android.content.Intent) {
        val eventId = intent.takeIf { it.action == ReminderWorker.ACTION_OPEN_EVENT }
            ?.getLongExtra(ReminderWorker.EXTRA_EVENT_ID, -1L)
            ?.takeIf { it > 0 }
        if (eventId != null) notificationRequest.value = eventId to System.nanoTime()
    }
}

@Composable private fun CountdownApp(
    notificationRequest: Pair<Long, Long>? = null,
    vm: EventViewModel = viewModel()
) {
    val events by vm.events.collectAsState()
    val message by vm.message.collectAsState()
    val theme by vm.theme.collectAsState()
    val notifications by vm.notificationsEnabled.collectAsState()
    val firstDay by vm.firstDayOfWeek.collectAsState()
    val soundUri by vm.soundUri.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) { ThemeMode.SYSTEM -> systemDark; ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val scheme = if (dark) darkColorScheme(primary = Purple, secondary = Pink, background = Color(0xFF0E1020), surface = Color(0xFF1B1D35), onBackground = Color.White, onSurface = Color.White) else lightColorScheme(primary = Color(0xFF6543E8), secondary = Color(0xFFC43E70), background = Color(0xFFF7F5FF), surface = Color.White, onBackground = Color(0xFF19172A), onSurface = Color(0xFF19172A))
    var tab by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf("Tất cả") }
    var editor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EventEntity?>(null) }
    var detail by remember { mutableStateOf<EventEntity?>(null) }
    var deleting by remember { mutableStateOf<EventEntity?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { if (android.os.Build.VERSION.SDK_INT >= 33) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    LaunchedEffect(notificationRequest?.second) {
        val eventId = notificationRequest?.first ?: return@LaunchedEffect
        val event = vm.findEvent(eventId)
        tab = 0
        detail = event
    }
    LaunchedEffect(message) { if (message != null) { delay(1800); vm.clearMessage() } }
    MaterialTheme(colorScheme = scheme) {
        val surface = MaterialTheme.colorScheme.surface
        val background = MaterialTheme.colorScheme.background
        val onSurface = MaterialTheme.colorScheme.onSurface
        Scaffold(containerColor = background, floatingActionButton = { if (tab == 0) FloatingActionButton(onClick = { editing = null; editor = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = RoundedCornerShape(22.dp)) { Icon(Icons.Default.Add, "Thêm sự kiện") } }, bottomBar = {
            NavigationBar(containerColor = surface) {
                listOf("Đếm ngược", "Lịch", "Nhắc nhở", "Cài đặt").forEachIndexed { index, label ->
                    val icons = listOf(Icons.Default.HourglassBottom, Icons.Default.CalendarMonth, Icons.Default.Notifications, Icons.Default.Settings)
                    NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(icons[index], label) }, label = { Text(label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().background(background).padding(padding).padding(horizontal = 18.dp)) {
                when (tab) {
                    0 -> HomeScreen(events, category, { category = it }, { detail = it }, { editing = it; editor = true })
                    1 -> CalendarScreen(events, { detail = it }, firstDay)
                    2 -> ReminderScreen(events, { detail = it })
                    else -> SettingsScreen(theme, vm::setTheme, notifications, vm::setNotifications, firstDay, vm::setFirstDay, soundUri, vm::setSoundUri)
                }
            }
        }
        if (editor) EventEditor(editing, { editor = false }, { vm.save(it); editor = false }, { id -> vm.delete(id); editor = false })
        detail?.let { EventDetail(it, { detail = null }, { editing = it; detail = null; editor = true }, { deleting = it; detail = null }) }
        deleting?.let { event -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Xóa sự kiện?") }, text = { Text("Bạn có chắc muốn xóa “" + event.title + "”?") }, confirmButton = { TextButton(onClick = { vm.delete(event.id); deleting = null }) { Text("Xóa", color = Pink) } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Hủy") } }, containerColor = surface) }
        message?.let { Text(it, Modifier.padding(16.dp), color = onSurface) }
    }
}

@Composable private fun HomeScreen(events: List<EventEntity>, category: String, onCategory: (String) -> Unit, onSelect: (EventEntity) -> Unit, onEdit: (EventEntity) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sort by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val upcoming = events.filter { it.dateTimeMillis >= now }.sortedBy { it.dateTimeMillis }
    val next = upcoming.firstOrNull()
    val categoryNames = (categories + events.filter { it.category == "Khác" }.map { it.displayCategory }).distinct()
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(18.dp))
        Text(SimpleDateFormat("EEEE, d MMMM", Locale("vi", "VN")).format(Date(now)).replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text("Đếm ngược", color = MaterialTheme.colorScheme.onBackground, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        if (next != null) CountdownHero(next, now) else EmptyHero()
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("TẤT CẢ SỰ KIỆN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); TextButton(onClick = { sort = !sort }) { Text(if (sort) "CŨ NHẤT ↑" else "SẮP TỚI ↑", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp) } }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(end = 20.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) { categoryNames.forEach { c -> FilterChip(selected = category == c, onClick = { onCategory(c) }, label = { Text(c, fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary, containerColor = MaterialTheme.colorScheme.surface, labelColor = MaterialTheme.colorScheme.onSurfaceVariant), border = null) } }
        val visible = events.filter { category == "Tất cả" || it.displayCategory == category || (category == "Khác" && it.category == "Khác" && it.customCategoryName.isBlank()) }
            .sortedWith(compareBy<EventEntity> { it.dateTimeMillis < now }.thenBy { if (sort) -it.dateTimeMillis else it.dateTimeMillis })
        if (visible.isEmpty()) EmptyList() else LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(visible, key = { it.id }) { e -> EventCard(e, now, { onSelect(e) }, { onEdit(e) }) } }
    }
}
@Composable private fun CountdownHero(event: EventEntity, now: Long) {
    val delta = ((event.dateTimeMillis - now).coerceAtLeast(0)) / 1000
    val days = delta / 86400
    val numbers = listOf((if (days > 0) days else delta / 3600) to (if (days > 0) "Ngày" else "Giờ"), (if (days > 0) delta % 86400 / 3600 else delta % 3600 / 60) to (if (days > 0) "Giờ" else "Phút"), (if (days > 0) delta % 3600 / 60 else delta % 60) to (if (days > 0) "Phút" else "Giây"), (if (days > 0) delta % 60 else 0) to (if (days > 0) "Giây" else "") )
    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Purple, Pink)), RoundedCornerShape(24.dp)).padding(18.dp)) {
        Text("SẮP TỚI GẦN NHẤT", color = Color.White.copy(.85f), fontSize = 11.sp)
        Text(event.emoji + " " + event.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { numbers.forEach { pair -> Column(Modifier.weight(1f).background(Color.White.copy(.2f), RoundedCornerShape(14.dp)).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(pair.first.toString().padStart(2, '0'), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp); if(pair.second.isNotEmpty()) Text(pair.second, color = Color.White.copy(.8f), fontSize = 9.sp) } } }
        Text(dateText(event.dateTimeMillis), color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
    }
}
@Composable private fun EmptyHero() { Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Purple, Pink)), RoundedCornerShape(24.dp)).padding(22.dp)) { Text("SỰ KIỆN ĐANG CHỜ", color = Color.White.copy(.8f), fontSize = 11.sp); Text("✨ Bắt đầu đếm ngược", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 10.dp)); Text("Thêm sự kiện đầu tiên của bạn", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) } }
@Composable private fun EmptyList() { Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("📅", fontSize = 34.sp); Text("Chưa có sự kiện nào", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); Text("Nhấn nút + để tạo sự kiện mới", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } }
@Composable private fun EventCard(event: EventEntity, now: Long, onClick: () -> Unit, onLongClick: () -> Unit) {
    val days = ((event.dateTimeMillis - now).coerceAtLeast(0)) / 86_400_000L
    val total = (event.dateTimeMillis - event.createdAtMillis).coerceAtLeast(1L)
    val elapsed = (now - event.createdAtMillis).coerceIn(0L, total)
    val progress = elapsed.toFloat() / total.toFloat()
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)), Alignment.Center) { Text(event.emoji, fontSize = 22.sp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(event.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(dateText(event.dateTimeMillis).substringBefore(" · ") + " · " + event.displayCategory, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(7.dp)); Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) { Box(Modifier.fillMaxWidth(progress.coerceIn(.03f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary, CircleShape)) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(if (event.dateTimeMillis < now) "Đã qua" else if (days == 0L) "Hôm nay" else days.toString(), color = when { event.dateTimeMillis < now -> MaterialTheme.colorScheme.onSurfaceVariant; days == 0L -> TodayGreen; days < 30 -> Pink; else -> MaterialTheme.colorScheme.onSurface }, fontWeight = FontWeight.Bold, fontSize = if (days == 0L || event.dateTimeMillis < now) 11.sp else 20.sp); if (days > 0 && event.dateTimeMillis >= now) Text("ngày", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp) }
    }
}
