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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = TaskDatabase.getDatabase(this)
        val dao = db.taskDao()

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }

            StudyTaskTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodoScreen(
                        dao = dao,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { isDarkMode = !isDarkMode }
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

fun formatDateMillis(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerButton(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (selectedDate.isEmpty()) "Pilih due date" else selectedDate,
            fontSize = 14.sp
        )
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(formatDateMillis(it))
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TodoScreen(
    dao: TaskDao,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val username by userPrefs.username.collectAsStateWithLifecycle(initialValue = "")
    val scope = rememberCoroutineScope()

    var taskText by remember { mutableStateOf("") }
    val taskList by dao.getAllTasks().collectAsStateWithLifecycle(initialValue = emptyList())

    val totalTask = taskList.size
    val doneTask = taskList.count { it.isDone }
    val progress = if (totalTask > 0) doneTask.toFloat() / totalTask.toFloat() else 0f

    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var editText by remember { mutableStateOf("") }
    var editPriority by remember { mutableStateOf("Medium") }
    var editDueDate by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    var newPriority by remember { mutableStateOf("Medium") }
    var newDueDate by remember { mutableStateOf("") }

    // LAYAR SETUP NAMA
    if (username.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Halo! Siapa namamu?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Masukkan namamu untuk mulai", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Nama kamu", color = Color.White.copy(alpha = 0.8f)) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (usernameInput.isNotBlank()) {
                            scope.launch { userPrefs.saveUsername(usernameInput.trim()) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Mulai", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        return
    }

    // DIALOG EDIT NAMA
    if (showUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Ubah Nama", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Nama baru") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (usernameInput.isNotBlank()) {
                            scope.launch { userPrefs.saveUsername(usernameInput.trim()) }
                            showUsernameDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameDialog = false }) { Text("Batal") }
            }
        )
    }

    // DIALOG EDIT TASK
    if (taskToEdit != null) {
        AlertDialog(
            onDismissRequest = { taskToEdit = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Edit Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("Nama task") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    DatePickerButton(
                        selectedDate = editDueDate,
                        onDateSelected = { editDueDate = it }
                    )
                    Text("Prioritas:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("High", "Medium", "Low").forEach { p ->
                            FilterChip(
                                selected = editPriority == p,
                                onClick = { editPriority = p },
                                label = { Text(p, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = priorityColor(p).copy(alpha = 0.2f),
                                    selectedLabelColor = priorityColor(p)
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editText.isNotBlank()) {
                            val updated = taskToEdit!!.copy(
                                title = editText.trim(),
                                priority = editPriority,
                                dueDate = editDueDate
                            )
                            scope.launch(Dispatchers.IO) { dao.updateTask(updated) }
                            taskToEdit = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { taskToEdit = null }) { Text("Batal") }
            }
        )
    }

    // DIALOG TAMBAH TASK
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Tambah Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = taskText,
                        onValueChange = { taskText = it },
                        label = { Text("Nama task baru") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    DatePickerButton(
                        selectedDate = newDueDate,
                        onDateSelected = { newDueDate = it }
                    )
                    Text("Prioritas:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("High", "Medium", "Low").forEach { p ->
                            FilterChip(
                                selected = newPriority == p,
                                onClick = { newPriority = p },
                                label = { Text(p, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = priorityColor(p).copy(alpha = 0.2f),
                                    selectedLabelColor = priorityColor(p)
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskText.isNotBlank()) {
                            val titleToSave = taskText.trim()
                            val dueDateToSave = newDueDate
                            val priorityToSave = newPriority
                            taskText = ""
                            newDueDate = ""
                            newPriority = "Medium"
                            scope.launch(Dispatchers.IO) {
                                dao.insertTask(Task(title = titleToSave, priority = priorityToSave, dueDate = dueDateToSave))
                            }
                            showAddDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    taskText = ""
                    newDueDate = ""
                    newPriority = "Medium"
                }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF4F46E5),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White)
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text("Selesai") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onToggleDarkMode,
                    icon = {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = null
                        )
                    },
                    label = { Text(if (isDarkMode) "Light" else "Dark") }
                )
            }
        }
    ) { innerPadding ->

        when (selectedTab) {

            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))))
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Hi, $username! 👋", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Ayo selesaikan tugasmu", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp).clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f))
                                            .clickable { usernameInput = username; showUsernameDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth().clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.15f)).padding(16.dp)
                                ) {
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
                                            text = if (totalTask == 0) "Belum ada task"
                                            else if (doneTask == totalTask) "Semua task selesai! 🎉"
                                            else "${(progress * 100).toInt()}% selesai",
                                            color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Daftar Task", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("$totalTask task", fontSize = 13.sp, color = Color.Gray)
                        }
                    }

                    items(taskList, key = { it.id }) { task ->
                        val cardColor by animateColorAsState(
                            targetValue = if (task.isDone) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.surface,
                            animationSpec = tween(durationMillis = 400),
                            label = "cardColor"
                        )
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300))
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = cardColor)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp).clip(CircleShape)
                                            .background(if (task.isDone) Color(0xFF16A34A) else Color(0xFF4F46E5).copy(alpha = 0.1f))
                                            .clickable { scope.launch(Dispatchers.IO) { dao.updateTask(task.copy(isDone = !task.isDone)) } },
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
                                    Column(
                                        modifier = Modifier.weight(1f).clickable {
                                            taskToEdit = task
                                            editText = task.title
                                            editPriority = task.priority
                                            editDueDate = task.dueDate
                                        }
                                    ) {
                                        Text(
                                            task.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                            color = if (task.isDone) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
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
                                    IconButton(onClick = { scope.launch(Dispatchers.IO) { dao.deleteTask(task) } }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            1 -> {
                val doneTasks = taskList.filter { it.isDone }
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(colors = listOf(Color(0xFF16A34A), Color(0xFF15803D))))
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                    ) {
                        Column {
                            Text("Task Selesai ✅", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${doneTasks.size} task berhasil diselesaikan", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    if (doneTasks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(80.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Belum ada task selesai", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                Text("Selesaikan task di halaman Home", fontSize = 13.sp, color = Color.Gray.copy(alpha = 0.7f))
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(doneTasks, key = { it.id }) { task ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF16A34A)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(task.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A))
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(priorityColor(task.priority).copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(task.priority, fontSize = 10.sp, color = priorityColor(task.priority), fontWeight = FontWeight.Bold)
                                                }
                                                if (task.dueDate.isNotEmpty()) {
                                                    Text("📅 ${task.dueDate}", fontSize = 11.sp, color = Color(0xFF16A34A).copy(alpha = 0.7f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}