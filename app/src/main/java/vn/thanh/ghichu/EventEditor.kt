package vn.thanh.ghichu

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import vn.thanh.ghichu.data.EventEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EventEditor(existing: EventEntity?, onDismiss: () -> Unit, onSave: (EventEntity) -> Unit) {
    val context = LocalContext.current
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "Công việc") }
    var emoji by remember(existing) { mutableStateOf(existing?.emoji ?: "🎉") }
    var minutes by remember(existing) { mutableIntStateOf(existing?.reminderMinutes ?: 60) }
    var millis by remember(existing) { mutableLongStateOf(existing?.dateTimeMillis ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0) }.timeInMillis) }
    var error by remember { mutableStateOf("") }
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Card, title = { Text(if (existing == null) "Tạo sự kiện" else "Chỉnh sửa sự kiện", color = Color.White, fontWeight = FontWeight.Bold) }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it; error = "" }, label = { Text("Tên sự kiện *") }, singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Ghi chú") })
            Text("Biểu tượng", color = Muted); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("🎉", "🎂", "✈️", "📝", "🎓", "🎄", "💍").forEach { FilterChip(emoji == it, { emoji = it }, label = { Text(it) }) } }
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded, { expanded = it }) { OutlinedTextField(category, {}, readOnly = true, label = { Text("Danh mục") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor()); ExposedDropdownMenu(expanded, { expanded = false }) { categories.drop(1).forEach { DropdownMenuItem(text = { Text(it) }, onClick = { category = it; expanded = false }) } } }
            OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> val c = Calendar.getInstance().apply { timeInMillis = millis; set(y, m, d) }; millis = c.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }.show() }, modifier = Modifier.fillMaxWidth()) { Text("📅  " + SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(millis))) }
            OutlinedButton(onClick = { TimePickerDialog(context, { _, h, m -> val c = Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }; millis = c.timeInMillis }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show() }, modifier = Modifier.fillMaxWidth()) { Text("🕒  " + SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date(millis))) }
            var reminderExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(reminderExpanded, { reminderExpanded = it }) { OutlinedTextField(when (minutes) { 0 -> "Đúng thời điểm"; 10 -> "10 phút trước"; 60 -> "1 giờ trước"; 1440 -> "1 ngày trước"; else -> "3 ngày trước" }, {}, readOnly = true, label = { Text("Nhắc nhở") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(reminderExpanded) }, modifier = Modifier.menuAnchor()); ExposedDropdownMenu(reminderExpanded, { reminderExpanded = false }) { listOf(0 to "Đúng thời điểm", 10 to "10 phút trước", 60 to "1 giờ trước", 1440 to "1 ngày trước", 4320 to "3 ngày trước").forEach { (v, s) -> DropdownMenuItem(text = { Text(s) }, onClick = { minutes = v; reminderExpanded = false }) } } }
            if (error.isNotEmpty()) Text(error, color = Pink, fontSize = 12.sp)
        }
    }, confirmButton = { TextButton(onClick = { when { title.isBlank() -> error = "Vui lòng nhập tên sự kiện"; millis < System.currentTimeMillis() -> error = "Ngày sự kiện không được ở quá khứ"; else -> onSave(EventEntity(id = existing?.id ?: 0, title = title.trim(), dateTimeMillis = millis, category = category, note = note, reminderMinutes = minutes, emoji = emoji)) } }) { Text("Lưu", color = Purple) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Muted) } })
}

@Composable fun EventDetail(event: EventEntity, onDismiss: () -> Unit, onEdit: (EventEntity) -> Unit, onDelete: (EventEntity) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = Card, title = { Text(event.emoji + " " + event.title, color = Color.White, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("📅  " + SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("vi", "VN")).format(Date(event.dateTimeMillis)), color = Muted); Text("🏷️  " + event.category, color = Muted); if (event.note.isNotBlank()) Text(event.note, color = Color.White); Text("🔔  Nhắc trước " + event.reminderMinutes + " phút", color = Muted) } }, confirmButton = { TextButton(onClick = { onEdit(event) }) { Text("Sửa", color = Purple) } }, dismissButton = { Row { TextButton(onClick = { onDelete(event) }) { Text("Xóa", color = Pink) }; TextButton(onClick = onDismiss) { Text("Đóng", color = Muted) } } })
}
