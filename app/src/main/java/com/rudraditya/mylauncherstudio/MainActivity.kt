package com.rudraditya.mylauncherstudio

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val Night = Color(0xFF0D111B)
private val Panel = Color(0xFF1A2231)
private val Accent = Color(0xFFB9C9FF)
private val Muted = Color(0xFF9BA8C0)

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable,
    val category: Int
)

class MainActivity : ComponentActivity() {
    private fun loadApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val results = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
        return results.mapNotNull { info ->
            val activityInfo = info.activityInfo ?: return@mapNotNull null
            val pkg = activityInfo.packageName
            if (pkg == packageName) return@mapNotNull null
            val appInfo = activityInfo.applicationInfo
            LaunchableApp(
                info.loadLabel(packageManager).toString(),
                pkg,
                info.loadIcon(packageManager),
                if (Build.VERSION.SDK_INT >= 26) appInfo.category else -1
            )
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var apps by remember { mutableStateOf(emptyList<LaunchableApp>()) }
            LaunchedEffect(Unit) { apps = loadApps() }
            StudioHomeLauncher(
                apps = apps,
                openHomeSettings = {
                    try {
                        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                    } catch (_: Exception) {
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                },
                onLaunch = { pkg ->
                    packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
                }
            )
        }
    }
}

