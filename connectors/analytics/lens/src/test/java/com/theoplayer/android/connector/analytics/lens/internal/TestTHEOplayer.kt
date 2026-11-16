package com.theoplayer.android.connector.analytics.lens.internal

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.abr.Abr
import com.theoplayer.android.api.event.Event
import com.theoplayer.android.api.event.EventDispatcher
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.EventType
import com.theoplayer.android.api.event.player.CurrentSourceChangeEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.SourceChangeEvent
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.PreloadType
import com.theoplayer.android.api.player.PresentationMode
import com.theoplayer.android.api.player.track.mediatrack.MediaTrack
import com.theoplayer.android.api.player.track.mediatrack.MediaTrackList
import com.theoplayer.android.api.player.track.mediatrack.quality.AudioQuality
import com.theoplayer.android.api.player.track.mediatrack.quality.Quality
import com.theoplayer.android.api.player.track.mediatrack.quality.QualityList
import com.theoplayer.android.api.player.track.mediatrack.quality.VideoQuality
import com.theoplayer.android.api.player.track.texttrack.TextTrack
import com.theoplayer.android.api.player.track.texttrack.TextTrackList
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.timerange.TimeRanges
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RuntimeEnvironment

/** Keeps the listeners added to THEOplayer's dispatchers, so a test can fire their events. */
internal class Dispatchers {
    val listeners = mutableMapOf<EventType<*>, MutableList<EventListener<*>>>()

    val count get() = listeners.values.sumOf { it.size }

    fun <E : Event<*>> attach(target: EventDispatcher<E>) {
        doAnswer { listeners.getOrPut(it.getArgument(0)) { mutableListOf() } += it.getArgument<EventListener<*>>(1) }
            .whenever(target).addEventListener(any<EventType<E>>(), any<EventListener<E>>())
        doAnswer { listeners[it.getArgument(0)]?.remove(it.getArgument<EventListener<*>>(1)) }
            .whenever(target).removeEventListener(any<EventType<E>>(), any<EventListener<E>>())
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Event<*>> fire(type: EventType<T>, event: T = mock()) {
        listeners[type]?.toList()?.forEach { (it as EventListener<T>).handleEvent(event) }
    }
}

internal fun timeRanges(vararg ranges: Pair<Double, Double>): TimeRanges = mock {
    on { length() } doReturn ranges.size
    on { getStart(any<Int>()) } doAnswer { ranges[it.getArgument(0)].first }
    on { getEnd(any<Int>()) } doAnswer { ranges[it.getArgument(0)].second }
}

internal fun <Q : Quality> qualityList(vararg items: Q): QualityList<Q> = mock { on { iterator() } doAnswer { items.toMutableList().iterator() } }

private fun <Q : Quality> mediaTrackList(dispatchers: Dispatchers, items: () -> List<MediaTrack<Q>>): MediaTrackList<Q> =
    mock<MediaTrackList<Q>> { on { iterator() } doAnswer { items().toMutableList().iterator() } }.also { dispatchers.attach(it) }

/** The shape of THEOplayer's `Cmcd`, which the THEOplayer the connector compiles against does not have yet. */
internal interface TestCmcd {
    var linkedSessionId: String?
}

/** A THEOplayer `Player` with `getCmcd()`, as newer THEOplayer versions have it. */
internal interface PlayerWithCmcd : Player {
    fun getCmcd(): TestCmcd
}

/** A mocked THEOplayer view and player, idle and without a source; [withCmcd] gives the player `getCmcd()`. */
internal class TestTHEOplayer(withCmcd: Boolean = true) {
    val dispatchers = Dispatchers()
    val cmcd = object : TestCmcd {
        override var linkedSessionId: String? = null
    }
    val player: Player = if (withCmcd) mock<PlayerWithCmcd>().also { doReturn(cmcd).whenever(it).getCmcd() } else mock()
    val view: THEOplayerView = mock()

    var video = emptyList<MediaTrack<VideoQuality>>()
    var audio = emptyList<MediaTrack<AudioQuality>>()

    init {
        dispatchers.attach(player)
        whenever(view.player).thenReturn(player)
        whenever(view.version).thenReturn("11.15.0")
        whenever(view.presentationMode).thenReturn(PresentationMode.INLINE)
        whenever(view.resources).thenReturn(RuntimeEnvironment.getApplication().resources)
        doReturn(mediaTrackList(dispatchers) { video }).whenever(player).videoTracks
        doReturn(mediaTrackList(dispatchers) { audio }).whenever(player).audioTracks
        doReturn(mock<TextTrackList> { on { iterator() } doAnswer { mutableListOf<TextTrack>().iterator() } }.also { dispatchers.attach(it) })
            .whenever(player).textTracks
        doReturn(mock<Abr>()).whenever(player).abr
        whenever(player.duration).thenReturn(Double.NaN)
        doReturn(timeRanges()).whenever(player).buffered
        doReturn(timeRanges()).whenever(player).seekable
        whenever(player.playbackRate).thenReturn(1.0)
        whenever(player.volume).thenReturn(1.0)
        whenever(player.isPaused).thenReturn(true)
        whenever(player.preload).thenReturn(PreloadType.AUTO)
    }

    /** Sets a source as THEOplayer does: `player.source` reads null until `sourcechange`. */
    fun setSource(description: SourceDescription?) {
        whenever(player.source).thenReturn(null)
        dispatchers.fire(PlayerEventTypes.CURRENTSOURCECHANGE, mock<CurrentSourceChangeEvent> { on { currentSource } doReturn description?.sources?.first() })
        dispatchers.fire(PlayerEventTypes.SOURCECHANGE, mock<SourceChangeEvent> { on { source } doReturn description })
        whenever(player.source).thenReturn(description)
    }
}
