package com.rudraditya.mylauncherstudio

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LaunchableApp(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class MainActivity : ComponentActivity() {
    private fun loadApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
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
                background = Color(0xFF10131B),
                surface = Color(0xFF1B202B),
                primary = Color(0xFFB8C7FF)
            )) {
                LauncherScreen(apps = apps, onLaunch = { pkg ->
                    packageManager.getLaunchIntentForPackage(pkg)?.let(::startActivity)
                })
            }
        }
    }
}

@Composable
private fun LauncherScreen(apps: List<LaunchableApp>, onLaunch: (String) -> Unit) {
    var drawerOpen by remember { mutableStateOf(false) }
    val background = Brush.verticalGradient(
        listOf(Color(0xFF252D43), Color(0xFF11151F), Color(0xFF090C12))
    )
    Box(Modifier.fillMaxSize().background(background).padding(horizontal = 22.dp, vertical = 26.dp)) {
        if (drawerOpen) {
            Column(Modifier.fillMaxSize()) {
                Text("Your apps", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Text("All applications", color = Color(0xFFADB7CC), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(76.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(apps, key = { it.packageName }) { app -> AppTile(app, onLaunch) }
                }
                GlassBar(Modifier.fillMaxWidth().clickable { drawerOpen = false }) {
                    Text("Back to home", color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(24.dp))
                Text("Good day.", color = Color(0xFFB8C7FF), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Make it yours.", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("MYLAUNCHER STUDIO", color = Color(0xFF8D98B1), fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                GlassBar(Modifier.fillMaxWidth().clickable { drawerOpen = true }) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFB8C7FF))
                    Text("Search apps", color = Color(0xFFB8C0D0), modifier = Modifier.padding(start = 12.dp))
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Apps, contentDescription = "Open app drawer", tint = Color(0xFFB8C7FF))
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    apps.take(4).forEach { app -> Box(Modifier.weight(1f)) { AppTile(app, onLaunch) } }
                }
            }
        }
    }
}

@Composable
private fun GlassBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun AppTile(app: LaunchableApp, onLaunch: (String) -> Unit) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(18.dp))
            .clickable { onLaunch(app.packageName) }
            .padding(vertical = 5.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(17.dp),
            color = Color.White.copy(alpha = 0.10f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = remember(app.packageName) { androidx.compose.ui.graphics.painter.DrawablePainter(app.icon) },
                    contentDescription = app.label,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Text(
            app.label,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 7.dp)
        )
    }
}
