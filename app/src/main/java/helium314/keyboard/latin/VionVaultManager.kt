// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.io.File

/**
 * VionVaultManager — in-memory session manager for the unlocked KeePass vault.
 *
 * Lifecycle:
 *  1. Call [init] once from LatinIME.onCreate to register the screen-off receiver.
 *  2. Call [unlock] with the .kdbx file and master password (entered by user in the panel).
 *  3. Entries are cached in memory. [getEntriesForPackage] filters by current app.
 *  4. Session is cleared automatically on screen off, or explicitly via [lock].
 *
 * Biometric integration (Phase 9): the panel UI will gate [unlock] behind the
 * Phase 5b BiometricBridgeActivity before prompting for the master password.
 */
object VionVaultManager {

    private const val PREFS_NAME    = "vion_vault"
    private const val KEY_VAULT_PATH = "vault_path"
    private const val KEY_AUTO_MATCH = "auto_match"

    // ── Session state ─────────────────────────────────────────────────────────

    private var _entries: List<VionVaultEntry> = emptyList()
    private var _isUnlocked = false
    private var screenOffReceiver: BroadcastReceiver? = null

    val isUnlocked: Boolean get() = _isUnlocked

    /** All entries from the currently unlocked vault. Empty when locked. */
    val entries: List<VionVaultEntry> get() = _entries

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Register a screen-off receiver so the vault locks when the screen turns off.
     * Safe to call multiple times — only registers once.
     */
    fun init(context: Context) {
        if (screenOffReceiver != null) return
        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) lock()
            }
        }
        context.applicationContext.registerReceiver(
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF)
        )
    }

    // ── Unlock / Lock ─────────────────────────────────────────────────────────

    /**
     * Unlock the vault with [password].
     * Returns true on success, false on wrong password or corrupt file.
     * On success the entry list is populated and [isUnlocked] becomes true.
     */
    fun unlock(vaultFile: File, password: String): Boolean {
        return try {
            _entries = VionVaultRepository.openVault(vaultFile, password)
            _isUnlocked = true
            true
        } catch (_: Exception) {
            _isUnlocked = false
            _entries = emptyList()
            false
        }
    }

    /**
     * Clear the session.
     * Called automatically on screen off; can also be called explicitly
     * (e.g. when the user switches away from the vault panel).
     */
    fun lock() {
        _isUnlocked = false
        _entries = emptyList()
    }

    // ── Entry lookup ──────────────────────────────────────────────────────────

    /**
     * Return entries relevant to [packageName].
     *
     * Matching strategy (best-effort heuristic — no URL access from IME):
     *  - Split the package name into meaningful segments (length > 3, not "android"/"google"/"com")
     *  - Match if any segment appears in the entry URL or title (case-insensitive)
     *
     * Returns all entries if [packageName] is null or auto-match is disabled.
     * Returns empty list when vault is locked.
     */
    fun getEntriesForPackage(packageName: String?): List<VionVaultEntry> {
        if (!_isUnlocked) return emptyList()
        if (packageName == null) return _entries
        val parts = packageName.split(".")
            .filter { it.length > 3 && it !in IGNORED_SEGMENTS }
        if (parts.isEmpty()) return _entries
        return _entries.filter { entry ->
            val url   = entry.url.lowercase()
            val title = entry.title.lowercase()
            parts.any { part -> url.contains(part) || title.contains(part) }
        }
    }

    /**
     * Search entries by [query] string across title, username, and URL fields.
     * Used by the vault panel search bar. Returns empty list when locked.
     */
    fun search(query: String): List<VionVaultEntry> {
        if (!_isUnlocked || query.isBlank()) return _entries
        val q = query.trim().lowercase()
        return _entries.filter { entry ->
            entry.title.lowercase().contains(q)
                || entry.username.lowercase().contains(q)
                || entry.url.lowercase().contains(q)
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /** Persist the path of the active vault file across sessions. */
    fun saveVaultPath(context: Context, path: String) {
        prefs(context).edit().putString(KEY_VAULT_PATH, path).apply()
    }

    /** Retrieve the previously saved vault file path, or null if none. */
    fun loadVaultPath(context: Context): String? =
        prefs(context).getString(KEY_VAULT_PATH, null)

    /** Retrieve the active vault file if its path is saved and the file exists. */
    fun loadVaultFile(context: Context): File? {
        val path = loadVaultPath(context) ?: return null
        return File(path).takeIf { it.exists() }
    }

    /** Save whether auto-match by app package should be used. */
    fun saveAutoMatch(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_MATCH, enabled).apply()
    }

    /** Whether auto-match by app package is enabled (default: true). */
    fun isAutoMatchEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_MATCH, true)

    // ── Private ───────────────────────────────────────────────────────────────

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Package name segments that are too generic to be useful for matching. */
    private val IGNORED_SEGMENTS = setOf(
        "android", "google", "com", "org", "net", "app", "www",
        "mobile", "browser", "client", "main", "base",
    )
}
