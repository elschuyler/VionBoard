/*
 * Copyright (C) 2021 Patrick Goldinger
 * modified
 * SPDX-License-Identifier: Apache-2.0
 */
package helium314.keyboard.keyboard.internal.keyboard_parser.floris

import android.view.KeyEvent

// taken from FlorisBoard and modified
object KeyCode {
    object Spec {
        const val CHARACTERS_MIN = 1
        const val CHARACTERS_MAX = 65535
        val CHARACTERS = CHARACTERS_MIN..CHARACTERS_MAX

        const val INTERNAL_FLORIS_MIN = -9999
        const val INTERNAL_FLORIS_MAX = -1
        val INTERNAL_FLORIS = INTERNAL_FLORIS_MIN..INTERNAL_FLORIS_MAX // do NOT add key codes in this range
        val INTERNAL_HELI = -19999..-10000 // for keys exclusive to this app
        val CURRENCY = CURRENCY_SLOT_6..CURRENCY_SLOT_1
    }

    const val UNSPECIFIED =                    0

    const val CTRL =                          -1
    const val CTRL_LOCK =                     -2
    const val ALT =                           -3
    const val ALT_LOCK =                      -4
    const val FN =                            -5
    const val FN_LOCK =                       -6
    const val DELETE =                        -7
    const val SHIFT =                        -11
    const val CAPS_LOCK =                    -13

    const val ARROW_LEFT =                   -21
    const val ARROW_RIGHT =                  -22
    const val ARROW_UP =                     -23
    const val ARROW_DOWN =                   -24
    const val MOVE_START_OF_PAGE =           -25
    const val MOVE_END_OF_PAGE =             -26
    const val MOVE_START_OF_LINE =           -27
    const val MOVE_END_OF_LINE =             -28

    const val CLIPBOARD_COPY =               -31
    const val CLIPBOARD_CUT =                -32
    const val CLIPBOARD_PASTE =              -33
    const val CLIPBOARD_SELECT_WORD =        -34
    const val CLIPBOARD_SELECT_ALL =         -35
    const val CLIPBOARD_CLEAR_HISTORY =      -36

    const val SPLIT_LAYOUT =                -113

    const val UNDO =                        -131
    const val REDO =                        -132

    const val ALPHA =                       -201
    const val SYMBOL =                      -202
    const val NUMPAD =                      -205
    const val VIEW_PHONE =                  -206
    const val VIEW_PHONE2 =                 -207

    const val IME_UI_MODE_TEXT =            -211
    const val EMOJI =                       -212
    const val CLIPBOARD =                   -213

    const val LANGUAGE_SWITCH =             -227
    const val IME_HIDE_UI =                 -232
    const val VOICE_INPUT =                 -233

    const val TOGGLE_INCOGNITO_MODE =       -244
    const val TOGGLE_AUTOCORRECT =          -245

    const val URI_COMPONENT_TLD =           -255

    const val SETTINGS =                    -301

    const val CURRENCY_SLOT_1 =             -801
    const val CURRENCY_SLOT_2 =             -802
    const val CURRENCY_SLOT_3 =             -803
    const val CURRENCY_SLOT_4 =             -804
    const val CURRENCY_SLOT_5 =             -805
    const val CURRENCY_SLOT_6 =             -806

    const val MULTIPLE_CODE_POINTS =        -902

    const val KESHIDA =                     1600
    const val ZWNJ =                        8204
    const val ZWJ =                         8205

    const val CJK_SPACE =                  12288

    // heliboard only codes
    const val SYMBOL_ALPHA =              -10001
    const val TOGGLE_ONE_HANDED_MODE =    -10002
    const val TOGGLE_ONE_HANDED_MODE_2 =  -10003
    const val SWITCH_ONE_HANDED_MODE =    -10004
    const val SHIFT_ENTER =               -10005
    const val ACTION_NEXT =               -10006
    const val ACTION_PREVIOUS =           -10007
    const val NOT_SPECIFIED =             -10008
    const val CLIPBOARD_COPY_ALL =        -10009
    const val PAGE_UP =                   -10010
    const val PAGE_DOWN =                 -10011
    const val META =                      -10012
    const val META_LOCK =                 -10013
    const val TAB =                       -10014
    const val WORD_LEFT =                 -10015
    const val WORD_RIGHT =                -10016
    const val ESCAPE =                    -10017
    const val INSERT =                    -10018
    const val SLEEP =                     -10019
    const val MEDIA_PLAY =                -10020
    const val MEDIA_PAUSE =               -10021
    const val MEDIA_PLAY_PAUSE =          -10022
    const val MEDIA_NEXT =                -10023
    const val MEDIA_PREVIOUS =            -10024
    const val VOL_UP =                    -10025
    const val VOL_DOWN =                  -10026
    const val MUTE =                      -10027
    const val F1 =                        -10028
    const val F2 =                        -10029
    const val F3 =                        -10030
    const val F4 =                        -10031
    const val F5 =                        -10032
    const val F6 =                        -10033
    const val F7 =                        -10034
    const val F8 =                        -10035
    const val F9 =                        -10036
    const val F10 =                       -10037
    const val F11 =                       -10038
    const val F12 =                       -10039
    const val BACK =                      -10040
    const val TIMESTAMP =                 -10043
    const val CTRL_LEFT =                 -10044
    const val CTRL_RIGHT =                -10045
    const val ALT_LEFT =                  -10046
    const val ALT_RIGHT =                 -10047
    const val META_LEFT =                 -10048
    const val META_RIGHT =                -10049
    const val EMOJI_SEARCH =              -10050
    const val INLINE_EMOJI_SEARCH_DONE =  -10051
    // VionBoard: opens the KeePass vault panel
    const val VION_VAULT =                -10052

