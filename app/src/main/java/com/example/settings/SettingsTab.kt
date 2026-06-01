package com.example.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.MainViewModel

@Composable
fun SettingsTab(viewModel: MainViewModel) {
    var showPinResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = "StreamVault Settings",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumnSettings(viewModel = viewModel, onPinResetClick = { showPinResetDialog = true })
    }

    // PIN PIN RESET CHANGE PASSWORD MODAL
    if (showPinResetDialog) {
        var oldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinResetDialog = false },
            title = { Text("Update Private PIN passcode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Current PIN code is required to authorize modifications.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { oldPin = it },
                        label = { Text("Enter Old PIN") },
                        placeholder = { Text("Default was '1234'") }
                    )

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("Enter New 4-Digit PIN") },
                        placeholder = { Text("e.g. 5678") }
                    )

                    if (errorText.isNotEmpty()) {
                        Text(text = errorText, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (oldPin != viewModel.vaultPasscode) {
                        errorText = "Incorrect old PIN!"
                    } else if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
                        errorText = "New PIN must be exactly 4 digits!"
                    } else {
                        viewModel.changeVaultPasscode(newPin)
                        showPinResetDialog = false
                        oldPin = ""
                        newPin = ""
                        errorText = ""
                    }
                }) {
                    Text("Upgrade PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LazyColumnSettings(
    viewModel: MainViewModel,
    onPinResetClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // THEME CONFIGURATION
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎨 System Visual Theme",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val modes = listOf("Light Mode" to 0, "Dark Navy" to 1, "AMOLED Dark" to 2)
                    modes.forEach { (title, idx) ->
                        val isSelected = viewModel.themeMode == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.themeMode = idx }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // PLAYBACK CONFIGURATION
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⚙️ Playback & Control System",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Auto Resume Playback", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Restore from last position progress", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = viewModel.playbackSettingAutoResume,
                        onCheckedChange = { viewModel.playbackSettingAutoResume = it }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Swiping Gestures Enabled", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Volume & Brightness player overlays", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = viewModel.swipeGesturesEnabled,
                        onCheckedChange = { viewModel.swipeGesturesEnabled = it }
                    )
                }
            }
        }

        // SECURITY AND DATABASES ACTION
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔒 Vault Security & Maintenance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // PIN Code Change
                Button(
                    onClick = onPinResetClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Security, "Update Pin")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Secure Vault Passcode PIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Clear history cached log
                Button(
                    onClick = { viewModel.clearAllHistory() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.DeleteSweep, "Clear database log")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Local Watch History Cache", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
