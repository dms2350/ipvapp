package com.iptv.presentation.ui.player

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.media3.ui.PlayerView

class CustomPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    var onSeekForward: (() -> Unit)? = null
    var onSeekBackward: (() -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    onSeekForward?.invoke()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    onSeekBackward?.invoke()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}