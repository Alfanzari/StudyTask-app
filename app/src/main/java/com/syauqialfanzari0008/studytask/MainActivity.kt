package com.syauqialfanzari0008.studytask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = TaskDatabase.getDatabase(this)
        val dao = db.taskDao()

        setContent {
            val context = applicationContext
            val userPrefs = remember { UserPreferences(context) }
            val isDarkMode by userPrefs.darkMode.collectAsStateWithLifecycle(initialValue = false)
            val scope = rememberCoroutineScope()

            StudyTaskTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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

fun priorityColor(priority: String): Color {
    return when (priority) {
        "High" -> Color(0xFFEF4444)
        "Medium" -> Color(0xFFF59E0B)
        else -> Color(0xFF22C55E)
    }
}

fun categoryColor(category: String): Color {
    return when (category) {
        "Kuliah" -> Color(0xFF4F46E5)
        "Kerja" -> Color(0xFF0891B2)
        "Personal" -> Color(0xFFDB2777)
        "Belanja" -> Color(0xFFD97706)
        "Kesehatan" -> Color(0xFF16A34A)
        else -> Color(0xFF6B7280)
    }
}

fun formatDateMillis(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerButton(selectedDate: String, onDateSelected: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
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
        position = Position.Relative(0.5, 0.0),
        spread = 360,
        colors = listOf(0xFF4F46E5.toInt(), 0xFF7C3AED.toInt(), 0xFF16A34A.toInt(), 0xFFF59E0B.toInt(), 0xFFEF4444.toInt(), 0xFF06B6D4.toInt())
    )
)

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

// ===== PILIH KATEGORI CHIPS =====
@Composable
fun CategoryChips(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CATEGORIES.forEach { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(cat, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = categoryColor(cat).copy(alpha = 0.2f),
                    selectedLabelColor = categoryColor(cat)
                )
            )
        }
    }
}

