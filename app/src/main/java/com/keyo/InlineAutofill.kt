package com.keyo

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Size
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.common.ImageViewStyle
import androidx.autofill.inline.common.TextViewStyle
import androidx.autofill.inline.common.ViewStyle
import androidx.autofill.inline.v1.InlineSuggestionUi

/**
 * Inline autofill (Android 11+): the keyboard tells the system it can show autofill suggestions,
 * and a password manager — Bitwarden, Google Password Manager — answers with ready-made chips that
 * the keyboard places above the keys. The chips are drawn by the password manager in its own
 * process and a tap on one is handled by the system: the keyboard never sees a login or password.
 *
 * The style travels with the request so the chips match the keyboard theme.
 */
@RequiresApi(Build.VERSION_CODES.R)
internal object InlineAutofill {

    const val MAX_SUGGESTIONS = 6

    // setBackground/setPadding/setLayoutMargin are public, documented builder methods; lint flags
    // them only because they are declared on the library's restricted BaseBuilder superclass.
    @android.annotation.SuppressLint("RestrictedApi")
    fun request(context: Context, theme: KeyboardPrefs.KeyboardTheme): InlineSuggestionsRequest {
        val d = context.resources.displayMetrics.density
        val heightPx = (32 * d).toInt()
        val pad = (10 * d).toInt()
        val chipBg = Icon.createWithResource(context, R.drawable.inline_chip_bg).setTint(theme.key.toInt())
        val chip = ViewStyle.Builder()
            .setBackground(chipBg)
            .setPadding(pad, 0, pad, 0)
            .build()
        val style = InlineSuggestionUi.newStyleBuilder()
            .setSingleIconChipStyle(chip)
            .setChipStyle(chip)
            .setStartIconStyle(ImageViewStyle.Builder().setLayoutMargin(0, 0, pad / 2, 0).build())
            .setTitleStyle(TextViewStyle.Builder().setTextColor(theme.text.toInt()).setTextSize(14f).build())
            .setSubtitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(pad / 2, 0, 0, 0)
                    .setTextColor((theme.text.toInt() and 0x00FFFFFF) or 0x99000000.toInt())
                    .setTextSize(12f)
                    .build()
            )
            .build()
        val styles = UiVersions.newStylesBuilder().addStyle(style).build()
        val spec = InlinePresentationSpec.Builder(
            Size((48 * d).toInt(), heightPx),
            Size(context.resources.displayMetrics.widthPixels, heightPx)
        ).setStyle(styles).build()
        return InlineSuggestionsRequest.Builder(listOf(spec))
            .setMaxSuggestionCount(MAX_SUGGESTIONS)
            .build()
    }
}
