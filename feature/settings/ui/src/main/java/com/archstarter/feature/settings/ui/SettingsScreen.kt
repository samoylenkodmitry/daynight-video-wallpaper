package com.archstarter.feature.settings.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.feature.settings.api.ScheduleSlotUi
import com.archstarter.feature.settings.api.SettingsPresenter
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.core.common.wallpaper.WallpaperScheduleMode
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(onExit: () -> Unit = {}) {
    val presenter = rememberPresenter<SettingsPresenter, Unit>()
    val state by presenter.state.collectAsStateWithLifecycle()
    SettingsContent(state = state, presenter = presenter, onExit = onExit)
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    presenter: SettingsPresenter,
    onExit: () -> Unit,
) {
    var editingSlot by remember { mutableStateOf<ScheduleSlotUi?>(null) }
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }

    editingSlot?.let { slot ->
        val timeState = remember(slot) {
            rememberTimePickerState(
                initialHour = slot.startMinutes / 60,
                initialMinute = slot.startMinutes % 60,
                is24Hour = false,
            )
        }
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = { Text("Set ${slot.title} start time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = timeState.hour * 60 + timeState.minute
                        presenter.onSlotTimeSelected(slot.slot, minutes)
                        editingSlot = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSlot = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Wallpaper schedule", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Choose how DayNight Video Wallpaper determines which video plays.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onExit) { Text("Done") }
        }

        ModePicker(
            selected = state.scheduleMode,
            onSelected = presenter::onScheduleModeSelected,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Schedule details", style = MaterialTheme.typography.titleMedium)
                Text(state.description, style = MaterialTheme.typography.bodyMedium)
                if (state.scheduleMode == WallpaperScheduleMode.FIXED) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.slots, key = { it.slot }) { slot ->
                            SlotRow(
                                slot = slot,
                                formatter = formatter,
                                onEdit = { editingSlot = slot },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = presenter::onResetDefaults) {
                        Text("Reset to recommended times")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModePicker(
    selected: WallpaperScheduleMode,
    onSelected: (WallpaperScheduleMode) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilterChip(
            selected = selected == WallpaperScheduleMode.SOLAR,
            onClick = { onSelected(WallpaperScheduleMode.SOLAR) },
            label = { Text("Solar aware") },
        )
        FilterChip(
            selected = selected == WallpaperScheduleMode.FIXED,
            onClick = { onSelected(WallpaperScheduleMode.FIXED) },
            label = { Text("Custom times") },
        )
    }
}

@Composable
private fun SlotRow(
    slot: ScheduleSlotUi,
    formatter: DateTimeFormatter,
    onEdit: () -> Unit,
) {
    val formatted = rememberUpdatedState(formatMinutes(slot.startMinutes, formatter))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(slot.title, style = MaterialTheme.typography.titleSmall)
            Text(formatted.value, style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = onEdit) { Text("Change") }
    }
}

private fun formatMinutes(minutes: Int, formatter: DateTimeFormatter): String {
    val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val time = LocalTime.of(normalized / 60, normalized % 60)
    return time.format(formatter)
}
