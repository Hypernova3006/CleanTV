package com.cleantv.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.*
import kotlinx.coroutines.delay

/** Popup for choosing what the Live tab opens: any installed app, or the system Live TV. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LivePicker(apps: List<AppItem>, currentLabel: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val fm = LocalFocusManager.current
        LaunchedEffect(Unit) { delay(150); runCatching { fm.moveFocus(FocusDirection.Next) } }
        Column(
            Modifier.fillMaxWidth(0.82f).fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(24.dp)).background(Color(0xF2141418)).padding(28.dp)
        ) {
            Text("Choose your Live app", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Text(
                "Pressing Live will open this app. Long-press Live any time to change it." +
                    (if (currentLabel != null) "  Current: $currentLabel." else ""),
                color = Color(0xFFC4C7C5), fontSize = 14.sp
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton("System Live TV") { onPick("system") }
                PillButton("Cancel", onDismiss)
            }
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                GridCells.Adaptive(110.dp), Modifier.weight(1f), contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(apps, key = { it.pkg }) { a -> AppCircle(a, 64.dp, { x -> onPick(x.pkg) }) }
            }
        }
    }
}
