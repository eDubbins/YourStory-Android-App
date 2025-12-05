package com.example.yourstory

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.yourstory.ui.theme.YourStoryTheme
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YourStoryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // --- 1. TOOLS & DATA ---
    val db = remember { (context.applicationContext as StoryApplication).database }
    val taskDao = remember { db.taskDao() }
    val storyDao = remember { db.storyDao() }

    val storyGen = remember { StoryGenerator() }
    val imageGen = remember { ImageGenerator() }
    val imageStorage = remember { ImageStorage(context) }
    val calendarRepo = remember { CalendarRepository(context) }

    val scope = rememberCoroutineScope()

    // --- 2. STATE ---
    val tasks by taskDao.getAllTasks().collectAsState(initial = emptyList())

    // Filter Lists
    val openTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }

    // UI State
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showCompleted by rememberSaveable { mutableStateOf(true) } // Toggle Visibility

    // Editing State
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    // Generation State
    var comicPanels by remember { mutableStateOf<List<ComicPanel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // --- 3. CALENDAR SYNC LOGIC ---
    fun syncCalendar() {
        scope.launch {
            val events = calendarRepo.getTodaysEvents()
            var addedCount = 0
            events.forEach { eventTitle ->
                // Check DB for duplicates before adding
                val exists = taskDao.countTasksWithTitle(eventTitle) > 0
                if (!exists) {
                    taskDao.insert(Task(title = eventTitle, type = TaskType.WORK))
                    addedCount++
                }
            }
            if (addedCount > 0) println("APP_DEBUG: Synced $addedCount events.")
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) syncCalendar()
        }
    )

    // Auto-sync on startup if permission exists
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            == PackageManager.PERMISSION_GRANTED) {
            syncCalendar()
        } else {
            // Optional: launcher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    // --- 4. ROUTING ---
    if (showHistory) {
        DiaryScreen(storyDao = storyDao, onBack = { showHistory = false })
        return
    }

    // --- 5. MAIN UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Tasks") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    // TOGGLE COMPLETED VISIBILITY
                    if (completedTasks.isNotEmpty()) {
                        IconButton(onClick = { showCompleted = !showCompleted }) {
                            Icon(
                                imageVector = if (showCompleted) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Completed"
                            )
                        }
                    }

                    // SYNC CALENDAR
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                            == PackageManager.PERMISSION_GRANTED) {
                            syncCalendar()
                        } else {
                            launcher.launch(Manifest.permission.READ_CALENDAR)
                        }
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Sync Calendar")
                    }

                    // DIARY / HISTORY
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Diary")
                    }

                    // GENERATE STORY
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                // 1. Generate Text Script (Filters for Today internally)
                                val panels = storyGen.generateComicPanels(tasks)
                                val finishedPanels = mutableListOf<ComicPanel>()

                                // 2. Generate Images Sequentially
                                for (panel in panels) {
                                    val url = imageGen.generateComicPanel(panel.visualDescription)
                                    // Save Base64/Url to Disk
                                    val path = if (url != null) {
                                        if (url.startsWith("data:")) {
                                            // It's Base64 (Gemini/Imagen) -> Save to File
                                            imageStorage.saveImage(url, "comic_${System.currentTimeMillis()}")
                                        } else {
                                            // It's a URL (Pollinations) -> Keep as URL (or download if you want)
                                            url
                                        }
                                    } else null
                                    finishedPanels.add(panel.copy(imageUrl = path))
                                }

                                comicPanels = finishedPanels

                                // 3. Save to Database
                                if (finishedPanels.size == 4) {
                                    // Save titles of completed tasks for the record
                                    val doneTitles = completedTasks.map { it.title }
                                    val story = Story(
                                        completedTasks = doneTitles,
                                        panel1Path = finishedPanels[0].imageUrl, panel1Caption = finishedPanels[0].caption,
                                        panel2Path = finishedPanels[1].imageUrl, panel2Caption = finishedPanels[1].caption,
                                        panel3Path = finishedPanels[2].imageUrl, panel3Caption = finishedPanels[2].caption,
                                        panel4Path = finishedPanels[3].imageUrl, panel4Caption = finishedPanels[3].caption
                                    )
                                    storyDao.insert(story)
                                }
                                isLoading = false
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Icon(Icons.Default.AutoAwesome, "Generate")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { innerPadding ->

        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // OPEN TASKS
            items(openTasks, key = { it.id }) { task ->
                SwipeToDeleteContainer(onDelete = { scope.launch { taskDao.delete(task) } }) {
                    TaskRow(
                        task = task,
                        onCheckedChange = { isChecked ->
                            scope.launch { taskDao.update(task.copy(isCompleted = isChecked, completedAt = if(isChecked) LocalDateTime.now().toString() else null)) }
                        },
                        onEditClick = { taskToEdit = task }
                    )
                }
            }

            // COMPLETED TASKS (With Divider)
            if (showCompleted && completedTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text("Completed", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp), color = Color.Gray)
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                }
                items(completedTasks, key = { it.id }) { task ->
                    SwipeToDeleteContainer(onDelete = { scope.launch { taskDao.delete(task) } }) {
                        TaskRow(
                            task = task,
                            onCheckedChange = { isChecked ->
                                scope.launch { taskDao.update(task.copy(isCompleted = isChecked, completedAt = null)) }
                            },
                            onEditClick = { taskToEdit = task }
                        )
                    }
                }
            }
        }

        // DIALOGS
        if (showAddDialog) {
            TaskDialog(
                taskToEdit = null,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, desc, type ->
                    scope.launch { taskDao.insert(Task(title = title, description = desc, type = type)); showAddDialog = false }
                }
            )
        }

        if (taskToEdit != null) {
            TaskDialog(
                taskToEdit = taskToEdit,
                onDismiss = { taskToEdit = null },
                onConfirm = { title, desc, type ->
                    scope.launch {
                        taskDao.update(taskToEdit!!.copy(title = title, description = desc, type = type))
                        taskToEdit = null
                    }
                }
            )
        }

        if (comicPanels.isNotEmpty()) {
            ComicStripDialog(panels = comicPanels, onDismiss = { comicPanels = emptyList() })
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun TaskRow(task: Task, onCheckedChange: (Boolean) -> Unit, onEditClick: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = onCheckedChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) Color.Gray else Color.Unspecified
                )
                if (task.description.isNotBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.Gray)
                }
                Text(task.type.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun TaskDialog(taskToEdit: Task? = null, onDismiss: () -> Unit, onConfirm: (String, String, TaskType) -> Unit) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var selectedType by remember { mutableStateOf(taskToEdit?.type ?: TaskType.NORMAL) }

    val dialogTitle = if (taskToEdit == null) "New Quest" else "Edit Quest"
    val buttonText = if (taskToEdit == null) "Add" else "Save"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("What needs doing?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Details (Optional)") }, minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                // Type Selector
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskType.values().forEach { type ->
                        val isSelected = (type == selectedType)
                        Button(
                            onClick = { selectedType = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) { Text(type.name, style = MaterialTheme.typography.labelSmall) }
                    }
                }

                // Completion Date (Read Only)
                if (taskToEdit?.completedAt != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val dateStr = try {
                        val parsed = LocalDateTime.parse(taskToEdit.completedAt)
                        parsed.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a"))
                    } catch (e: Exception) { "Unknown" }
                    OutlinedTextField(value = dateStr, onValueChange = {}, readOnly = true, enabled = false, label = { Text("Completed") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { Icon(Icons.Default.DateRange, null) })
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onConfirm(title, description, selectedType) }) { Text(buttonText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ComicStripDialog(panels: List<ComicPanel>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(panels) { panel ->
                    Card(elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val imageModel = if (panel.imageUrl != null) {
                                val file = File(panel.imageUrl!!)
                                if (file.exists()) file else panel.imageUrl
                            } else null

                            if (imageModel != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(imageModel).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(Modifier.fillMaxWidth().height(120.dp)) { Text("...", Modifier.align(Alignment.Center)) }
                            }
                            Text(panel.caption, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp), maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(Color.Red).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.White)
            }
        },
        content = { content() }
    )
}