// ===== TASK CARD (reusable) =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showSwipe: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val cardColor by animateColorAsState(
        targetValue = if (task.isDone) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 400), label = "cardColor"
    )

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
                        // Badge kategori
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(categoryColor(task.category).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(task.category, fontSize = 10.sp, color = categoryColor(task.category), fontWeight = FontWeight.Bold)
                        }
                        // Badge prioritas
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(priorityColor(task.priority).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(task.priority, fontSize = 10.sp, color = priorityColor(task.priority), fontWeight = FontWeight.Bold)
                        }
                        if (task.dueDate.isNotEmpty()) {
                            Text("📅 ${task.dueDate}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
                IconButton(onClick = { onDelete() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444))
                }
            }
        }
    }

    if (showSwipe) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
            },
            positionalThreshold = { it * 0.4f }
        )
        AnimatedVisibility(visible = true, enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300))) {
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

// ===== STATISTIK =====
@Composable
fun StatistikScreen(taskList: List<Task>, innerPadding: PaddingValues) {
    val totalTask = taskList.size
    val doneTask = taskList.count { it.isDone }
    val pendingTask = totalTask - doneTask
    val highTask = taskList.count { it.priority == "High" }
    val mediumTask = taskList.count { it.priority == "Medium" }
    val lowTask = taskList.count { it.priority == "Low" }
    val progress = if (totalTask > 0) doneTask.toFloat() / totalTask.toFloat() else 0f

    LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))))
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Column {
                    Text("Statistik 📊", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Ringkasan semua task kamu", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Progress Keseluruhan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.fillMaxWidth(progress).height(12.dp).clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)))))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${(progress * 100).toInt()}% selesai", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Total", value = totalTask.toString(), color = Color(0xFF4F46E5))
                StatCard(modifier = Modifier.weight(1f), label = "Selesai", value = doneTask.toString(), color = Color(0xFF16A34A))
                StatCard(modifier = Modifier.weight(1f), label = "Pending", value = pendingTask.toString(), color = Color(0xFFF59E0B))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Berdasarkan Prioritas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    PriorityStatRow("High", highTask, totalTask, Color(0xFFEF4444))
                    PriorityStatRow("Medium", mediumTask, totalTask, Color(0xFFF59E0B))
                    PriorityStatRow("Low", lowTask, totalTask, Color(0xFF22C55E))
                }
            }
        }
        item {
            // STATISTIK PER KATEGORI
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Berdasarkan Kategori", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    CATEGORIES.forEach { cat ->
                        val count = taskList.count { it.category == cat }
                        if (count > 0) PriorityStatRow(cat, count, totalTask, categoryColor(cat))
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = color.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PriorityStatRow(label: String, count: Int, total: Int, color: Color) {
    val fraction = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Text("$count task", fontSize = 13.sp, color = Color.Gray)
        }
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(modifier = Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(50)).background(color))
        }
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

    val allDone = totalTask > 0 && doneTask == totalTask
    var showKonfetti by remember { mutableStateOf(false) }
    var lastAllDone by remember { mutableStateOf(false) }
    LaunchedEffect(allDone) {
        if (allDone && !lastAllDone) showKonfetti = true
        lastAllDone = allDone
    }

    // STATE FORM
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

    // SEARCH & FILTER STATE
    var searchQuery by remember { mutableStateOf("") }
    var filterPriority by remember { mutableStateOf("Semua") }
    var filterCategory by remember { mutableStateOf("Semua") }
    var showFilterPanel by remember { mutableStateOf(false) }

    // FILTERED LIST
    val filteredList = taskList.filter { task ->
        val matchSearch = task.title.contains(searchQuery, ignoreCase = true)
        val matchPriority = filterPriority == "Semua" || task.priority == filterPriority
        val matchCategory = filterCategory == "Semua" || task.category == filterCategory
        matchSearch && matchPriority && matchCategory
    }

    // LAYAR SETUP NAMA
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

    // DIALOG EDIT NAMA
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

    // DIALOG EDIT TASK
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

    // DIALOG TAMBAH TASK
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
                    FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Color(0xFF4F46E5), shape = CircleShape) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White)
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Selesai") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("Statistik") })
                    NavigationBarItem(selected = false, onClick = onToggleDarkMode, icon = { Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, null) }, label = { Text(if (isDarkMode) "Light" else "Dark") })
                }
            }
        ) { innerPadding ->
            when (selectedTab) {
                0 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // HEADER
                        item {
                            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)))).padding(horizontal = 24.dp, vertical = 32.dp)) {
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("Hi, $username! 👋", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Ayo selesaikan tugasmu", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                                        }
                                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable { usernameInput = username; showUsernameDialog = true }, contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.15f)).padding(16.dp)) {
                                        Column {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Progress Hari Ini", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("$doneTask / $totalTask task", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.3f))) {
                                                Box(modifier = Modifier.fillMaxWidth(progress).height(8.dp).clip(RoundedCornerShape(50)).background(Color.White))
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

                        // ===== SEARCH BAR =====
                        item {
                            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Cari task...") },
                                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                                            }
                                            IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                                                Icon(
                                                    Icons.Default.FilterList, null,
                                                    tint = if (filterPriority != "Semua" || filterCategory != "Semua") Color(0xFF4F46E5) else Color.Gray
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                // PANEL FILTER (muncul saat ikon filter diklik)
                                if (showFilterPanel) {
                                    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("Filter Prioritas:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf("Semua", "High", "Medium", "Low").forEach { p ->
                                                    FilterChip(
                                                        selected = filterPriority == p, onClick = { filterPriority = p },
                                                        label = { Text(p, fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = if (p == "Semua") Color(0xFF4F46E5).copy(alpha = 0.2f) else priorityColor(p).copy(alpha = 0.2f),
                                                            selectedLabelColor = if (p == "Semua") Color(0xFF4F46E5) else priorityColor(p)
                                                        )
                                                    )
                                                }
                                            }
                                            Text("Filter Kategori:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf("Semua", *CATEGORIES.toTypedArray()).forEach { cat ->
                                                    FilterChip(
                                                        selected = filterCategory == cat, onClick = { filterCategory = cat },
                                                        label = { Text(cat, fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = categoryColor(if (cat == "Semua") "Umum" else cat).copy(alpha = 0.2f),
                                                            selectedLabelColor = categoryColor(if (cat == "Semua") "Umum" else cat)
                                                        )
                                                    )
                                                }
                                            }
                                            if (filterPriority != "Semua" || filterCategory != "Semua") {
                                                TextButton(onClick = { filterPriority = "Semua"; filterCategory = "Semua" }) {
                                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Reset filter", fontSize = 12.sp)
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

                        if (filteredList.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.SearchOff, null, tint = Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(60.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Tidak ada task ditemukan", fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            items(filteredList, key = { it.id }) { task ->
                                TaskCard(
                                    task = task,
                                    onToggleDone = { scope.launch(Dispatchers.IO) { dao.updateTask(task.copy(isDone = !task.isDone)) } },
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(80.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
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

                2 -> StatistikScreen(taskList = taskList, innerPadding = innerPadding)
            }
        }

        if (showKonfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = konfettiParty(),
                updateListener = object : OnParticleSystemUpdateListener {
                    override fun onParticleSystemEnded(system: nl.dionsegijn.konfetti.core.PartySystem, activeSystems: Int) {
                        if (activeSystems == 0) showKonfetti = false
                    }
                }
            )
        }
    }
}