package com.cleantv.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

/**
 * Select an app to pick it up, press left/right to move it, select it again to drop it.
 * The new order is saved by [onMove] and used on Home and the Apps tab.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ReorderScreen(apps: List<AppItem>, onMove: (String, Int) -> Unit) {
    var grabbed by remember { mutableStateOf<String?>(null) }
    val state = rememberLazyListState()

    // keep the moving app on screen
    LaunchedEffect(apps) {
        val g = grabbed ?: return@LaunchedEffect
        val i = apps.indexOfFirst { it.pkg == g }
        val vis = state.layoutInfo.visibleItemsInfo
        if (i >= 0 && vis.isNotEmpty() && (i <= vis.first().index || i >= vis.last().index))
            state.animateScrollToItem((i - 2).coerceAtLeast(0))
    }

    Column(Modifier.fillMaxSize()) {
        Text("Reorder apps", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Text("Select an app to pick it up, press left or right to move it, then select it again to drop it.",
            color = Color(0xFFC4C7C5), fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
            itemsIndexed(apps, key = { _, a -> a.pkg }) { _, a ->
                AppCircle(
                    a, 75.dp, open = { grabbed = if (grabbed == a.pkg) null else a.pkg },
                    highlight = grabbed == a.pkg,
                    modifier = Modifier.onPreviewKeyEvent { e ->
                        if (grabbed != a.pkg || e.type != KeyEventType.KeyDown) false
                        else when (e.key) {
                            Key.DirectionLeft -> { onMove(a.pkg, -1); true }
                            Key.DirectionRight -> { onMove(a.pkg, 1); true }
                            Key.DirectionUp, Key.DirectionDown -> true  // stay put while moving
                            else -> false
                        }
                    }
                )
            }
        }
    }
}
