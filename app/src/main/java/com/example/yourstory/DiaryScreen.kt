package com.example.yourstory

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

enum class DiaryViewMode(val label: String) {
    MIXED("Mixed View"),
    COMIC("Comic Only"),
    TASKS("Tasks Only")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiaryScreen(storyDao: StoryDao, onBack: () -> Unit) {
    val stories by storyDao.getAllStories().collectAsState(initial = emptyList())
    val pagerState = rememberPagerState(pageCount = { stories.size })
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var viewMode by remember { mutableStateOf(DiaryViewMode.MIXED) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("The Chronicles")
                        if (stories.isNotEmpty()) {
                            val currentStory = stories.getOrNull(pagerState.currentPage)
                            if (currentStory != null) {
                                Text(currentStory.date, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("< Back") } },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.ViewCarousel, contentDescription = "Change View")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DiaryViewMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (viewMode == mode) Text("✓ ", color = MaterialTheme.colorScheme.primary)
                                            else Spacer(Modifier.width(14.dp))
                                            Text(mode.label)
                                        }
                                    },
                                    onClick = { viewMode = mode; showMenu = false }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        if (stories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No stories yet. Go complete some quests!")
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFFF5F5DC))
            ) { pageIndex ->
                val story = stories[pageIndex]

                when (viewMode) {
                    DiaryViewMode.COMIC -> {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            ComicPage(story, isFitScreen = true, showHeader = !isLandscape)
                        }
                    }
                    DiaryViewMode.TASKS -> {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            TaskPage(story.completedTasks, isFullScreen = true, showHeader = !isLandscape)
                        }
                    }
                    DiaryViewMode.MIXED -> {
                        if (isLandscape) {
                            // --- LANDSCAPE: OPEN BOOK MODE ---
                            Row(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Left Page (Tasks) - 1/3 width
                                Card(modifier = Modifier.weight(1f).fillMaxHeight(), elevation = CardDefaults.cardElevation(4.dp)) {
                                    TaskPage(story.completedTasks, showHeader = false)
                                }
                                // Right Page (Comic) - 2/3 width
                                Card(modifier = Modifier.weight(2f).fillMaxHeight(), elevation = CardDefaults.cardElevation(4.dp)) {
                                    ComicPage(story, isFitScreen = true, showHeader = false)
                                }
                            }
                        } else {
                            // --- PORTRAIT: VERTICAL STACK ---
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Card(modifier = Modifier.weight(1.2f).fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                                    ComicPage(story, isFitScreen = false, showHeader = true)
                                }
                                Card(modifier = Modifier.weight(0.8f).fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                                    TaskPage(story.completedTasks, showHeader = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun TaskPage(tasks: List<String>, isFullScreen: Boolean = false, showHeader: Boolean = true) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (showHeader) {
            Text(
                text = "Completed Quests",
                style = if (isFullScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks) { taskName ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✓", color = Color.Green, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        taskName,
                        style = if (isFullScreen) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ComicPage(story: Story, isFitScreen: Boolean = false, showHeader: Boolean = true) {
    val panels = listOf(
        story.panel1Path to story.panel1Caption,
        story.panel2Path to story.panel2Caption,
        story.panel3Path to story.panel3Caption,
        story.panel4Path to story.panel4Caption
    )

    Column(modifier = Modifier.padding(16.dp)) {
        if (showHeader) {
            Text(
                text = "Visual Log",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        if (isFitScreen) {
            FitScreenGrid(panels)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(panels) { (path, caption) ->
                    ComicPanelItem(path, caption, isCompact = false)
                }
            }
        }
    }
}

@Composable
fun FitScreenGrid(panels: List<Pair<String?, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ComicPanelItem(panels[0].first, panels[0].second, isCompact = true)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ComicPanelItem(panels[1].first, panels[1].second, isCompact = true)
            }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ComicPanelItem(panels[2].first, panels[2].second, isCompact = true)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ComicPanelItem(panels[3].first, panels[3].second, isCompact = true)
            }
        }
    }
}

@Composable
fun ComicPanelItem(path: String?, caption: String, isCompact: Boolean) {
    if (isCompact) {
        // --- LANDSCAPE: Row Layout (Image Left, Text Right) ---
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Container (Weight 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (path != null) {
                    val file = File(path)
                    AsyncImage(
                        model = if (file.exists()) file else path,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Text Container (Weight 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 6, // Generous limit for landscape
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        // --- PORTRAIT: Column Layout (Image Top, Text Bottom) ---
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (path != null) {
                val file = File(path)
                AsyncImage(
                    model = if (file.exists()) file else path,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth
                )
            }

            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}