    // Intents
    const val SEND_INTENT_ONE =            -20000
    const val SEND_INTENT_TWO =            -20001
    const val SEND_INTENT_THREE =          -20002

    /** to make sure a FlorisBoard code works when reading a JSON layout */
    fun Int.checkAndConvertCode(): Int = if (this > 0) this else when (this) {
        // working
        CURRENCY_SLOT_1, CURRENCY_SLOT_2, CURRENCY_SLOT_3, CURRENCY_SLOT_4, CURRENCY_SLOT_5, CURRENCY_SLOT_6,
        VOICE_INPUT, LANGUAGE_SWITCH, SETTINGS, DELETE, ALPHA, SYMBOL, EMOJI, CLIPBOARD, CLIPBOARD_CUT, UNDO,
        REDO, ARROW_DOWN, ARROW_UP, ARROW_RIGHT, ARROW_LEFT, CLIPBOARD_COPY, CLIPBOARD_PASTE, CLIPBOARD_SELECT_ALL,
        CLIPBOARD_SELECT_WORD, TOGGLE_INCOGNITO_MODE, TOGGLE_AUTOCORRECT, MOVE_START_OF_LINE, MOVE_END_OF_LINE,
        MOVE_START_OF_PAGE, MOVE_END_OF_PAGE, SHIFT, CAPS_LOCK, MULTIPLE_CODE_POINTS, UNSPECIFIED, CTRL, ALT,
        FN, CLIPBOARD_CLEAR_HISTORY, NUMPAD, IME_HIDE_UI, CTRL_LOCK, ALT_LOCK, FN_LOCK,

        // heliboard only
        SYMBOL_ALPHA, TOGGLE_ONE_HANDED_MODE, SWITCH_ONE_HANDED_MODE, SPLIT_LAYOUT, SHIFT_ENTER,
        ACTION_NEXT, ACTION_PREVIOUS, NOT_SPECIFIED, CLIPBOARD_COPY_ALL, WORD_LEFT, WORD_RIGHT, PAGE_UP,
        PAGE_DOWN, META, TAB, ESCAPE, INSERT, SLEEP, MEDIA_PLAY, MEDIA_PAUSE, MEDIA_PLAY_PAUSE, MEDIA_NEXT,
        MEDIA_PREVIOUS, VOL_UP, VOL_DOWN, MUTE, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, BACK,
        TIMESTAMP, CTRL_LEFT, CTRL_RIGHT, ALT_LEFT, ALT_RIGHT, META_LEFT, META_RIGHT, SEND_INTENT_ONE,
        SEND_INTENT_TWO, SEND_INTENT_THREE, EMOJI_SEARCH, INLINE_EMOJI_SEARCH_DONE, META_LOCK,
        // VionBoard
        VION_VAULT
        -> this

        // conversion
        IME_UI_MODE_TEXT -> ALPHA
        VIEW_PHONE -> ALPHA
        VIEW_PHONE2 -> SYMBOL
        TOGGLE_ONE_HANDED_MODE_2 -> TOGGLE_ONE_HANDED_MODE

        else -> throw IllegalStateException("key code $this not yet supported")
    }

    fun Int.isModifier() = when (this) {
        SHIFT, SYMBOL_ALPHA, ALPHA, SYMBOL, NUMPAD, FN, CTRL, CTRL_LEFT, CTRL_RIGHT, CTRL_LOCK, ALT, ALT_LEFT,
        ALT_RIGHT, ALT_LOCK, META, META_LEFT, META_RIGHT, META_LOCK, -> true
        else -> false
    }

