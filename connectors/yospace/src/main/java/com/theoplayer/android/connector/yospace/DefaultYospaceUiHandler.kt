package com.theoplayer.android.connector.yospace

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import com.theoplayer.android.api.THEOplayerView
import com.yospace.admanagement.LinearCreative
import com.yospace.admanagement.NonLinearCreative
import com.yospace.admanagement.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

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
    private val nonLinearContainer = adsContainer.findViewById<ViewGroup>(R.id.theo_yospace_nonlinear).apply {
        visibility = View.GONE
    }
    private val nonLinearLoadJobs: MutableMap<NonLinearCreative, NonLinearUiState> = mutableMapOf()
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun showLinearClickThrough(creative: LinearCreative, callback: YospaceClickThroughCallback) {
        linearClickThroughButton.run {
            setOnClickListener { callback.onClickThrough(context) }
            visibility = View.VISIBLE
        }
    }

    override fun hideLinearClickThrough() {
        linearClickThroughButton.run {
            setOnClickListener(null)
            visibility = View.GONE
        }
    }

    override fun showNonLinear(nonLinearCreative: NonLinearCreative, callback: YospaceClickThroughCallback) {
        val imageUrl = nonLinearCreative.getResource(Resource.ResourceType.STATIC)?.stringData ?: return
        val imageView = ImageView(parentView.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            visibility = View.GONE
            setOnClickListener { callback.onClickThrough(context) }
        }
        val loadJob = scope.launch {
            ensureActive()
            val imageBitmap = withContext(Dispatchers.IO) {
                URL(imageUrl).openStream().use { BitmapFactory.decodeStream(it) }
            }
            ensureActive()
            imageView.run {
                setImageBitmap(imageBitmap)
                visibility = View.VISIBLE
            }
        }
        nonLinearLoadJobs[nonLinearCreative] = NonLinearUiState(imageView = imageView, loadJob = loadJob)
    }

    override fun hideNonLinear(nonLinearCreative: NonLinearCreative) {
        nonLinearLoadJobs.remove(nonLinearCreative)?.let {
            it.loadJob.cancel()
            nonLinearContainer.removeView(it.imageView)
        }
    }

    override fun destroy() {
        scope.cancel()
        adsContainer.removeAllViews()
        parentView.removeView(adsContainer)
    }
}

class NonLinearUiState(
    val imageView: ImageView,
    val loadJob: Job
)