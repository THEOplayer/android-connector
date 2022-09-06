package com.theoplayer.android.connector

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.TypedSource

class MainActivity : AppCompatActivity() {

    private lateinit var theoplayerView: THEOplayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupTHEOplayer()
    }

    private fun setupTHEOplayer() {
        val theoplayerConfig = THEOplayerConfig.Builder()
            .build()
        theoplayerView = THEOplayerView(this, theoplayerConfig)
        theoplayerView.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        val tpvContainer = findViewById<FrameLayout>(R.id.tpv_container)
        tpvContainer.addView(theoplayerView)
    }

    fun setSource(view: View) {
        theoplayerView.player.source = SourceDescription.Builder(
            TypedSource.Builder("https://cdn.theoplayer.com/video/big_buck_bunny/big_buck_bunny.m3u8")
                .build()
        )
        .build()
    }

    fun playPause(view: View) {
        if (theoplayerView.player.isPaused) {
            theoplayerView.player.play()
        } else {
            theoplayerView.player.pause()
        }
    }

    fun seekBackward(view: View) {
        theoplayerView.player.currentTime = theoplayerView.player.currentTime - 10
    }

    fun seekForward(view: View) {
        theoplayerView.player.currentTime = theoplayerView.player.currentTime + 10
    }

}