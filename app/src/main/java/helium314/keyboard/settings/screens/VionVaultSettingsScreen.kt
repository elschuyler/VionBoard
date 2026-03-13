// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helium314.keyboard.latin.VionVaultManager
import java.io.File

// ── Colours (match VionVaultActivity palette) ─────────────────────────────────
private val VS_Bg      = Color(0xFF1B2B1B)
private val VS_Surface = Color(0xFF243424)
private val VS_Accent  = Color(0xFFFF8C00)
private val VS_Text    = Color(0xFFE8F5E8)
private val VS_Sub     = Color(0xFF9DB89D)
private val VS_Error   = Color(0xFFFF5555)
private val VS_Divider = Color(0xFF2E3E2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VionVaultSettingsScreen(onClickBack: () -> Unit) {
    val context = LocalContext.current

    // ── State ──────────────────────────────────────────────────────────────────
    var vaultPath       by remember { mutableStateOf(VionVaultManager.loadVaultPath(context) ?: "") }
    var biometric       by remember { mutableStateOf(VionVaultManager.isBiometricEnabled(context)) }
    var autoLockMs      by remember { mutableStateOf(VionVaultManager.loadAutoLockMs(context)) }
    var clipboardClear  by remember { mutableStateOf(VionVaultManager.isClipboardClearEnabled(context)) }
    var clipboardDelay  by remember { mutableStateOf(VionVaultManager.loadClipboardClearDelayMs(context)) }
    var showClearDialog by remember { mutableStateOf(false) }
    var pickError       by remember { mutableStateOf("") }
    var pickSuccess     by remember { mutableStateOf("") }

    // ── File picker ────────────────────────────────────────────────────────────
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val destFile = File(context.filesDir, "vault.kdbx")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            VionVaultManager.saveVaultPath(context, destFile.absolutePath)
            vaultPath = destFile.absolutePath
            pickError   = ""
            pickSuccess = "Vault file imported successfully."
        } catch (e: Exception) {
            pickError   = "Failed to import file: ${e.message}"
            pickSuccess = ""
        }
    }

    // ── Confirmation dialog — clear all ───────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all vault settings?", color = VS_Text) },
            text  = {
                Text(
                    "This will remove the vault file path, lock the vault, and reset all vault preferences. " +
                    "Your original .kdbx file is NOT deleted.",
                    color = VS_Sub, fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    VionVaultManager.clearAllVaultSettings(context)
                    vaultPath      = ""
                    biometric      = false
                    autoLockMs     = VionVaultManager.AUTO_LOCK_5MIN
                    clipboardClear = true
                    clipboardDelay = VionVaultManager.CLIPBOARD_DELAY_30S
                    showClearDialog = false
                }) { Text("Clear", color = VS_Error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = VS_Sub)
                }
            },
            containerColor = VS_Surface,
        )
    }

    // ── Screen ─────────────────────────────────────────────────────────────────
    Scaffold(
        containerColor = VS_Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "KeePass / Vault",
                        color = VS_Text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onClickBack) {
                        Text("‹", color = VS_Accent, fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VS_Surface),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {

            // ── Vault File ─────────────────────────────────────────────────────
            VSectionHeader("Vault File")

            VSSurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val displayPath = when {
                        vaultPath.isEmpty() -> "No vault file set"
                        else                -> File(vaultPath).name
                    }
                    Text(
                        text     = displayPath,
                        color    = if (vaultPath.isEmpty()) VS_Sub else VS_Text,
                        fontSize = 13.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    if (vaultPath.isNotEmpty()) {
                        Text(
                            text     = "Stored in internal app storage (your original file is untouched)",
                            color    = VS_Sub,
                            fontSize = 11.sp
                        )
                    }
                    if (pickError.isNotEmpty()) {
                        Text(pickError, color = VS_Error, fontSize = 12.sp)
                    }
                    if (pickSuccess.isNotEmpty()) {
                        Text(pickSuccess, color = Color(0xFF66BB66), fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VSButton(
                            label   = if (vaultPath.isEmpty()) "Select .kdbx file" else "Change file",
                            onClick = { filePicker.launch(arrayOf("*/*")) }
                        )
                        if (vaultPath.isNotEmpty()) {
                            VSButtonOutline(
                                label   = "Clear path",
                                onClick = {
                                    VionVaultManager.saveVaultPath(context, "")
                                    vaultPath   = ""
                                    pickSuccess = ""
                                    pickError   = ""
                                }
                            )
                        }
                    }
                }
            }

            VSDivider()

            // ── Security ───────────────────────────────────────────────────────
            VSectionHeader("Security")

            VSSurface {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Biometric toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric unlock", color = VS_Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "Require fingerprint/face before showing password field",
                                color = VS_Sub, fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked         = biometric,
                            onCheckedChange = {
                                biometric = it
                                VionVaultManager.saveBiometricEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor  = VS_Accent,
                                checkedTrackColor  = VS_Accent.copy(alpha = 0.4f),
                                uncheckedThumbColor = VS_Sub,
                                uncheckedTrackColor = VS_Surface,
                            )
                        )
                    }

                    HorizontalDivider(color = VS_Divider)

                    // Auto-lock timeout
                    Text("Auto-lock timeout", color = VS_Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Lock vault after this period of inactivity", color = VS_Sub, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))

                    val lockOptions = listOf(
                        VionVaultManager.AUTO_LOCK_NEVER  to "Never",
                        VionVaultManager.AUTO_LOCK_30S    to "30 seconds",
                        VionVaultManager.AUTO_LOCK_1MIN   to "1 minute",
                        VionVaultManager.AUTO_LOCK_5MIN   to "5 minutes (default)",
                        VionVaultManager.AUTO_LOCK_15MIN  to "15 minutes",
                    )
                    VSRadioGroup(
                        options   = lockOptions,
                        selected  = autoLockMs,
                        onSelect  = {
                            autoLockMs = it
                            VionVaultManager.saveAutoLockMs(context, it)
                        }
                    )
                }
            }

            VSDivider()

            // ── Clipboard ──────────────────────────────────────────────────────
            VSectionHeader("Clipboard")

            VSSurface {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Clear clipboard toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear clipboard after paste", color = VS_Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "Automatically clear the clipboard after typing a vault entry",
                                color = VS_Sub, fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked         = clipboardClear,
                            onCheckedChange = {
                                clipboardClear = it
                                VionVaultManager.saveClipboardClearEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor   = VS_Accent,
                                checkedTrackColor   = VS_Accent.copy(alpha = 0.4f),
                                uncheckedThumbColor = VS_Sub,
                                uncheckedTrackColor = VS_Surface,
                            )
                        )
                    }

                    if (clipboardClear) {
                        HorizontalDivider(color = VS_Divider)
                        Text("Clear delay", color = VS_Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))

                        val delayOptions = listOf(
                            VionVaultManager.CLIPBOARD_DELAY_5S  to "5 seconds",
                            VionVaultManager.CLIPBOARD_DELAY_15S to "15 seconds",
                            VionVaultManager.CLIPBOARD_DELAY_30S to "30 seconds (default)",
                            VionVaultManager.CLIPBOARD_DELAY_60S to "60 seconds",
                        )
                        VSRadioGroup(
                            options  = delayOptions,
                            selected = clipboardDelay,
                            onSelect = {
                                clipboardDelay = it
                                VionVaultManager.saveClipboardClearDelayMs(context, it)
                            }
                        )
                    }
                }
            }

            VSDivider()

            // ── Danger Zone ────────────────────────────────────────────────────
            VSectionHeader("Danger Zone")

            VSSurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Clear all vault settings",
                        color = VS_Error, fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Removes the stored vault path, locks the vault session, and resets all preferences. " +
                        "Your original .kdbx file on your device is NOT affected.",
                        color = VS_Sub, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    VSButtonOutline(
                        label   = "Clear all vault settings",
                        color   = VS_Error,
                        onClick = { showClearDialog = true }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Shared sub-composables ─────────────────────────────────────────────────────

@Composable
private fun VSectionHeader(title: String) {
    Text(
        text     = title.uppercase(),
        color    = VS_Sub,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun VSSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color  = Color(0xFF1F2F1F),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content  = content
        )
    }
}

@Composable
private fun VSDivider() {
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun VSButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors  = ButtonDefaults.buttonColors(containerColor = VS_Accent)
    ) {
        Text(label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun VSButtonOutline(
    label: String,
    onClick: () -> Unit,
    color: Color = VS_Accent
) {
    OutlinedButton(
        onClick = onClick,
        colors  = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border  = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun VSRadioGroup(
    options:  List<Pair<Long, String>>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == value,
                    onClick  = { onSelect(value) },
                    colors   = RadioButtonDefaults.colors(
                        selectedColor   = VS_Accent,
                        unselectedColor = VS_Sub
                    )
                )
                Text(
                    text     = label,
                    color    = if (selected == value) VS_Text else VS_Sub,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
