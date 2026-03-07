package helium314.keyboard.latin

import app.keemobile.kotpass.constants.BasicField
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.decode
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

    fun loadFromFile(file: File, passphrase: String): Result<List<VionVaultEntry>> {
        return try {
            file.inputStream().use { stream ->
                loadFromStream(stream, passphrase)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun collectEntries(group: Group, out: MutableList<VionVaultEntry>) {
        for (entry in group.entries) out.add(entry.toVionEntry())
        for (subGroup in group.groups) collectEntries(subGroup, out)
    }

    private fun Entry.toVionEntry(): VionVaultEntry {
        val title    = fields[BasicField.Title()]?.content    ?: ""
        val username = fields[BasicField.UserName()]?.content ?: ""
        val password = fields[BasicField.Password()]?.content ?: ""
        val url      = fields[BasicField.Url()]?.content      ?: ""
        return VionVaultEntry(title, username, password, url)
    }
}
