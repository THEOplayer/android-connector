package com.theoplayer.android.connector.analytics.conviva

import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.Ads
import com.theoplayer.android.api.ads.Omid
import com.theoplayer.android.api.ads.ServerSideAdIntegrationFactory
import com.theoplayer.android.api.event.EventDispatcher
import com.theoplayer.android.api.event.ads.AdEvent
import com.theoplayer.android.api.source.addescription.AdDescription

internal class FakeAds(dispatcher: TestEventDispatcher<AdEvent<*>>) :
    Ads, EventDispatcher<AdEvent<*>> by dispatcher {
    var playing = false
    var breakInProgress: AdBreak? = null
    override fun isPlaying() = playing
    override fun getCurrentAds(): List<Ad> = emptyList()
    override fun getCurrentAdBreak(): AdBreak? = breakInProgress
    override fun getScheduledAds(): List<Ad> = emptyList()
    override fun schedule(desc: AdDescription) {}
    override fun skip() {}
    override fun getOmid(): Omid = throw UnsupportedOperationException()
    override fun registerServerSideIntegration(id: String, factory: ServerSideAdIntegrationFactory) {}
}
