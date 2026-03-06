// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

/**
 * DAO for VionBoard protected suggestions.
 *
 * A protected entry maps a short trigger to sensitive text (phone number,
 * email, address, full name, etc.) that is:
 * - Stored encrypted (AES-256-GCM via Android Keystore)
 * - Shown in the suggestion strip as a masked hint only
 * - Revealed only after biometric / master password authentication
 *
 * Schema:
 *   id           INTEGER PRIMARY KEY AUTOINCREMENT
 *   trigger      TEXT UNIQUE         — short word that activates the entry ("91", "vi", "fn")
 *   masked       TEXT                — display hint shown in strip  ("91••••••21", "vi••••@gmail.com")
 *   blob         BLOB                — AES-GCM encrypted protected text (IV prepended)
 *   label        TEXT                — optional user-friendly name ("Phone", "Work email")
 *   mask_mode    TEXT                — PARTIAL | LABEL | CUSTOM
 *   auth_mode    TEXT                — BIO_THEN_PASS | BIO_ONLY | PASS_ONLY  (default: BIO_THEN_PASS)
 *   created_at   INTEGER
 */
class ProtectedEntriesDao private constructor(context: Context) {

    private val db = Database.getInstance(context)

    // In-memory cache — maps trigger (lowercase) → CachedEntry (masked display + encrypted blob)
    // Never stores plaintext
    private data class CachedEntry(
        val id: Long,
        val trigger: String,
        val masked: String,
        val blob: ByteArray,
        val label: String,
        val authMode: AuthMode
    )

    enum class AuthMode(val value: String) {
        BIO_THEN_PASS("BIO_THEN_PASS"),
        BIO_ONLY("BIO_ONLY"),
        PASS_ONLY("PASS_ONLY");

        companion object {
            fun fromString(s: String) = entries.firstOrNull { it.value == s } ?: BIO_THEN_PASS
        }
    }

    data class ProtectedEntry(
        val id: Long,
        val trigger: String,
        val masked: String,
        val label: String,
        val authMode: AuthMode,
        val createdAt: Long
    )

    private var cache: Map<String, CachedEntry> = emptyMap()
    private var cacheValid = false

    /**
     * Returns the masked display string for a trigger, or null if not found.
     * Safe to call on every keystroke — uses in-memory cache.
     */
    fun findMasked(trigger: String): String? {
        if (!cacheValid) rebuildCache()
        return cache[trigger.lowercase()]?.masked
    }

    /**
     * Returns the auth mode for a trigger, or null if not found.
     */
    fun findAuthMode(trigger: String): AuthMode? {
        if (!cacheValid) rebuildCache()
        return cache[trigger.lowercase()]?.authMode
    }

    /**
     * Decrypts and returns the protected text for a trigger.
     * Only call this AFTER authentication has been confirmed.
     * Returns null if trigger not found or decryption fails.
     */
    fun revealProtectedText(trigger: String): String? {
        if (!cacheValid) rebuildCache()
        val entry = cache[trigger.lowercase()] ?: return null
        return helium314.keyboard.latin.VionKeystore.decrypt(entry.blob)
    }

