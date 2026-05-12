package com.syauqialfanzari0008.studytask

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.syauqialfanzari0008.studytask.ui.theme.StudyTaskTheme
import java.text.SimpleDateFormat
import java.util.*
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

val CATEGORIES = listOf("Umum", "Kuliah", "Kerja", "Personal", "Belanja", "Kesehatan")

const val POMODORO_CHANNEL_ID = "pomodoro_channel"

fun formatDateMillis(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(millis))
}

fun todayString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

fun daysAgoString(daysAgo: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(cal.time)
}

fun shortDayLabel(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(dateStr) ?: return dateStr
        val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
        dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
    } catch (e: Exception) { dateStr }
}

fun calculateStreak(taskList: List<Task>): Int {
    val completedDates = taskList.filter { it.isDone && it.completedDate.isNotEmpty() }
        .map { it.completedDate }.toSet()
    if (completedDates.isEmpty()) return 0
    var streak = 0
    var checkDay = 0
    while (true) {
        val dayStr = daysAgoString(checkDay)
        if (completedDates.contains(dayStr)) { streak++; checkDay++ } else break
    }
    return streak
}

fun weeklyCompletedData(taskList: List<Task>): List<Pair<String, Int>> {
    return (6 downTo 0).map { daysAgo ->
        val dayStr = daysAgoString(daysAgo)
        val count = taskList.count { it.isDone && it.completedDate == dayStr }
        shortDayLabel(dayStr) to count
    }
}

fun sendPomodoroNotification(context: Context, title: String, message: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            POMODORO_CHANNEL_ID,
            "Pomodoro Timer",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Notifikasi sesi Pomodoro" }
        manager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, POMODORO_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    manager.notify(System.currentTimeMillis().toInt(), notification)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        }

        val db = TaskDatabase.getDatabase(this)
        val dao = db.taskDao()
        setContent {
            val context = applicationContext
            val userPrefs = remember { UserPreferences(context) }
            val isDarkMode by userPrefs.darkMode.collectAsStateWithLifecycle(initialValue = false)
            val scope = rememberCoroutineScope()
            StudyTaskTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TodoScreen(
                        dao = dao,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { scope.launch { userPrefs.saveDarkMode(!isDarkMode) } }
                    )
                }
            }
        }
    }
}

fun priorityColor(priority: String): Color = when (priority) {
    "High" -> Color(0xFFEF4444)
    "Medium" -> Color(0xFFF59E0B)
    else -> Color(0xFF22C55E)
}

fun categoryColor(category: String): Color = when (category) {
    "Kuliah" -> Color(0xFF4F46E5)
    "Kerja" -> Color(0xFF0891B2)
    "Personal" -> Color(0xFFDB2777)
    "Belanja" -> Color(0xFFD97706)
    "Kesehatan" -> Color(0xFF16A34A)
    else -> Color(0xFF6B7280)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerButton(selectedDate: String, onDateSelected: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = if (selectedDate.isEmpty()) "Pilih due date" else selectedDate, fontSize = 14.sp)
    }
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(formatDateMillis(it)) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Batal") } }
        ) { DatePicker(state = datePickerState) }
    }
}

fun konfettiParty(): List<Party> = listOf(
    Party(
        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(80),
        position = Position.Relative(0.5, 0.0), spread = 360,
        colors = listOf(0xFF4F46E5.toInt(), 0xFF7C3AED.toInt(), 0xFF16A34A.toInt(), 0xFFF59E0B.toInt(), 0xFFEF4444.toInt(), 0xFF06B6D4.toInt())
    )
)

@Composable
fun DeleteConfirmDialog(
    taskTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                "Hapus Task?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                "\"$taskTitle\" akan dihapus permanen dan tidak bisa dikembalikan.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hapus", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDeleteBackground(dismissDirection: SwipeToDismissBoxValue) {
    val color by animateColorAsState(
        targetValue = if (dismissDirection == SwipeToDismissBoxValue.EndToStart) Color(0xFFEF4444) else Color.Transparent,
        animationSpec = tween(300), label = "swipeBg"
    )
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).clip(RoundedCornerShape(20.dp)).background(color),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (dismissDirection == SwipeToDismissBoxValue.EndToStart) {
            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.White, modifier = Modifier.padding(end = 24.dp))
        }
    }
}