@Composable
private fun StudioHomeLauncher(
    apps: List<LaunchableApp>,
    openHomeSettings: () -> Unit,
    onLaunch: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("launcher_preferences", 0) }

    var page by remember { mutableStateOf("Home") }
    var drawerOpen by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var pinned by remember {
        mutableStateOf(prefs.getStringSet("pinned_apps", null)?.toList() ?: emptyList())
    }
    var dockCount by remember { mutableIntStateOf(prefs.getInt("dock_count", 4).coerceIn(2, 6)) }
    var labels by remember { mutableStateOf(prefs.getBoolean("show_labels", true)) }
    var lightTheme by remember { mutableStateOf(prefs.getBoolean("light_theme", false)) }
    var wallpaper by remember { mutableIntStateOf(prefs.getInt("wallpaper", 0).coerceIn(0, 3)) }
    var columns by remember { mutableIntStateOf(prefs.getInt("home_columns", 4).coerceIn(3, 6)) }
    var showPinDialog by remember { mutableStateOf(false) }
    var selectedPins by remember { mutableStateOf(setOf<String>()) }

    val wallpapers = listOf(
        listOf(Color(0xFF26395F), Night),
        listOf(Color(0xFF49305E), Night),
        listOf(Color(0xFF17524F), Night),
        listOf(Color(0xFF60412F), Night)
    )
    val background = if (lightTheme) Brush.verticalGradient(listOf(Color(0xFFE9EEFA), Color(0xFFC9D5ED)))
                     else Brush.verticalGradient(wallpapers[wallpaper])
    val appByPackage = remember(apps) { apps.associateBy { it.packageName } }
    val validPinned = pinned.filter { appByPackage.containsKey(it) }
    val favoriteApps = validPinned.mapNotNull { appByPackage[it] }
    val filteredApps = apps.filter { app ->
        app.label.contains(search, ignoreCase = true) &&
            when (filter) {
                "Games" -> app.category == android.content.pm.ApplicationInfo.CATEGORY_GAME
                "Other" -> app.category != android.content.pm.ApplicationInfo.CATEGORY_GAME
                else -> true
            }
    }

    fun savePins(value: Set<String>) {
        val ordered = apps.map { it.packageName }.filter { it in value }
        // Keep the user's existing order, then append newly selected apps.
        val result = (pinned.filter { it in value } + ordered.filter { it !in pinned }).distinct()
        pinned = result
        prefs.edit().putStringSet("pinned_apps", result.toSet()).apply()
    }

    LaunchedEffect(apps) {
        if (apps.isNotEmpty() && prefs.getBoolean("first_run", true)) {
            val starter = apps.take(4).map { it.packageName }
            pinned = starter
            prefs.edit().putStringSet("pinned_apps", starter.toSet()).putBoolean("first_run", false).apply()
        }
    }

    BackHandler(enabled = drawerOpen || page != "Home" || showPinDialog) {
        when {
            showPinDialog -> showPinDialog = false
            drawerOpen -> { drawerOpen = false; search = "" }
            else -> page = "Home"
        }
    }

    MaterialTheme(
        colorScheme = if (lightTheme) lightColorScheme(
            primary = Color(0xFF445A92), background = Color(0xFFE9EEFA),
            surface = Color(0xFFF6F8FD), onSurface = Color(0xFF17213A)
        ) else darkColorScheme(
            primary = Accent, background = Night, surface = Panel,
            onSurface = Color.White
        )
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(background)
                .pointerInput(drawerOpen, page) {
                    var dragDistance = 0f
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragDistance < -70f && page == "Home") drawerOpen = true
                            if (dragDistance > 70f && drawerOpen) { drawerOpen = false; search = "" }
                            dragDistance = 0f
                        },
                        onVerticalDrag = { change: PointerInputChange, amount: Float ->
                            dragDistance += amount
                        }
                    )
                }
        ) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (drawerOpen || page != "Home") {
                        IconButton(onClick = {
                            if (drawerOpen) { drawerOpen = false; search = "" } else page = "Home"
                        }) { Icon(Icons.Default.ArrowBack, "Back", tint = Accent) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("MYLAUNCHER STUDIO", color = if (lightTheme) Color(0xFF64708B) else Muted,
                            fontSize = 10.sp, letterSpacing = 2.sp)
                        Text(
                            when { drawerOpen -> "All apps"; page == "Customize" -> "Personalize"; else -> "Home" },
                            color = if (lightTheme) Color(0xFF17213A) else Color.White,
                            fontSize = 28.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { page = "Customize"; drawerOpen = false }) {
                        Icon(Icons.Default.Settings, "Launcher settings", tint = Accent)
                    }
                }

                when {
                    drawerOpen -> {
                        SearchBox(search, { search = it }, "Search installed apps", lightTheme)
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All", "Games", "Other").forEach { item ->
                                FilterChip(
                                    selected = filter == item,
                                    onClick = { filter = item },
                                    label = { Text(item) }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text("${filteredApps.size}", color = if (lightTheme) Color.DarkGray else Muted,
                                modifier = Modifier.align(Alignment.CenterVertically))
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(76.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                AppTile(app, labels, lightTheme, onLaunch)
                            }
                        }
                    }
                    page == "Customize" -> {
                        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 18.dp)) {
                            item {
                                Text("HOME SCREEN", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                                SettingSwitch("Show app labels", "Show names below home icons", labels) {
                                    labels = it; prefs.edit().putBoolean("show_labels", it).apply()
                                }
                                Spacer(Modifier.height(14.dp))
                                Text("HOME GRID", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                                Text("Columns: $columns", color = if (lightTheme) Color(0xFF17213A) else Color.White,
                                    modifier = Modifier.padding(top = 8.dp))
                                Slider(value = columns.toFloat(), onValueChange = {
                                    columns = it.toInt().coerceIn(3, 6)
                                    prefs.edit().putInt("home_columns", columns).apply()
                                }, valueRange = 3f..6f, steps = 2)
                                Spacer(Modifier.height(8.dp))
                                Text("DOCK", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                                Text("Visible dock apps: $dockCount", color = if (lightTheme) Color(0xFF17213A) else Color.White,
                                    modifier = Modifier.padding(top = 8.dp))
                                Slider(value = dockCount.toFloat(), onValueChange = {
                                    dockCount = it.toInt().coerceIn(2, 6)
                                    prefs.edit().putInt("dock_count", dockCount).apply()
                                }, valueRange = 2f..6f, steps = 3)
                                Spacer(Modifier.height(16.dp))
                                Text("WALLPAPER", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                                    wallpapers.forEachIndexed { index, colors ->
                                        Box(
                                            Modifier.size(54.dp).clip(RoundedCornerShape(16.dp))
                                                .background(Brush.verticalGradient(colors))
                                                .clickable {
                                                    wallpaper = index
                                                    prefs.edit().putInt("wallpaper", index).apply()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) { if (wallpaper == index) Icon(Icons.Default.Check, null, tint = Color.White) }
                                    }
                                }
                                SettingSwitch("Light theme", "Use a light launcher interface", lightTheme) {
                                    lightTheme = it; prefs.edit().putBoolean("light_theme", it).apply()
                                }
                                Spacer(Modifier.height(16.dp))
                                Text("DEFAULT HOME APP", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                                Text("Choose MyLauncher Studio as your phone's Home app. Then the phone's Home button opens this launcher.",
                                    color = if (lightTheme) Color(0xFF5E6B83) else Muted, fontSize = 12.sp,
                                    lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
                                Button(onClick = openHomeSettings) { Text("Set as default launcher") }
                                Spacer(Modifier.height(18.dp))
                                Text("PINNED HOME APPS", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                                OutlinedButton(onClick = {
                                    selectedPins = pinned.toSet()
                                    showPinDialog = true
                                }, modifier = Modifier.padding(top = 8.dp)) { Text("Manage home apps") }
                            }
                        }
                    }
                    else -> {
                        Column(Modifier.weight(1f)) {
                            Spacer(Modifier.height(8.dp))
                            Text("Make your phone feel like yours.", color = if (lightTheme) Color(0xFF4E5D7A) else Accent,
                                fontSize = 14.sp)
                            Text("Your apps, your space.", color = if (lightTheme) Color(0xFF17213A) else Color.White,
                                fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
                            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("HOME APPS", color = if (lightTheme) Color(0xFF64708B) else Muted,
                                    fontSize = 11.sp, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
                                Text("+ EDIT", color = Accent, fontSize = 12.sp,
                                    modifier = Modifier.clickable {
                                        selectedPins = pinned.toSet()
                                        showPinDialog = true
                                    }.padding(8.dp))
                            }
                            if (favoriteApps.isEmpty()) {
                                Surface(color = Color.White.copy(alpha = .09f), shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Add, null, tint = Accent, modifier = Modifier.size(28.dp))
                                        Text("Add apps to your home screen", color = if (lightTheme) Color.DarkGray else Color.White,
                                            fontWeight = FontWeight.SemiBold)
                                        Text("Use EDIT to choose your shortcuts.", color = Muted, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(columns),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(bottom = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(favoriteApps, key = { it.packageName }) { app ->
                                        AppTile(app, labels, lightTheme, onLaunch)
                                    }
                                }
                            }
                            Surface(color = Color.White.copy(alpha = .13f), shape = RoundedCornerShape(28.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    favoriteApps.take(dockCount).forEach { app ->
                                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                            AppTile(app, false, lightTheme, onLaunch, compact = true)
                                        }
                                    }
                                    if (favoriteApps.isEmpty()) {
                                        Text("Dock • pin apps with EDIT", color = Muted, fontSize = 12.sp,
                                            modifier = Modifier.padding(12.dp))
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = Accent.copy(alpha = .20f), shape = CircleShape,
                                    modifier = Modifier.clickable { drawerOpen = true }.padding(2.dp)) {
                                    Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Apps, null, tint = Accent, modifier = Modifier.size(18.dp))
                                        Text("  Swipe up or tap to open apps", color = if (lightTheme) Color(0xFF34415B) else Color.White,
                                            fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = { Text("Choose home screen apps") },
                text = {
                    LazyColumn(Modifier.heightIn(max = 430.dp)) {
                        items(apps, key = { it.packageName }) { app ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    selectedPins = if (app.packageName in selectedPins)
                                        selectedPins - app.packageName else selectedPins + app.packageName
                                }.padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(app, Modifier.size(34.dp))
                                Text(app.label, modifier = Modifier.weight(1f).padding(start = 10.dp),
                                    color = if (lightTheme) Color(0xFF17213A) else Color.White, maxLines = 1)
                                Checkbox(checked = app.packageName in selectedPins,
                                    onCheckedChange = { checked ->
                                        selectedPins = if (checked) selectedPins + app.packageName
                                            else selectedPins - app.packageName
                                    })
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        savePins(selectedPins)
                        showPinDialog = false
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancel") } },
                containerColor = if (lightTheme) Color(0xFFF6F8FD) else Panel
            )
        }
    }
}

@Composable
private fun SearchBox(value: String, onValue: (String) -> Unit, hint: String, light: Boolean) {
    Surface(color = if (light) Color.White.copy(alpha = .85f) else Color.White.copy(alpha = .10f),
        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = Accent)
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value, onValueChange = onValue, modifier = Modifier.weight(1f), singleLine = true,
                textStyle = TextStyle(color = if (light) Color(0xFF17213A) else Color.White, fontSize = 14.sp),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text(hint, color = Muted, fontSize = 14.sp)
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AppIcon(app: LaunchableApp, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context -> ImageView(context).apply {
            setImageDrawable(app.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        } },
        modifier = modifier
    )
}

@Composable
private fun AppTile(
    app: LaunchableApp,
    labels: Boolean,
    light: Boolean,
    onLaunch: (String) -> Unit,
    compact: Boolean = false
) {
    Column(
        Modifier.clip(RoundedCornerShape(18.dp)).clickable { onLaunch(app.packageName) }
            .padding(vertical = if (compact) 1.dp else 5.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(if (compact) 46.dp else 54.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (light) Color.White.copy(alpha = .82f) else Color.White.copy(alpha = .11f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AppIcon(app, Modifier.size(if (compact) 30.dp else 36.dp))
            }
        }
        if (labels && !compact) {
            Text(app.label, color = if (light) Color(0xFF26314A) else Color.White.copy(alpha = .94f),
                fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1,
                modifier = Modifier.padding(top = 6.dp))
        }
    }
}
