package vn.thanh.ghichu

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.thanh.ghichu.data.EventDatabase
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class CountdownWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val event = EventDatabase.get(context).eventDao().observeAll().first().firstOrNull { it.dateTimeMillis > System.currentTimeMillis() }
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(Color(0xFF1B1D35)).padding(16.dp), verticalAlignment = Alignment.Vertical.Top) {
                Text("Đếm ngược sự kiện", style = TextStyle(color = ColorProvider(Color(0xFFB6A6FF)), fontSize = 14.sp))
                if (event == null) Text("Chưa có sự kiện sắp tới", style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp)) else {
                    Text(event.emoji + " " + event.title, style = TextStyle(color = ColorProvider(Color.White), fontSize = 17.sp))
                    val days = ((event.dateTimeMillis - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)
                    Text("Còn " + days + " ngày · " + SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(event.dateTimeMillis)), style = TextStyle(color = ColorProvider(Color(0xFFFF6B9D)), fontSize = 13.sp))
                }
            }
        }
    }
}
class CountdownWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = CountdownWidget() }