@Composable
fun CategoryChips(selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CATEGORIES.forEach { cat ->
            FilterChip(
                selected = selected == cat, onClick = { onSelect(cat) },
                label = { Text(cat, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = categoryColor(cat).copy(alpha = 0.2f),
                    selectedLabelColor = categoryColor(cat)
                )
            )
        }
    }
}


fun deadlineLabel(dueDate: String): Pair<String, Color>? {
    if (dueDate.isEmpty()) return null
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val due = sdf.parse(dueDate) ?: return null
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
        val diff = ((due.time - today.time) / (1000 * 60 * 60 * 24)).toInt()
        when {
            diff < 0  -> "Terlambat! 🔴" to Color(0xFFEF4444)
            diff == 0 -> "Hari ini! ⚠️" to Color(0xFFF59E0B)
            diff == 1 -> "Besok ⏰" to Color(0xFFF59E0B)
            diff <= 3 -> "$diff hari lagi" to Color(0xFFF59E0B)
            else      -> "$diff hari lagi" to Color(0xFF6B7280)
        }
    } catch (e: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showSwipe: Boolean = true
) {
    val cardColor by animateColorAsState(
        targetValue = if (task.isDone) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 400), label = "cardColor"
    )
    val deadline = deadlineLabel(task.dueDate)


    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            taskTitle = task.title,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    val content: @Composable () -> Unit = {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(if (task.isDone) Color(0xFF16A34A) else Color(0xFF4F46E5).copy(alpha = 0.1f))
                        .clickable { onToggleDone() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (task.isDone) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (task.isDone) Color.White else Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f).clickable { onEdit() }) {
                    Text(
                        task.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = if (task.isDone) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(categoryColor(task.category).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text(task.category, fontSize = 10.sp, color = categoryColor(task.category), fontWeight = FontWeight.Bold) }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(priorityColor(task.priority).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text(task.priority, fontSize = 10.sp, color = priorityColor(task.priority), fontWeight = FontWeight.Bold) }
                        if (!task.isDone && deadline != null) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(deadline.second.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) { Text(deadline.first, fontSize = 10.sp, color = deadline.second, fontWeight = FontWeight.Bold) }
                        } else if (task.dueDate.isNotEmpty()) {
                            Text("📅 ${task.dueDate}", fontSize = 11.sp, color = Color.Gray)
                        }
                        if (task.pomodoroCount > 0) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) { Text("🍅 ${task.pomodoroCount}", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444))
                }
            }
        }
    }

    if (showSwipe) {

        var showSwipeDeleteDialog by remember { mutableStateOf(false) }

        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {

                    showSwipeDeleteDialog = true

                    false
                } else false
            },
            positionalThreshold = { it * 0.4f }
        )

        if (showSwipeDeleteDialog) {
            DeleteConfirmDialog(
                taskTitle = task.title,
                onConfirm = {
                    showSwipeDeleteDialog = false
                    onDelete()
                },
                onDismiss = { showSwipeDeleteDialog = false }
            )
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300))
        ) {
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = true,
                backgroundContent = { SwipeDeleteBackground(dismissState.dismissDirection) }
            ) { content() }
        }
    } else {
        content()
    }
}

@Composable
fun DonutChart(progress: Float, size: Dp = 160.dp, strokeWidth: Dp = 18.dp, doneTask: Int, totalTask: Int) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress, animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val diameter = this.size.minDimension - strokePx
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(color = Color(0xFFE5E7EB), startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = strokePx, cap = StrokeCap.Round))
            if (animatedProgress.value > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF4F46E5))),
                    startAngle = -90f, sweepAngle = 360f * animatedProgress.value,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${(animatedProgress.value * 100).toInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
            Text(text = "$doneTask / $totalTask", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(text = "selesai", fontSize = 11.sp, color = Color.Gray.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun StatCardPremium(modifier: Modifier = Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(colors = listOf(color.copy(alpha = 0.12f), color.copy(alpha = 0.04f)))).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = color)
                Text(label, fontSize = 12.sp, color = color.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AnimatedStatRow(label: String, count: Int, total: Int, color: Color) {
    val fraction = if (total > 0) count.toFloat() / total.toFloat() else 0f
    val animatedFraction = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        animatedFraction.animateTo(fraction, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing))
    }
    val percent = (fraction * 100).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$count task", fontSize = 12.sp, color = Color.Gray)
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("$percent%", fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(modifier = Modifier.fillMaxWidth(animatedFraction.value).height(8.dp).clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(colors = listOf(color, color.copy(alpha = 0.6f)))))
        }
    }
}

