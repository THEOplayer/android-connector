package com.theoplayer.android.connector.yospace

import android.app.Activity
import android.content.Context
import com.theoplayer.android.api.THEOplayerView
import com.yospace.admanagement.LinearCreative
import com.yospace.admanagement.NonLinearCreative

/**
 * A callback that should be called when a linear or non-linear creative is clicked.
 *
 * @see YospaceUiHandler.showLinearClickThrough
 * @see YospaceUiHandler.hideLinearClickThrough
 */
fun interface YospaceClickThroughCallback {
    /**
     * Called when the creative is clicked.
     *
     * @param context The context of the calling [Activity].
     */
    fun onClickThrough(context: Context)
}

/**
 * A UI handler for the Yospace connector.
 *
 * This handler is called whenever a linear clickthrough or non-linear ad needs to be shown
 * to the user.
 *
 * All methods are always called from the main thread.
 */
interface YospaceUiHandler {
    /**
     * Called when a click-through button for the given linear creative should be shown.
     *
     * @param linearCreative The linear creative.
     * @param callback The callback to call when the click-through button is clicked.
     */
    fun showLinearClickThrough(linearCreative: LinearCreative, callback: YospaceClickThroughCallback)

    /**
     * Called when the click-through button for the current linear creative should be removed.
     */
    fun hideLinearClickThrough()

    /**
     * Called when the given non-linear creative should be shown.
     *
     * @param nonLinearCreative The non-linear creative.
     * @param callback The callback to call when the non-linear creative is clicked.
     */
    fun showNonLinear(nonLinearCreative: NonLinearCreative, callback: YospaceClickThroughCallback)

    /**
     * Called when the given non-linear creative should be removed.
     */
    fun hideNonLinear(nonLinearCreative: NonLinearCreative)

    /**
     * Called when the player is [destroyed][THEOplayerView.isDestroyed],
     * to clean up any created resources.
     */
    fun destroy()
}