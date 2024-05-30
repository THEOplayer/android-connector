package com.theoplayer.android.connector.yospace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.theoplayer.android.api.THEOplayerView
import com.yospace.admanagement.LinearCreative

class DefaultYospaceUiHandler(
    theoplayerView: THEOplayerView
) : YospaceUiHandler {
    private val parentView: ViewGroup = theoplayerView.findViewById(R.id.theo_ads_container)
    private val adsContainer: ViewGroup = (LayoutInflater.from(parentView.context).inflate(R.layout.ads, parentView, false) as ViewGroup).also {
        parentView.addView(it)
    }
    private val linearClickThroughButton = adsContainer.findViewById<Button>(R.id.theo_yospace_linear_clickthrough).apply {
        visibility = View.GONE
    }

    override fun showLinearClickThrough(creative: LinearCreative, callback: YospaceClickThroughCallback) {
        linearClickThroughButton.setOnClickListener { callback.onClickThrough(parentView.context) }
        linearClickThroughButton.visibility = View.VISIBLE
    }

    override fun hideLinearClickThrough() {
        linearClickThroughButton.setOnClickListener(null)
        linearClickThroughButton.visibility = View.GONE
    }

    override fun destroy() {
        adsContainer.removeAllViews()
        parentView.removeView(adsContainer)
    }
}