    /**
     * Adds or updates a protected entry. The protected text is encrypted before storage.
     * The masked display is auto-generated from the protected text unless overridden.
     */
    fun upsert(
        trigger: String,
        protectedText: String,
        label: String = "",
        customMasked: String? = null,
        authMode: AuthMode = AuthMode.BIO_THEN_PASS
    ): Long {
        val encrypted = helium314.keyboard.latin.VionKeystore.encrypt(protectedText)
        val masked = customMasked ?: autoMask(protectedText)
        val values = ContentValues().apply {
            put(COL_TRIGGER, trigger.trim().lowercase())
            put(COL_MASKED, masked)
            put(COL_BLOB, encrypted)
            put(COL_LABEL, label.trim())
            put(COL_MASK_MODE, if (customMasked != null) "CUSTOM" else "PARTIAL")
            put(COL_AUTH_MODE, authMode.value)
            put(COL_CREATED_AT, System.currentTimeMillis())
        }
        val rowId = db.writableDatabase.insertWithOnConflict(
            TABLE, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        cacheValid = false
        return rowId
    }

    /** Deletes a protected entry by trigger. */
    fun delete(trigger: String) {
        db.writableDatabase.delete(TABLE, "$COL_TRIGGER = ?", arrayOf(trigger.lowercase()))
        cacheValid = false
    }

    /** Deletes a protected entry by id. */
    fun deleteById(id: Long) {
        db.writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))
        cacheValid = false
    }

    /** Returns all entries for display in settings (no plaintext exposed). */
    fun getAll(): List<ProtectedEntry> {
        val result = mutableListOf<ProtectedEntry>()
        db.readableDatabase.query(
            TABLE,
            arrayOf(COL_ID, COL_TRIGGER, COL_MASKED, COL_LABEL, COL_AUTH_MODE, COL_CREATED_AT),
            null, null, null, null, "$COL_TRIGGER ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    ProtectedEntry(
                        id = cursor.getLong(0),
                        trigger = cursor.getString(1),
                        masked = cursor.getString(2),
                        label = cursor.getString(3),
                        authMode = AuthMode.fromString(cursor.getString(4)),
                        createdAt = cursor.getLong(5)
                    )
                )
            }
        }
        return result
    }

    private fun rebuildCache() {
        val map = mutableMapOf<String, CachedEntry>()
        db.readableDatabase.query(
            TABLE,
            arrayOf(COL_ID, COL_TRIGGER, COL_MASKED, COL_BLOB, COL_LABEL, COL_AUTH_MODE),
            null, null, null, null, null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val trigger = cursor.getString(1)
                map[trigger] = CachedEntry(
                    id = cursor.getLong(0),
                    trigger = trigger,
                    masked = cursor.getString(2),
                    blob = cursor.getBlob(3),
                    label = cursor.getString(4),
                    authMode = AuthMode.fromString(cursor.getString(5))
                )
            }
        }
        cache = map
        cacheValid = true
    }

    companion object {
        const val TABLE = "protected_entries"
        const val COL_ID = "id"
        const val COL_TRIGGER = "trigger"
        const val COL_MASKED = "masked"
        const val COL_BLOB = "blob"
        const val COL_LABEL = "label"
        const val COL_MASK_MODE = "mask_mode"
        const val COL_AUTH_MODE = "auth_mode"
        const val COL_CREATED_AT = "created_at"

        const val CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TRIGGER TEXT NOT NULL UNIQUE,
                $COL_MASKED TEXT NOT NULL,
                $COL_BLOB BLOB NOT NULL,
                $COL_LABEL TEXT NOT NULL DEFAULT '',
                $COL_MASK_MODE TEXT NOT NULL DEFAULT 'PARTIAL',
                $COL_AUTH_MODE TEXT NOT NULL DEFAULT 'BIO_THEN_PASS',
                $COL_CREATED_AT INTEGER NOT NULL DEFAULT 0
            )
        """

        @Volatile private var instance: ProtectedEntriesDao? = null

        fun getInstance(context: Context): ProtectedEntriesDao {
            return instance ?: synchronized(this) {
                instance ?: ProtectedEntriesDao(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Auto-generates a masked display string from plaintext.
         * Never stores or returns the plaintext — only a hint.
         *
         * Examples:
         *   "viabhron@gmail.com"   → "vi••••••@gmail.com"
         *   "9187654321"           → "91••••••21"
         *   "John Smith"           → "J••• S•••"
         *   "123 Main Street"      → "12• M••• S•••••"
         *   "mypassword123"        → "my••••••••••"
         */
        fun autoMask(plaintext: String): String {
            if (plaintext.length <= 2) return "••••"
            return when {
                // Email: show first 2 chars of local part + ••• + @domain
                plaintext.contains('@') -> {
                    val atIdx = plaintext.indexOf('@')
                    val local = plaintext.substring(0, atIdx)
                    val domain = plaintext.substring(atIdx)
                    val visibleLocal = local.take(2)
                    val maskedLocal = "•".repeat(minOf(local.length - 2, 6).coerceAtLeast(3))
                    "$visibleLocal$maskedLocal$domain"
                }
                // Phone: all digits 6+ chars → show first 2 + •••• + last 2
                plaintext.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
                        && plaintext.count { it.isDigit() } >= 6 -> {
                    val digits = plaintext.filter { it.isDigit() }
                    "${digits.take(2)}${"•".repeat(digits.length - 4)}${digits.takeLast(2)}"
                }
                // Multi-word (name, address): first char of each word + •••
                plaintext.contains(' ') -> {
                    plaintext.split(' ').joinToString(" ") { word ->
                        if (word.isEmpty()) ""
                        else "${word.first()}${"•".repeat(minOf(word.length - 1, 4).coerceAtLeast(2))}"
                    }
                }
                // Generic: show first 2 chars + bullets
                else -> {
                    val visible = plaintext.take(2)
                    "$visible${"•".repeat(minOf(plaintext.length - 2, 8).coerceAtLeast(3))}"
                }
            }
        }
    }
}
