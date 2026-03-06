/*
 * VionBoard — VionShortcutExpander.kt
 * Personal shortcuts expander. Maps trigger words to full expansions.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package helium314.keyboard.latin

import android.content.Context
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.database.ShortcutsDao
import helium314.keyboard.latin.dictionary.Dictionary

/**
 * VionBoard shortcut expander.
 *
 * When the user types a trigger word exactly matching a saved shortcut,
 * the expanded text is injected as the top suggestion in the strip.
 * Tapping it replaces the trigger with the full expansion.
 *
 * Examples:
 *   "omw"  → "On my way!"
 *   "addr" → "123 Main Street, Springfield"
 *   "ty"   → "Thank you for your message."
 *
 * Shortcuts are stored in the local SQLite database via ShortcutsDao.
 * No network, no cloud — fully offline and private.
 */
object VionShortcutExpander {

    private var dao: ShortcutsDao? = null

    /** Must be called once on keyboard init (LatinIME.onCreate). */
    fun init(context: Context) {
        dao = ShortcutsDao.getInstance(context)
    }

    /**
     * Returns a SuggestedWordInfo for the expansion if the typed word
     * exactly matches a shortcut trigger, or null otherwise.
     *
     * The result uses KIND_COMPLETION so it shows distinctly in the strip
     * and does not auto-correct (user must tap it deliberately).
     */
    fun getShortcutSuggestion(typedWord: String): SuggestedWordInfo? {
        if (typedWord.length < 2) return null // no single-char triggers
        val d = dao ?: return null
        val expansion = d.findExpansion(typedWord) ?: return null
        return SuggestedWordInfo(
            expansion,
            "",
            SuggestedWordInfo.MAX_SCORE - 50, // just below typed word score
            SuggestedWordInfo.KIND_COMPLETION,
            Dictionary.DICTIONARY_USER_TYPED,
            SuggestedWordInfo.NOT_AN_INDEX,
            SuggestedWordInfo.NOT_A_CONFIDENCE
        )
    }

    /**
     * Injects a shortcut suggestion at the front of the suggestions list
     * when a trigger match is found. Does nothing if no match.
     *
     * Injection point: called from Suggest.kt after typed word is added
     * and before the list is finalised.
     */
    fun injectShortcutSuggestion(
        typedWord: String,
        suggestions: ArrayList<SuggestedWordInfo>
    ) {
        val suggestion = getShortcutSuggestion(typedWord) ?: return
        // Insert at position 1 (after typed word at 0), pushing other suggestions down
        val insertAt = minOf(1, suggestions.size)
        suggestions.add(insertAt, suggestion)
    }

    // --- Convenience write methods (called from settings UI) ---

    /** Adds or updates a shortcut. Trigger is case-insensitive. */
    fun addShortcut(trigger: String, expansion: String) {
        dao?.upsert(trigger, expansion)
    }

    /** Removes a shortcut by trigger word. */
    fun removeShortcut(trigger: String) {
        dao?.delete(trigger)
    }

    /** Returns all shortcuts sorted by trigger, for display in settings. */
    fun getAllShortcuts() = dao?.getAll() ?: emptyList()
}
