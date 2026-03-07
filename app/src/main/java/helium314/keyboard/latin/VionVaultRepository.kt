package helium314.keyboard.latin

import app.keemobile.kotpass.constants.BasicField
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.Group
import java.io.File
import java.io.InputStream

data class VionVaultEntry(
    val title: String,
    val username: String,
    val password: String,
    val url: String
)

object VionVaultRepository {

    /**
     * Load entries from any InputStream (file, content URI via contentResolver, etc.).
     * Caller is responsible for closing the stream.
     */
    fun loadFromStream(stream: InputStream, passphrase: String): Result<List<VionVaultEntry>> {
        return try {
            val credentials = Credentials.from(EncryptedValue.fromString(passphrase))
            val database = KeePassDatabase.decode(stream, credentials)
            val entries = mutableListOf<VionVaultEntry>()
            collectEntries(database.content.group, entries)
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convenience wrapper for a File (used for the VionBoard-internal vault copy).
     */
    fun loadFromFile(file: File, passphrase: String): Result<List<VionVaultEntry>> {
        return try {
            file.inputStream().use { stream ->
                loadFromStream(stream, passphrase)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun collectEntries(group: Group, out: MutableList<VionVaultEntry>) {
        for (entry in group.entries) {
            out.add(entry.toVionEntry())
        }
        for (subGroup in group.groups) {
            collectEntries(subGroup, out)
        }
    }

    /**
     * Map a kotpass Entry to our flat VionVaultEntry.
     * BasicField.Title() / UserName() / Password() / Url() each return the
     * canonical KDBX field-key string; entry.fields is Map<String, EntryValue>.
     * EntryValue (Plain or Encrypted) both expose .content: String.
     */
    private fun Entry.toVionEntry(): VionVaultEntry {
        val title    = fields[BasicField.Title()]?.content    ?: ""
        val username = fields[BasicField.UserName()]?.content ?: ""
        val password = fields[BasicField.Password()]?.content ?: ""
        val url      = fields[BasicField.Url()]?.content      ?: ""
        return VionVaultEntry(title, username, password, url)
    }
}
