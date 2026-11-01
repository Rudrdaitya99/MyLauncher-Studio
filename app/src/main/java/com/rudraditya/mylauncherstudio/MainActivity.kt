package com.rudraditya.mylauncherstudio

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val Ink = Color(0xFF10131B)
private val Panel = Color(0xFF1B2230)
private val Accent = Color(0xFFB9C8FF)
private val Muted = Color(0xFF9BA8C0)

data class LaunchableApp(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class MainActivity : ComponentActivity() {
    private fun loadApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") packageManager.queryIntentActivities(intent, 0)
        }
        return matches.mapNotNull { info ->
            val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
            LaunchableApp(info.loadLabel(packageManager).toString(), pkg, info.loadIcon(packageManager))
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var apps by remember { mutableStateOf(emptyList<LaunchableApp>()) }
            LaunchedEffect(Unit) { apps = loadApps() }
            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Panel, primary = Accent,
                onBackground = Color.White, onSurface = Color.White
            )) {
                StudioLauncher(
                    apps = apps,
                    onLaunch = { pkg -> packageManager.getLaunchIntentForPackage(pkg)?.let(::startActivity) }
                )
            }
        }
    }
}

@Composable
private fun StudioLauncher(apps: List<LaunchableApp>, onLaunch: (String) -> Unit) {
    var page by remember { mutableStateOf("Home") }
    var search by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(listOf<String>()) }
    var dockCount by remember { mutableIntStateOf(4) }
    var labels by remember { mutableStateOf(true) }
    var rounded by remember { mutableStateOf(true) }
    var wallpaper by remember { mutableIntStateOf(0) }
    var addDialog by remember { mutableStateOf(false) }

    val wallpapers = listOf(
        listOf(Color(0xFF263657), Color(0xFF10131B)),
        listOf(Color(0xFF39264F), Color(0xFF10131B)),
        listOf(Color(0xFF164B4A), Color(0xFF10131B)),
        listOf(Color(0xFF51352B), Color(0xFF10131B))
    )
    val bg = Brush.verticalGradient(wallpapers[wallpaper])
    val pinnedApps = pinned.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
    val filtered = apps.filter { it.label.contains(search, true) }.sortedBy { it.label.lowercase() }

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MYLAUNCHER STUDIO", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(when (page) { "Apps" -> "App drawer"; "Customize" -> "Your style"; else -> "Your space" },
                        color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                }
                Surface(color = Color.White.copy(alpha = .10f), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Settings, "Settings", tint = Accent, modifier = Modifier.clickable { page = "Customize" }.padding(12.dp))
                }
            }

            when (page) {
                "Apps" -> {
                    SearchField(search, { search = it }, "Search all installed apps")
                    Spacer(Modifier.height(16.dp))
                    Text("${filtered.size} APPS", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(10.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(82.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) { items(filtered, key = { it.packageName }) { AppTile(it, labels, rounded, onLaunch) } }
                }
                "Customize" -> {
                    Column(Modifier.weight(1f)) {
                        Text("HOME SCREEN", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        SettingRow("Show app labels", "Display names below icons", labels) { labels = it }
                        SettingRow("Rounded icon cards", "Use soft rounded icon backgrounds", rounded) { rounded = it }
                        Spacer(Modifier.height(22.dp))
                        Text("DOCK", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Text("Apps shown in dock: $dockCount", color = Color.White, modifier = Modifier.padding(top = 12.dp))
                        Slider(value = dockCount.toFloat(), onValueChange = { dockCount = it.toInt().coerceIn(2, 6) }, valueRange = 2f..6f, steps = 3)
                        Spacer(Modifier.height(18.dp))
                        Text("WALLPAPER STYLE", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 14.dp)) {
                            wallpapers.forEachIndexed { index, colors ->
                                Box(Modifier.size(58.dp).clip(RoundedCornerShape(16.dp))
                                    .background(Brush.verticalGradient(colors))
                                    .clickable { wallpaper = index },
                                    contentAlignment = Alignment.Center) {
                                    if (wallpaper == index) Text("✓", color = Color.White, fontSize = 22.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("APP INFO", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Text("MyLauncher Studio  •  Preview build", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                        Text("Set MyLauncher Studio as your default Home app in Android Settings to use it as your launcher.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                else -> {
                    Column(Modifier.weight(1f)) {
                        Spacer(Modifier.height(18.dp))
                        Text("A calmer home for", color = Accent, fontSize = 15.sp)
                        Text("everything you use.", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
                        Text("Pin your favourite apps. Shape your space.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                        Spacer(Modifier.height(28.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("FAVOURITES", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
                            Text("+ Add apps", color = Accent, fontSize = 13.sp, modifier = Modifier.clickable { addDialog = true }.padding(8.dp))
                        }
                        if (pinnedApps.isEmpty()) {
                            Surface(color = Color.White.copy(alpha = .07f), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, null, tint = Accent, modifier = Modifier.size(28.dp))
                                    Text("Your home is ready to personalize", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("Tap Add apps to pin shortcuts here.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.padding(top = 8.dp), userScrollEnabled = false,
                                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(pinnedApps, key = { it.packageName }) { AppTile(it, labels, rounded, onLaunch) }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("DOCK", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 10.dp))
                        Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                pinnedApps.take(dockCount).forEach { AppTile(it, false, rounded, onLaunch, Modifier.weight(1f)) }
                                if (pinnedApps.isEmpty()) Text("Add apps to build your dock", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }

            NavigationBar(containerColor = Color(0xCC111722), contentColor = Accent) {
                NavigationBarItem(selected = page == "Home", onClick = { page = "Home"; search = "" }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = page == "Apps", onClick = { page = "Apps"; search = "" }, icon = { Icon(Icons.Default.Apps, null) }, label = { Text("Apps") })
                NavigationBarItem(selected = page == "Customize", onClick = { page = "Customize" }, icon = { Icon(Icons.Default.Palette, null) }, label = { Text("Customize") })
            }
        }
    }

    if (addDialog) {
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text("Add home shortcuts") },
            text = {
                Column(Modifier.heightIn(max = 420.dp)) {
                    apps.filter { it.packageName !in pinned }.forEach { app ->
                        Row(Modifier.fillMaxWidth().clickable {
                            pinned = (pinned + app.packageName).take(12)
                            addDialog = false
                        }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(app, rounded, Modifier.size(36.dp))
                            Text(app.label, color = Color.White, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { addDialog = false }) { Text("Done") } },
            containerColor = Panel
        )
    }
}

@Composable
private fun SearchField(value: String, onValue: (String) -> Unit, hint: String) {
    Surface(color = Color.White.copy(alpha = .10f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = Accent)
            Spacer(Modifier.width(10.dp))
            BasicTextField(value, onValue, modifier = Modifier.weight(1f), singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                decorationBox = { inner -> Box { if (value.isEmpty()) Text(hint, color = Muted, fontSize = 14.sp); inner() } })
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AppIcon(app: LaunchableApp, rounded: Boolean, modifier: Modifier = Modifier) {
    AndroidView(factory = { context -> ImageView(context).apply {
        setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER
    } }, modifier = modifier)
}

@Composable
private fun AppTile(app: LaunchableApp, labels: Boolean, rounded: Boolean, onLaunch: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable { onLaunch(app.packageName) }.padding(vertical = 5.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(54.dp), shape = RoundedCornerShape(if (rounded) 17.dp else 10.dp), color = Color.White.copy(alpha = .10f)) {
            Box(contentAlignment = Alignment.Center) { AppIcon(app, rounded, Modifier.size(36.dp)) }
        }
        if (labels) Text(app.label, color = Color.White.copy(alpha = .92f), fontSize = 10.sp, textAlign = TextAlign.Center,
            maxLines = 1, modifier = Modifier.padding(top = 6.dp))
    }
}