    @JvmStatic fun keyCodeToKeyEventCode(keyCode: Int) = when (keyCode) {
        ARROW_UP -> KeyEvent.KEYCODE_DPAD_UP
        ARROW_RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
        ARROW_DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
        ARROW_LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
        MOVE_START_OF_LINE -> KeyEvent.KEYCODE_MOVE_HOME
        MOVE_END_OF_LINE -> KeyEvent.KEYCODE_MOVE_END
        TAB -> KeyEvent.KEYCODE_TAB
        PAGE_UP -> KeyEvent.KEYCODE_PAGE_UP
        PAGE_DOWN -> KeyEvent.KEYCODE_PAGE_DOWN
        ESCAPE -> KeyEvent.KEYCODE_ESCAPE
        INSERT -> KeyEvent.KEYCODE_INSERT
        SLEEP -> KeyEvent.KEYCODE_SLEEP
        MEDIA_PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
        MEDIA_PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
        MEDIA_PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        MEDIA_NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
        MEDIA_PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        VOL_UP -> KeyEvent.KEYCODE_VOLUME_UP
        VOL_DOWN -> KeyEvent.KEYCODE_VOLUME_DOWN
        MUTE -> KeyEvent.KEYCODE_VOLUME_MUTE
        BACK -> KeyEvent.KEYCODE_BACK
        F1 -> KeyEvent.KEYCODE_F1
        F2 -> KeyEvent.KEYCODE_F2
        F3 -> KeyEvent.KEYCODE_F3
        F4 -> KeyEvent.KEYCODE_F4
        F5 -> KeyEvent.KEYCODE_F5
        F6 -> KeyEvent.KEYCODE_F6
        F7 -> KeyEvent.KEYCODE_F7
        F8 -> KeyEvent.KEYCODE_F8
        F9 -> KeyEvent.KEYCODE_F9
        F10 -> KeyEvent.KEYCODE_F10
        F11 -> KeyEvent.KEYCODE_F11
        F12 -> KeyEvent.KEYCODE_F12
        else -> if (keyCode < 0) KeyEvent.KEYCODE_UNKNOWN else keyCode
    }

    @JvmStatic fun codePointToKeyEventCode(codePoint: Int): Int = when (codePoint.toChar().uppercaseChar()) {
        '/' -> KeyEvent.KEYCODE_SLASH
        '\\' -> KeyEvent.KEYCODE_BACKSLASH
        ';' -> KeyEvent.KEYCODE_SEMICOLON
        ',' -> KeyEvent.KEYCODE_COMMA
        '.' -> KeyEvent.KEYCODE_PERIOD
        '\'' -> KeyEvent.KEYCODE_APOSTROPHE
        '`' -> KeyEvent.KEYCODE_GRAVE
        '*' -> KeyEvent.KEYCODE_STAR
        ']' -> KeyEvent.KEYCODE_RIGHT_BRACKET
        '[' -> KeyEvent.KEYCODE_LEFT_BRACKET
        ')' -> KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN
        '(' -> KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN
        '+' -> KeyEvent.KEYCODE_PLUS
        '-' -> KeyEvent.KEYCODE_MINUS
        '=' -> KeyEvent.KEYCODE_EQUALS
        '@' -> KeyEvent.KEYCODE_AT
        '#' -> KeyEvent.KEYCODE_POUND
        '\n' -> KeyEvent.KEYCODE_ENTER
        '\t' -> KeyEvent.KEYCODE_TAB
        ' ' -> KeyEvent.KEYCODE_SPACE
        '0' -> KeyEvent.KEYCODE_0
        '1' -> KeyEvent.KEYCODE_1
        '2' -> KeyEvent.KEYCODE_2
        '3' -> KeyEvent.KEYCODE_3
        '4' -> KeyEvent.KEYCODE_4
        '5' -> KeyEvent.KEYCODE_5
        '6' -> KeyEvent.KEYCODE_6
        '7' -> KeyEvent.KEYCODE_7
        '8' -> KeyEvent.KEYCODE_8
        '9' -> KeyEvent.KEYCODE_9
        'A' -> KeyEvent.KEYCODE_A
        'B' -> KeyEvent.KEYCODE_B
        'C' -> KeyEvent.KEYCODE_C
        'D' -> KeyEvent.KEYCODE_D
        'E' -> KeyEvent.KEYCODE_E
        'F' -> KeyEvent.KEYCODE_F
        'G' -> KeyEvent.KEYCODE_G
        'H' -> KeyEvent.KEYCODE_H
        'I' -> KeyEvent.KEYCODE_I
        'J' -> KeyEvent.KEYCODE_J
        'K' -> KeyEvent.KEYCODE_K
        'L' -> KeyEvent.KEYCODE_L
        'M' -> KeyEvent.KEYCODE_M
        'N' -> KeyEvent.KEYCODE_N
        'O' -> KeyEvent.KEYCODE_O
        'P' -> KeyEvent.KEYCODE_P
        'Q' -> KeyEvent.KEYCODE_Q
        'R' -> KeyEvent.KEYCODE_R
        'S' -> KeyEvent.KEYCODE_S
        'T' -> KeyEvent.KEYCODE_T
        'U' -> KeyEvent.KEYCODE_U
        'V' -> KeyEvent.KEYCODE_V
        'W' -> KeyEvent.KEYCODE_W
        'X' -> KeyEvent.KEYCODE_X
        'Y' -> KeyEvent.KEYCODE_Y
        'Z' -> KeyEvent.KEYCODE_Z
        else -> KeyEvent.KEYCODE_UNKNOWN
    }
}
