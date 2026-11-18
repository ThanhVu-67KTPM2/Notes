package vn.thanh.ghichu

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import vn.thanh.ghichu.data.EventEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EventEditor(existing: EventEntity?, onDismiss: () -> Unit, onSave: (EventEntity) -> Unit, onDelete: (Long) -> Unit) {
    val context = LocalContext.current
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "Công việc") }
    var customCategory by remember(existing) { mutableStateOf(existing?.customCategoryName ?: "") }
    var emoji by remember(existing) { mutableStateOf(existing?.emoji ?: "🎉") }
    var minutes by remember(existing) { mutableIntStateOf(existing?.reminderMinutes ?: 60) }
    var millis by remember(existing) { mutableLongStateOf(existing?.dateTimeMillis ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0) }.timeInMillis) }
    var timeMode by remember(existing) { mutableStateOf(existing?.timeMode ?: "DATE") }
    var relativeDaysText by remember(existing) { mutableStateOf(existing?.relativeDays?.takeIf { it > 0 }?.toString() ?: "") }
    var relativeError by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var categoryError by remember { mutableStateOf("") }
    var deleteConfirm by remember { mutableStateOf(false) }
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, title = { Text(if (existing == null) "Tạo sự kiện" else "Chỉnh sửa sự kiện", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { value -> title = value.take(60); error = "" }, label = { Text("Tên sự kiện *") }, supportingText = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error); Text(title.length.toString() + "/60", color = MaterialTheme.colorScheme.onSurfaceVariant) } }, isError = error.isNotEmpty(), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth())
            Text("Biểu tượng: " + emoji, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("🎉", "🎂", "✈️", "📝", "🎓", "🎄", "💍").forEach { icon -> FilterChip(selected = emoji == icon, onClick = { emoji = icon }, label = { Text(icon, fontSize = 18.sp) }, border = if (emoji == icon) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) } }
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded, { expanded = it }) { OutlinedTextField(category, {}, readOnly = true, label = { Text("Danh mục") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth()); ExposedDropdownMenu(expanded, { expanded = false }) { categories.drop(1).forEach { DropdownMenuItem(text = { Text(it) }, onClick = { category = it; expanded = false; categoryError = "" }) } } }
            if (category == "Khác") OutlinedTextField(customCategory, { value -> customCategory = value.take(20); categoryError = "" }, label = { Text("Tên danh mục *") }, supportingText = { if (categoryError.isNotEmpty()) Text(categoryError, color = MaterialTheme.colorScheme.error) else Text(customCategory.length.toString() + "/20") }, isError = categoryError.isNotEmpty(), singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = timeMode == "DATE", onClick = { timeMode = "DATE" }, label = { Text("Ngày cụ thể") })
                FilterChip(selected = timeMode == "RELATIVE", onClick = { timeMode = "RELATIVE" }, label = { Text("Sau số ngày") })
            }
            if (timeMode == "RELATIVE") {
                OutlinedTextField(relativeDaysText, { value -> relativeDaysText = value.filter(Char::isDigit).take(4); relativeError = "" }, label = { Text("Số ngày *") }, supportingText = { Text(if (relativeError.isNotEmpty()) relativeError else "Từ 1 đến 3650 ngày", color = if (relativeError.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }, isError = relativeError.isNotEmpty(), singleLine = true, modifier = Modifier.fillMaxWidth())
            } else {
                OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> millis = Calendar.getInstance().apply { timeInMillis = millis; set(y, m, d) }.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }.show() }, modifier = Modifier.fillMaxWidth()) { Text("📅  " + SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(millis))) }
            }
            OutlinedButton(onClick = { TimePickerDialog(context, { _, h, m -> millis = Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }.timeInMillis }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show() }, modifier = Modifier.fillMaxWidth()) { Text("🕒  " + SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date(millis))) }
            var reminderExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(reminderExpanded, { reminderExpanded = it }) { OutlinedTextField(when (minutes) { 0 -> "Đúng thời điểm"; 10 -> "10 phút trước"; 60 -> "1 giờ trước"; 1440 -> "1 ngày trước"; else -> "3 ngày trước" }, {}, readOnly = true, label = { Text("Nhắc nhở") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(reminderExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth()); ExposedDropdownMenu(reminderExpanded, { reminderExpanded = false }) { listOf(0 to "Đúng thời điểm", 10 to "10 phút trước", 60 to "1 giờ trước", 1440 to "1 ngày trước", 4320 to "3 ngày trước").forEach { (v, s) -> DropdownMenuItem(text = { Text(s) }, onClick = { minutes = v; reminderExpanded = false }) } } }
            if (millis <= System.currentTimeMillis()) Text("Ngày và giờ đã chọn ở quá khứ", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }, confirmButton = { Row { if (existing != null) TextButton(onClick = { deleteConfirm = true }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }; TextButton(onClick = onDismiss) { Text("Hủy") }; TextButton(onClick = {
        val days = relativeDaysText.toIntOrNull()
        when {
            title.isBlank() -> error = "Vui lòng nhập tên sự kiện"
            category == "Khác" && customCategory.isBlank() -> categoryError = "Vui lòng nhập tên danh mục"
            timeMode == "RELATIVE" && (days == null || days !in 1..3650) -> relativeError = "Nhập số nguyên từ 1 đến 3650"
            else -> {
                val target = if (timeMode == "RELATIVE") Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, days!!); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, cal.get(Calendar.MINUTE)) }.timeInMillis else millis
                if (target <= System.currentTimeMillis()) error = "Ngày và giờ sự kiện phải ở tương lai" else onSave(EventEntity(id = existing?.id ?: 0, title = title.trim(), dateTimeMillis = target, category = category, customCategoryName = customCategory.trim(), note = note, reminderMinutes = minutes, emoji = emoji, createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(), timeMode = timeMode, relativeDays = if (timeMode == "RELATIVE") days!! else 0))
            }
        }
    }) { Text("Lưu", color = MaterialTheme.colorScheme.primary) } } })
    if (deleteConfirm && existing != null) AlertDialog(onDismissRequest = { deleteConfirm = false }, title = { Text("Xóa sự kiện?") }, text = { Text("Bạn có chắc muốn xóa sự kiện này không?") }, confirmButton = { TextButton(onClick = { deleteConfirm = false; onDelete(existing.id); onDismiss() }) { Text("Xóa", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("Hủy") } })
}

@Composable fun EventDetail(event: EventEntity, onDismiss: () -> Unit, onEdit: (EventEntity) -> Unit, onDelete: (EventEntity) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, title = { Text(event.emoji + " " + event.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("📅  " + SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("vi", "VN")).format(Date(event.dateTimeMillis)), color = MaterialTheme.colorScheme.onSurfaceVariant); Text("🏷️  " + event.displayCategory, color = MaterialTheme.colorScheme.onSurfaceVariant); if (event.note.isNotBlank()) Text(event.note, color = MaterialTheme.colorScheme.onSurface); Text("🔔  Nhắc trước " + event.reminderMinutes + " phút", color = MaterialTheme.colorScheme.onSurfaceVariant) } }, confirmButton = { TextButton(onClick = { onEdit(event) }) { Text("Sửa") } }, dismissButton = { Row { TextButton(onClick = { onDelete(event) }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }; TextButton(onClick = onDismiss) { Text("Đóng") } } })
}
