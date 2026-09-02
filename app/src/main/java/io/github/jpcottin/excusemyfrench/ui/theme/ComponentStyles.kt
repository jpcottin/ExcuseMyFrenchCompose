package io.github.jpcottin.excusemyfrench.ui.theme

import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.contentPadding
import androidx.compose.foundation.style.fillSize
import androidx.compose.ui.unit.dp

/**
 * Default [Style]s for the app's custom components (Compose Styles API).
 *
 * Material 3 components keep their own theming; only the app's own composables
 * read these. Callers can override any property by passing a [Style] to the
 * component, which is merged after the defaults defined here.
 */
object ComponentStyles {

    /** Root insult screen: fills the window and insets all content by the screen margin. */
    val screen = Style {
        fillSize()
        contentPadding(16.dp)
    }

    /** Insult/placeholder image: occupies at most 90% of the media area in each dimension. */
    val mediaImage = Style {
        width(0.9f)
        height(0.9f)
    }
}