@Composable
fun WeeklyBarChart(data: List<Pair<String, Int>>) {
    val maxVal = data.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val barColor = Color(0xFF4F46E5)
    val today = shortDayLabel(daysAgoString(0))
    Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        data.forEach { (day, count) ->
            val fraction = count.toFloat() / maxVal.toFloat()
            val animFraction = remember { Animatable(0f) }
            LaunchedEffect(fraction) { animFraction.animateTo(fraction, animationSpec = tween(700, easing = FastOutSlowInEasing)) }
            val isToday = day == today
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                Text(text = if (count > 0) "$count" else "", fontSize = 10.sp, color = if (isToday) Color(0xFF4F46E5) else Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(0.55f).height((80 * animFraction.value).coerceAtLeast(4f).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (isToday) Brush.verticalGradient(listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))) else Brush.verticalGradient(listOf(barColor.copy(alpha = 0.5f), barColor.copy(alpha = 0.25f))))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = day, fontSize = 11.sp, color = if (isToday) Color(0xFF4F46E5) else Color.Gray, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun StreakBadge(streak: Int) {
    val color = when { streak >= 7 -> Color(0xFFEF4444); streak >= 3 -> Color(0xFFF59E0B); streak > 0 -> Color(0xFF4F46E5); else -> Color.Gray }
    val emoji = when { streak >= 7 -> "🔥"; streak >= 3 -> "⚡"; streak > 0 -> "✨"; else -> "💤" }
    Row(
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(text = "$streak hari", fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

fun exportTasksToCSV(context: android.content.Context, taskList: List<Task>) {
    try {
        val sb = StringBuilder()
        sb.appendLine("No,Judul,Kategori,Prioritas,Due Date,Status,Tanggal Selesai,Pomodoro")
        taskList.forEachIndexed { index, task ->
            val status = if (task.isDone) "Selesai" else "Pending"
            sb.appendLine("${index + 1},\"${task.title}\",${task.category},${task.priority},${task.dueDate},$status,${task.completedDate},${task.pomodoroCount}")
        }
        val fileName = "StudyTask_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(sb.toString().toByteArray()) } }
        } else {
            val file = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            file.writeText(sb.toString())
        }
        Toast.makeText(context, "✅ Tersimpan di Downloads/$fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal export: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

enum class PomodoroPhase { FOCUS, BREAK }

@Composable
fun PomodoroScreen(taskList: List<Task>, dao: TaskDao, innerPadding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val focusDuration = 25 * 60
    val breakDuration = 5 * 60

    var phase by remember { mutableStateOf(PomodoroPhase.FOCUS) }
    var secondsLeft by remember { mutableIntStateOf(focusDuration) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var sessionCount by remember { mutableIntStateOf(0) }
    var showTaskPicker by remember { mutableStateOf(false) }

    val pendingTasks = taskList.filter { !it.isDone }

    LaunchedEffect(isRunning, phase) {
        if (isRunning) {
            while (secondsLeft > 0 && isRunning) {
                delay(1000L)
                secondsLeft--
            }
            if (secondsLeft == 0) {
                isRunning = false
                if (phase == PomodoroPhase.FOCUS) {
                    sessionCount++
                    selectedTask?.let { task ->
                        scope.launch(Dispatchers.IO) {
                            dao.updateTask(task.copy(pomodoroCount = task.pomodoroCount + 1))
                        }
                        selectedTask = task.copy(pomodoroCount = task.pomodoroCount + 1)
                    }
                    sendPomodoroNotification(context, "🍅 Sesi Fokus Selesai!", "Waktunya istirahat 5 menit. Kerja bagus!")
                    phase = PomodoroPhase.BREAK
                    secondsLeft = breakDuration
                } else {
                    sendPomodoroNotification(context, "⚡ Istirahat Selesai!", "Siap untuk sesi fokus berikutnya?")
                    phase = PomodoroPhase.FOCUS
                    secondsLeft = focusDuration
                }
            }
        }
    }

    val totalSeconds = if (phase == PomodoroPhase.FOCUS) focusDuration else breakDuration
    val timerFraction = secondsLeft.toFloat() / totalSeconds.toFloat()
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60

    val phaseColor = if (phase == PomodoroPhase.FOCUS) Color(0xFF4F46E5) else Color(0xFF16A34A)
    val phaseBg = if (phase == PomodoroPhase.FOCUS)
        listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
    else
        listOf(Color(0xFF16A34A), Color(0xFF15803D))

    if (showTaskPicker) {
        AlertDialog(
            onDismissRequest = { showTaskPicker = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Pilih Task", fontWeight = FontWeight.Bold) },
            text = {
                if (pendingTasks.isEmpty()) {
                    Text("Tidak ada task pending. Tambah task dulu di tab Home.", color = Color.Gray, fontSize = 14.sp)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pendingTasks) { task ->
                            val isSelected = selectedTask?.id == task.id
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedTask = task; showTaskPicker = false },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF4F46E5).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(categoryColor(task.category)))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(task.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text(task.category, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    if (task.pomodoroCount > 0) {
                                        Text("🍅 ${task.pomodoroCount}", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTaskPicker = false }) { Text("Tutup") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(colors = phaseBg))
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (phase == PomodoroPhase.FOCUS) "🍅 Sesi Fokus" else "☕ Istirahat",
                        fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                    Text(
                        text = if (phase == PomodoroPhase.FOCUS) "Fokus penuh selama 25 menit!" else "Santai dulu 5 menit",
                        fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f)
                    )
                    if (sessionCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🍅", fontSize = 13.sp)
                            Text("$sessionCount sesi selesai hari ini", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                        val animatedFrac = remember { Animatable(1f) }
                        LaunchedEffect(timerFraction) {
                            animatedFrac.animateTo(timerFraction, animationSpec = tween(800, easing = FastOutSlowInEasing))
                        }
                        Canvas(modifier = Modifier.size(200.dp)) {
                            val strokePx = 16.dp.toPx()
                            val diameter = this.size.minDimension - strokePx
                            val topLeft = Offset(strokePx / 2, strokePx / 2)
                            val arcSize = Size(diameter, diameter)
                            drawArc(color = Color(0xFFE5E7EB), startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = strokePx, cap = StrokeCap.Round))
                            if (animatedFrac.value > 0f) {
                                drawArc(
                                    brush = Brush.sweepGradient(colors = listOf(phaseColor, phaseColor.copy(alpha = 0.6f), phaseColor)),
                                    startAngle = -90f, sweepAngle = 360f * animatedFrac.value,
                                    useCenter = false, topLeft = topLeft, size = arcSize,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%02d:%02d".format(minutes, seconds),
                                fontSize = 44.sp, fontWeight = FontWeight.Bold, color = phaseColor
                            )
                            Text(
                                text = if (phase == PomodoroPhase.FOCUS) "FOKUS" else "ISTIRAHAT",
                                fontSize = 12.sp, color = phaseColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                isRunning = false
                                phase = PomodoroPhase.FOCUS
                                secondsLeft = focusDuration
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset")
                        }

                        Button(
                            onClick = { isRunning = !isRunning },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = phaseColor),
                            modifier = Modifier.height(52.dp).weight(1f)
                        ) {
                            Icon(
                                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null, modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isRunning) "Pause" else if (secondsLeft == totalSeconds) "Mulai" else "Lanjut",
                                fontSize = 16.sp, fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                isRunning = false
                                if (phase == PomodoroPhase.FOCUS) {
                                    phase = PomodoroPhase.BREAK
                                    secondsLeft = breakDuration
                                } else {
                                    phase = PomodoroPhase.FOCUS
                                    secondsLeft = focusDuration
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Skip")
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF4F46E5).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Assignment, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp)) }
                        Text("Task yang Dikerjakan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    if (selectedTask != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF4F46E5).copy(alpha = 0.06f))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(categoryColor(selectedTask!!.category)))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedTask!!.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(selectedTask!!.category, fontSize = 11.sp, color = Color.Gray)
                                        if (selectedTask!!.pomodoroCount > 0) {
                                            Text("• 🍅 ${selectedTask!!.pomodoroCount} sesi", fontSize = 11.sp, color = Color(0xFFEF4444))
                                        }
                                    }
                                }
                                IconButton(onClick = { selectedTask = null }) {
                                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    } else {
                        Text("Belum ada task dipilih", fontSize = 14.sp, color = Color.Gray)
                    }

                    OutlinedButton(
                        onClick = { showTaskPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedTask == null) "Pilih Task" else "Ganti Task")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.08f), Color(0xFFF59E0B).copy(alpha = 0.03f))))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💡", fontSize = 18.sp)
                            Text("Tips Pomodoro", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        listOf(
                            "🍅 Fokus 25 menit tanpa distraksi",
                            "☕ Istirahat 5 menit setelah tiap sesi",
                            "🔥 Setelah 4 sesi, istirahat 15-30 menit",
                            "📵 Matikan notifikasi saat sesi fokus"
                        ).forEach { tip ->
                            Text(tip, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun StatistikScreen(taskList: List<Task>, innerPadding: PaddingValues) {
    val context = LocalContext.current
    val totalTask = taskList.size
    val doneTask = taskList.count { it.isDone }
    val pendingTask = totalTask - doneTask
    val highTask = taskList.count { it.priority == "High" }
    val mediumTask = taskList.count { it.priority == "Medium" }
    val lowTask = taskList.count { it.priority == "Low" }
    val progress = if (totalTask > 0) doneTask.toFloat() / totalTask.toFloat() else 0f
    val streak = calculateStreak(taskList)
    val weeklyData = weeklyCompletedData(taskList)
    val totalPomodoro = taskList.sumOf { it.pomodoroCount }

    val motivasi = when {
        totalTask == 0 -> "Belum ada task. Yuk mulai! 🚀"
        progress == 1f -> "Luar biasa! Semua selesai! 🎉"
        progress >= 0.75f -> "Hampir selesai, semangat! 💪"
        progress >= 0.5f -> "Sudah lebih dari setengah! 👍"
        progress > 0f -> "Baru mulai, terus semangat! ⚡"
        else -> "Ayo kerjakan task pertamamu! 🔥"
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))))
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Statistik 📊", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Ringkasan semua task kamu", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Streak", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            StreakBadge(streak = streak)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(6.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Progress Keseluruhan", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
                    DonutChart(progress = progress, doneTask = doneTask, totalTask = totalTask)
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF4F46E5).copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(motivasi, fontSize = 13.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCardPremium(modifier = Modifier.weight(1f), label = "Total", value = totalTask.toString(), icon = Icons.Default.FormatListBulleted, color = Color(0xFF4F46E5))
                StatCardPremium(modifier = Modifier.weight(1f), label = "Selesai", value = doneTask.toString(), icon = Icons.Default.CheckCircle, color = Color(0xFF16A34A))
                StatCardPremium(modifier = Modifier.weight(1f), label = "🍅 Sesi", value = totalPomodoro.toString(), icon = Icons.Default.Timer, color = Color(0xFFEF4444))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(6.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF4F46E5).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.BarChart, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                            }
                            Text("Aktivitas 7 Hari Terakhir", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        val weekTotal = weeklyData.sumOf { it.second }
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF4F46E5).copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("$weekTotal task", fontSize = 12.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold)
                        }
                    }
                    WeeklyBarChart(data = weeklyData)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(6.dp)) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFF7C3AED).copy(alpha = 0.08f), Color(0xFF4F46E5).copy(alpha = 0.04f)))).padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🔥 Streak Harian", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = when {
                                    streak == 0 -> "Selesaikan task hari ini untuk mulai streak!"
                                    streak == 1 -> "Bagus! Pertahankan streak-mu!"
                                    streak < 7 -> "Keren! $streak hari berturut-turut!"
                                    else -> "Luar biasa! $streak hari streak! 🏆"
                                },
                                fontSize = 13.sp, color = Color.Gray
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$streak", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                                color = when { streak >= 7 -> Color(0xFFEF4444); streak >= 3 -> Color(0xFFF59E0B); streak > 0 -> Color(0xFF4F46E5); else -> Color.Gray.copy(alpha = 0.4f) }
                            )
                            Text("hari", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(6.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Flag, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                        Text("Berdasarkan Prioritas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    AnimatedStatRow("High", highTask, totalTask, Color(0xFFEF4444))
                    AnimatedStatRow("Medium", mediumTask, totalTask, Color(0xFFF59E0B))
                    AnimatedStatRow("Low", lowTask, totalTask, Color(0xFF22C55E))
                }
            }
        }

        item {
            val usedCategories = CATEGORIES.filter { cat -> taskList.any { it.category == cat } }
            if (usedCategories.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(6.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF7C3AED).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Category, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                            }
                            Text("Berdasarkan Kategori", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        usedCategories.forEach { cat ->
                            val count = taskList.count { it.category == cat }
                            AnimatedStatRow(cat, count, totalTask, categoryColor(cat))
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(6.dp)) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFF16A34A).copy(alpha = 0.08f), Color(0xFF16A34A).copy(alpha = 0.03f)))).padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                            Text("📤 Export Data", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Simpan semua task ke file CSV di Downloads", fontSize = 13.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { exportTasksToCSV(context, taskList) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            enabled = taskList.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(dao: TaskDao, isDarkMode: Boolean, onToggleDarkMode: () -> Unit) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val username by userPrefs.username.collectAsStateWithLifecycle(initialValue = "")
    val scope = rememberCoroutineScope()

    val taskList by dao.getAllTasks().collectAsStateWithLifecycle(initialValue = emptyList())
    val totalTask = taskList.size
    val doneTask = taskList.count { it.isDone }
    val progress = if (totalTask > 0) doneTask.toFloat() / totalTask.toFloat() else 0f
    val streak = calculateStreak(taskList)

    val allDone = totalTask > 0 && doneTask == totalTask
    var showKonfetti by remember { mutableStateOf(false) }
    var lastAllDone by remember { mutableStateOf(false) }
    LaunchedEffect(allDone) {
        if (allDone && !lastAllDone) showKonfetti = true
        lastAllDone = allDone
    }

    var taskText by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf("Medium") }
    var newDueDate by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Umum") }

    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var editText by remember { mutableStateOf("") }
    var editPriority by remember { mutableStateOf("Medium") }
    var editDueDate by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Umum") }

    var showAddDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    var searchQuery by remember { mutableStateOf("") }
    var filterPriority by remember { mutableStateOf("Semua") }
    var filterCategory by remember { mutableStateOf("Semua") }
    var showFilterPanel by remember { mutableStateOf(false) }

    val filteredList = taskList.filter { task ->
        val matchSearch = task.title.contains(searchQuery, ignoreCase = true)
        val matchPriority = filterPriority == "Semua" || task.priority == filterPriority
        val matchCategory = filterCategory == "Semua" || task.category == filterCategory
        matchSearch && matchPriority && matchCategory
    }

    if (username.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Halo! Siapa namamu?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Masukkan namamu untuk mulai", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(
                    value = usernameInput, onValueChange = { usernameInput = it },
                    label = { Text("Nama kamu", color = Color.White.copy(alpha = 0.8f)) },
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { if (usernameInput.isNotBlank()) scope.launch { userPrefs.saveUsername(usernameInput.trim()) } },
                    modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) { Text("Mulai", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
        return
    }

    if (showUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false }, shape = RoundedCornerShape(20.dp),
            title = { Text("Ubah Nama", fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = usernameInput, onValueChange = { usernameInput = it }, label = { Text("Nama baru") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = { if (usernameInput.isNotBlank()) { scope.launch { userPrefs.saveUsername(usernameInput.trim()) }; showUsernameDialog = false } },
                    shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showUsernameDialog = false }) { Text("Batal") } }
        )
    }

    if (taskToEdit != null) {
        AlertDialog(
            onDismissRequest = { taskToEdit = null }, shape = RoundedCornerShape(20.dp),
            title = { Text("Edit Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = editText, onValueChange = { editText = it }, label = { Text("Nama task") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    DatePickerButton(selectedDate = editDueDate, onDateSelected = { editDueDate = it })
                    Text("Kategori:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    CategoryChips(selected = editCategory, onSelect = { editCategory = it })
                    Text("Prioritas:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("High", "Medium", "Low").forEach { p ->
                            FilterChip(selected = editPriority == p, onClick = { editPriority = p }, label = { Text(p, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = priorityColor(p).copy(alpha = 0.2f), selectedLabelColor = priorityColor(p)))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editText.isNotBlank()) {
                        val updated = taskToEdit!!.copy(title = editText.trim(), priority = editPriority, dueDate = editDueDate, category = editCategory)
                        scope.launch(Dispatchers.IO) { dao.updateTask(updated) }
                        taskToEdit = null
                    }
                }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { taskToEdit = null }) { Text("Batal") } }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false }, shape = RoundedCornerShape(20.dp),
            title = { Text("Tambah Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = taskText, onValueChange = { taskText = it }, label = { Text("Nama task baru") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    DatePickerButton(selectedDate = newDueDate, onDateSelected = { newDueDate = it })
                    Text("Kategori:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    CategoryChips(selected = newCategory, onSelect = { newCategory = it })
                    Text("Prioritas:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("High", "Medium", "Low").forEach { p ->
                            FilterChip(selected = newPriority == p, onClick = { newPriority = p }, label = { Text(p, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = priorityColor(p).copy(alpha = 0.2f), selectedLabelColor = priorityColor(p)))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (taskText.isNotBlank()) {
                        val t = taskText.trim(); val d = newDueDate; val p = newPriority; val c = newCategory
                        taskText = ""; newDueDate = ""; newPriority = "Medium"; newCategory = "Umum"
                        scope.launch(Dispatchers.IO) { dao.insertTask(Task(title = t, priority = p, dueDate = d, category = c)) }
                        showAddDialog = false
                    }
                }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))) { Text("Tambah") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; taskText = ""; newDueDate = ""; newPriority = "Medium"; newCategory = "Umum" }) { Text("Batal") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = Color(0xFF4F46E5), shape = CircleShape,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) { Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White) }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Selesai") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Timer, null) }, label = { Text("Fokus") })
                    NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("Statistik") })
                }
            }
        ) { innerPadding ->
            when (selectedTab) {
                0 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .background(Brush.verticalGradient(colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))))
                                    .padding(horizontal = 24.dp, vertical = 32.dp)
                            ) {
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("Hi, $username! 👋", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Ayo selesaikan tugasmu", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (streak > 0) {
                                                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 5.dp)) {
                                                    Text("🔥 $streak", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable { onToggleDarkMode() }, contentAlignment = Alignment.Center) {
                                                Icon(imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Toggle dark mode", tint = Color.White, modifier = Modifier.size(22.dp))
                                            }
                                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable { usernameInput = username; showUsernameDialog = true }, contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.15f)).padding(16.dp)) {
                                        Column {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Progress Hari Ini", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("$doneTask / $totalTask task", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.25f))) {
                                                Box(modifier = Modifier.fillMaxWidth(progress).height(12.dp).clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(colors = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)))))
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (totalTask == 0) "Belum ada task" else if (doneTask == totalTask) "Semua task selesai! 🎉" else "${(progress * 100).toInt()}% selesai",
                                                color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = searchQuery, onValueChange = { searchQuery = it },
                                        placeholder = { Text("Cari task...") },
                                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray) } }
                                                IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                                                    Icon(Icons.Default.FilterList, null, tint = if (filterPriority != "Semua" || filterCategory != "Semua") Color(0xFF4F46E5) else Color.Gray)
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent)
                                    )
                                }
                                if (showFilterPanel) {
                                    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("Filter Prioritas:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf("Semua", "High", "Medium", "Low").forEach { p ->
                                                    FilterChip(selected = filterPriority == p, onClick = { filterPriority = p }, label = { Text(p, fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (p == "Semua") Color(0xFF4F46E5).copy(alpha = 0.2f) else priorityColor(p).copy(alpha = 0.2f), selectedLabelColor = if (p == "Semua") Color(0xFF4F46E5) else priorityColor(p)))
                                                }
                                            }
                                            Text("Filter Kategori:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf("Semua", *CATEGORIES.toTypedArray()).forEach { cat ->
                                                    FilterChip(selected = filterCategory == cat, onClick = { filterCategory = cat }, label = { Text(cat, fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = categoryColor(if (cat == "Semua") "Umum" else cat).copy(alpha = 0.2f), selectedLabelColor = categoryColor(if (cat == "Semua") "Umum" else cat)))
                                                }
                                            }
                                            if (filterPriority != "Semua" || filterCategory != "Semua") {
                                                TextButton(onClick = { filterPriority = "Semua"; filterCategory = "Semua" }) {
                                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Reset filter", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Daftar Task", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("${filteredList.size} task", fontSize = 13.sp, color = Color.Gray)
                            }
                        }

                        if (taskList.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("📝", fontSize = 56.sp)
                                        Text("Belum ada task", fontSize = 18.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("Yuk tambah task pertamamu!", fontSize = 14.sp, color = Color.Gray.copy(alpha = 0.7f))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(onClick = { showAddDialog = true }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))) {
                                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Tambah Task")
                                        }
                                    }
                                }
                            }
                        } else if (filteredList.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.SearchOff, null, tint = Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(60.dp))
                                        Text("Tidak ada task ditemukan", fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                        Text("Coba ubah kata kunci atau filter", fontSize = 13.sp, color = Color.Gray.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        } else {
                            items(filteredList, key = { it.id }) { task ->
                                TaskCard(
                                    task = task,
                                    onToggleDone = {
                                        scope.launch(Dispatchers.IO) {
                                            val newDone = !task.isDone
                                            val completedDate = if (newDone) todayString() else ""
                                            dao.updateTask(task.copy(isDone = newDone, completedDate = completedDate))
                                        }
                                    },
                                    onEdit = { taskToEdit = task; editText = task.title; editPriority = task.priority; editDueDate = task.dueDate; editCategory = task.category },
                                    onDelete = { scope.launch(Dispatchers.IO) { dao.deleteTask(task) } }
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }

                1 -> {
                    val doneTasks = taskList.filter { it.isDone }
                    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(colors = listOf(Color(0xFF16A34A), Color(0xFF15803D)))).padding(horizontal = 24.dp, vertical = 32.dp)) {
                            Column {
                                Text("Task Selesai ✅", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${doneTasks.size} task berhasil diselesaikan", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                        if (doneTasks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("✅", fontSize = 56.sp)
                                    Text("Belum ada task selesai", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                    Text("Selesaikan task di halaman Home", fontSize = 13.sp, color = Color.Gray.copy(alpha = 0.7f))
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(doneTasks, key = { it.id }) { task ->
                                    TaskCard(task = task, onToggleDone = {}, onEdit = {}, onDelete = {}, showSwipe = false)
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }
                    }
                }

                2 -> PomodoroScreen(taskList = taskList, dao = dao, innerPadding = innerPadding)
                3 -> StatistikScreen(taskList = taskList, innerPadding = innerPadding)
            }
        }

        if (showKonfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(), parties = konfettiParty(),
                updateListener = object : OnParticleSystemUpdateListener {
                    override fun onParticleSystemEnded(system: nl.dionsegijn.konfetti.core.PartySystem, activeSystems: Int) {
                        if (activeSystems == 0) showKonfetti = false
                    }
                }
            )
        }
    }
}