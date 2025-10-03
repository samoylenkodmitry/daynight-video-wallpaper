package com.archstarter.feature.catalog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.wallpaper.DaySlot
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.feature.catalog.api.SlotCardState
import com.archstarter.feature.catalog.api.WallpaperHomePresenter
import com.archstarter.feature.catalog.api.WallpaperHomeState

@Composable
fun WallpaperHomeScreen() {
    val presenter = rememberPresenter<WallpaperHomePresenter, Unit>()
    val state by presenter.state.collectAsStateWithLifecycle()
    WallpaperHomeContent(state = state, presenter = presenter)
}

@Composable
fun CatalogScreen() {
    WallpaperHomeScreen()
}

@Composable
private fun WallpaperHomeContent(
    state: WallpaperHomeState,
    presenter: WallpaperHomePresenter,
) {
    var pendingSlot by remember { mutableStateOf<DaySlot?>(null) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val slot = pendingSlot
        pendingSlot = null
        if (uri != null && slot != null) {
            presenter.onPickVideo(slot, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Text(
            text = "DayNight Video Wallpaper",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Assign immersive videos to each part of the day and let them fade in as time moves.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        LazyColumn(
            modifier = Modifier.weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(state.slots, key = { it.slot }) { slotState ->
                SlotCard(
                    state = slotState,
                    onPick = {
                        pendingSlot = slotState.slot
                        videoPicker.launch(arrayOf("video/*"))
                    },
                    onRemove = { presenter.onRemoveVideo(slotState.slot) },
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Playback")
                        ToggleRow(
                            title = "Mute audio",
                            checked = state.mutePlayback,
                            onToggle = { presenter.onToggleMute() },
                        )
                        ToggleRow(
                            title = "Loop videos",
                            checked = state.loopPlayback,
                            onToggle = { presenter.onToggleLoop() },
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Schedule", style = MaterialTheme.typography.titleMedium)
                        Text(state.scheduleSummary, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(onClick = presenter::onOpenSettings) {
                                Text("Adjust schedule")
                            }
                            TextButton(onClick = presenter::onSetWallpaper) {
                                Text("Set live wallpaper")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotCard(
    state: SlotCardState,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(state.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.videoLabel ?: "No video selected",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.videoLabel == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = state.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onPick) { Text("Choose video") }
                if (state.videoLabel != null) {
                    TextButton(onClick = onRemove) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}
