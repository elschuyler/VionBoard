// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.prefs
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * VionBoard-specific encrypted backup/restore.
 *
 * Format (.vionbackup binary):
 *   [4 bytes magic]  = 0x56 0x49 0x4F 0x4E  ("VION")
 *   [1 byte version] = 0x01
 *   [16 bytes salt]
 *   [12 bytes IV]
 *   [remaining]      = AES-256-GCM ciphertext of UTF-8 JSON payload
 *
 * Key derivation: PBKDF2WithHmacSHA256, 100 000 iterations, 256-bit key.
 */
object VionBackupManager {

    private const val TAG = "VionBackupManager"
    private val MAGIC = byteArrayOf(0x56, 0x49, 0x4F, 0x4E)
    private const val VERSION: Byte = 0x01
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128

    // SharedPreferences keys — must match what each feature actually writes
    private const val SNIPPETS_PREFS_NAME = "vion_snippets"
    private const val VAULT_PATH_KEY = "vion_vault_path"
    private const val PROTECTED_WORDS_KEY = "protected_suggestions"
    private const val TOOLBAR_KEYS_PREF = "pref_toolbar_keys"
    private val SNIPPET_CATEGORIES = listOf("git", "termux", "markdown", "symbols")
    private val VION_TOOLBAR_KEY_NAMES = listOf(
        "VION_VAULT", "VION_SNIPPETS", "VION_SHORTCUTS", "TIMESTAMP", "EMOJI_SEARCH"
    )

    // ── Payload build / restore ───────────────────────────────────────────────

    private fun buildPayload(context: Context): String {
        val prefs = context.prefs()
        val snippetPrefs = context.getSharedPreferences(SNIPPETS_PREFS_NAME, Context.MODE_PRIVATE)

        val payload = JSONObject()

        // vault path
        val vaultPath = prefs.getString(VAULT_PATH_KEY, null)
        payload.put("vault_path", vaultPath ?: JSONObject.NULL)

        // custom snippets — stored as newline-separated strings under key "custom_<cat>"
        val snippetsObj = JSONObject()
        SNIPPET_CATEGORIES.forEach { cat ->
            val raw = snippetPrefs.getString("custom_$cat", "") ?: ""
            val arr = JSONArray()
            raw.split("\n").filter { it.isNotBlank() }.forEach { arr.put(it) }
            snippetsObj.put(cat, arr)
        }
        payload.put("custom_snippets", snippetsObj)

        // protected words (string set)
        val protectedSet = prefs.getStringSet(PROTECTED_WORDS_KEY, emptySet()) ?: emptySet()
        val protArr = JSONArray()
        protectedSet.forEach { protArr.put(it) }
        payload.put("protected_words", protArr)

        // toolbar VionBoard key states — stored in a single pref string
        val toolbarPref = prefs.getString(TOOLBAR_KEYS_PREF, "") ?: ""
        val toolbarObj = JSONObject()
        VION_TOOLBAR_KEY_NAMES.forEach { key ->
            val enabled = toolbarPref.split(";", "\n", ",").any { entry ->
                val trimmed = entry.trim()
                trimmed == key || trimmed.startsWith("$key:true") || trimmed.startsWith("$key|true")
            }
            toolbarObj.put(key, enabled)
        }
        payload.put("toolbar_vion_keys", toolbarObj)
        // also store the raw toolbar pref so restore is lossless for VionBoard keys
        payload.put("toolbar_pref_raw", toolbarPref)

        return payload.toString(2)
    }

    private fun restorePayload(context: Context, json: String) {
        val prefs = context.prefs()
        val snippetPrefs = context.getSharedPreferences(SNIPPETS_PREFS_NAME, Context.MODE_PRIVATE)
        val payload = JSONObject(json)

        // vault path
        if (!payload.isNull("vault_path")) {
            prefs.edit().putString(VAULT_PATH_KEY, payload.getString("vault_path")).apply()
        }

        // custom snippets
        val snippetsObj = payload.optJSONObject("custom_snippets")
        if (snippetsObj != null) {
            val edit = snippetPrefs.edit()
            SNIPPET_CATEGORIES.forEach { cat ->
                val arr = snippetsObj.optJSONArray(cat)
                if (arr != null) {
                    val joined = (0 until arr.length()).joinToString("\n") { arr.getString(it) }
                    edit.putString("custom_$cat", joined)
                }
            }
            edit.apply()
        }

        // protected words
        val protArr = payload.optJSONArray("protected_words")
        if (protArr != null) {
            val set = (0 until protArr.length()).map { protArr.getString(it) }.toSet()
            prefs.edit().putStringSet(PROTECTED_WORDS_KEY, set).apply()
        }

        // toolbar — restore raw pref (preserves all key states including non-Vion keys)
        val rawToolbar = payload.optString("toolbar_pref_raw", null)
        if (!rawToolbar.isNullOrBlank()) {
            prefs.edit().putString(TOOLBAR_KEYS_PREF, rawToolbar).apply()
        }
    }

    // ── Plaintext JSON export / import ────────────────────────────────────────

    fun exportJson(context: Context, out: OutputStream) {
        out.write(buildPayload(context).toByteArray(Charsets.UTF_8))
        out.flush()
    }

    fun importJson(context: Context, input: InputStream) {
        val json = input.readBytes().toString(Charsets.UTF_8)
        restorePayload(context, json)
    }

    // ── Encrypted binary export / import ─────────────────────────────────────

    fun exportEncrypted(context: Context, password: String, out: OutputStream) {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv   = ByteArray(IV_LENGTH).also  { SecureRandom().nextBytes(it) }
        val key  = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(buildPayload(context).toByteArray(Charsets.UTF_8))
        out.write(MAGIC)
        out.write(byteArrayOf(VERSION))
        out.write(salt)
        out.write(iv)
        out.write(ciphertext)
        out.flush()
    }

    /**
     * Returns true on success, false if password is wrong or file is corrupt.
     */
    fun importEncrypted(context: Context, password: String, input: InputStream): Boolean {
        return try {
            val bytes = input.readBytes()
            var pos = 0
            val magic = bytes.sliceArray(pos until pos + 4); pos += 4
            if (!magic.contentEquals(MAGIC)) error("Not a VionBoard backup file")
            pos += 1 // version byte
            val salt = bytes.sliceArray(pos until pos + SALT_LENGTH); pos += SALT_LENGTH
            val iv   = bytes.sliceArray(pos until pos + IV_LENGTH);   pos += IV_LENGTH
            val ciphertext = bytes.sliceArray(pos until bytes.size)
            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            val json = cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
            restorePayload(context, json)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Encrypted import failed", t)
            false
        }
    }

    // ── Crypto ────────────────────────────────────────────────────────────────

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }
}
