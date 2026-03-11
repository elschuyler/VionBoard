// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R
import helium314.keyboard.latin.VionBackupManager
import helium314.keyboard.latin.utils.ExecutorUtils
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.previewDark
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.filePicker
import helium314.keyboard.settings.initPreview
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.CountDownLatch

@Composable
fun VionBackupScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current

    // dialog state
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var passwordDialogMode by rememberSaveable { mutableStateOf("export") } // "export" or "import"
    var password by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf(false) }

    // file pickers
    val jsonExportPicker = jsonExportPicker()
    val jsonImportPicker = jsonImportPicker()
    val encExportPicker  = encExportPicker(passwordProvider = { password })
    val encImportPicker  = encImportPicker(passwordProvider = { password }, onWrongPassword = { passwordError = true })

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = "VionBoard Backup",
        settings = emptyList(),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SectionLabel("Plain JSON")
            Text(
                "Exports vault path, custom snippets, protected words, and toolbar layout as a readable JSON file. No password required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(
                onClick = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_TITLE, "vionboard_backup_$date.json")
                        .setType("application/json")
                    jsonExportPicker.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Export JSON") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/json")
                    jsonImportPicker.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Import JSON") }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Encrypted (.vionbackup)")
            Text(
                "AES-256 encrypted backup. You must enter the same password to restore. Wrong password = data unreadable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(
                onClick = {
                    password = ""
                    passwordError = false
                    passwordDialogMode = "export"
                    showPasswordDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Export Encrypted") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    password = ""
                    passwordError = false
                    passwordDialogMode = "import"
                    showPasswordDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Import Encrypted") }

            Spacer(Modifier.height(24.dp))
            Text(
                "What is included: vault file path, custom snippets (all tabs), protected word suggestions, VionBoard toolbar key states.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; password = "" },
            title = { Text(if (passwordDialogMode == "export") "Set backup password" else "Enter backup password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = false },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordError,
                        supportingText = if (passwordError) {{ Text("Wrong password or corrupt file") }} else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (password.isBlank()) return@TextButton
                        showPasswordDialog = false
                        if (passwordDialogMode == "export") {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                                .addCategory(Intent.CATEGORY_OPENABLE)
                                .putExtra(Intent.EXTRA_TITLE, "vionboard_backup_$date.vionbackup")
                                .setType("application/octet-stream")
                            encExportPicker.launch(intent)
                        } else {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                                .addCategory(Intent.CATEGORY_OPENABLE)
                                .setType("*/*")
                            encImportPicker.launch(intent)
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false; password = "" }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun jsonExportPicker(): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val ctx = LocalContext.current
    return filePicker { uri ->
        val wait = CountDownLatch(1)
        ExecutorUtils.getBackgroundExecutor(ExecutorUtils.KEYBOARD).execute {
            try {
                ctx.getActivity()?.contentResolver?.openOutputStream(uri)?.use {
                    VionBackupManager.exportJson(ctx, it)
                }
                showToast(ctx, "Backup exported")
            } catch (t: Throwable) {
                showToast(ctx, "Export failed: ${t.message}")
            } finally { wait.countDown() }
        }
        wait.await()
    }
}

@Composable
private fun jsonImportPicker(): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val ctx = LocalContext.current
    return filePicker { uri ->
        val wait = CountDownLatch(1)
        ExecutorUtils.getBackgroundExecutor(ExecutorUtils.KEYBOARD).execute {
            try {
                ctx.getActivity()?.contentResolver?.openInputStream(uri)?.use {
                    VionBackupManager.importJson(ctx, it)
                }
                showToast(ctx, "Backup restored")
            } catch (t: Throwable) {
                showToast(ctx, "Import failed: ${t.message}")
            } finally { wait.countDown() }
        }
        wait.await()
    }
}

@Composable
private fun encExportPicker(passwordProvider: () -> String): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val ctx = LocalContext.current
    return filePicker { uri ->
        val pw = passwordProvider()
        val wait = CountDownLatch(1)
        ExecutorUtils.getBackgroundExecutor(ExecutorUtils.KEYBOARD).execute {
            try {
                ctx.getActivity()?.contentResolver?.openOutputStream(uri)?.use {
                    VionBackupManager.exportEncrypted(ctx, pw, it)
                }
                showToast(ctx, "Encrypted backup exported")
            } catch (t: Throwable) {
                showToast(ctx, "Export failed: ${t.message}")
            } finally { wait.countDown() }
        }
        wait.await()
    }
}

@Composable
private fun encImportPicker(
    passwordProvider: () -> String,
    onWrongPassword: () -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val ctx = LocalContext.current
    return filePicker { uri ->
        val pw = passwordProvider()
        val wait = CountDownLatch(1)
        ExecutorUtils.getBackgroundExecutor(ExecutorUtils.KEYBOARD).execute {
            try {
                val ok = ctx.getActivity()?.contentResolver?.openInputStream(uri)?.use {
                    VionBackupManager.importEncrypted(ctx, pw, it)
                } ?: false
                if (ok) showToast(ctx, "Encrypted backup restored")
                else onWrongPassword()
            } catch (t: Throwable) {
                showToast(ctx, "Import failed: ${t.message}")
            } finally { wait.countDown() }
        }
        wait.await()
    }
}

private fun showToast(ctx: android.content.Context, msg: String) {
    android.os.Handler(android.os.Looper.getMainLooper()).post {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
    }
}

@Preview
@Composable
private fun PreviewScreen() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            VionBackupScreen {}
        }
    }
}
