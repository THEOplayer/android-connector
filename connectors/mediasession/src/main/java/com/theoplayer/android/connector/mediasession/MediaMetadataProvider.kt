package com.theoplayer.android.connector.mediasession

import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.theoplayer.android.api.source.SourceDescription

private const val PROP_ALBUM = "album"
private const val PROP_ALBUM_ART = "albumArt"
private const val PROP_ALBUM_ARTIST = "albumArtist"
private const val PROP_ALBUM_ART_URI = "albumArtUri"
private const val PROP_ART = "art"
private const val PROP_ART_URI = "artUri"
private const val PROP_ARTIST = "artist"
private const val PROP_AUTHOR = "author"
private const val PROP_COMPILATION = "compilation"
private const val PROP_COMPOSER = "composer"
private const val PROP_DATE = "date"
private const val PROP_DISC_NUMBER = "discNumber"
private const val PROP_DISPLAY_DESCRIPTION = "displayDescription"
private const val PROP_DISPLAY_ICON = "displayIcon"
private const val PROP_DISPLAY_ICON_URI = "displayIconUri"
private const val PROP_DISPLAY_SUBTITLE = "displaySubtitle"
private const val PROP_DISPLAY_TITLE = "displayTitle"
private const val PROP_DOWNLOAD_STATUS = "downloadStatus"
private const val PROP_GENRE = "genre"
private const val PROP_MEDIA_ID = "mediaId"
private const val PROP_MEDIA_URI = "mediaUri"
private const val PROP_NUM_TRACKS = "numTracks"
private const val PROP_TITLE = "title"
private const val PROP_TRACK_NUMBER = "trackNumber"
private const val PROP_WRITER = "writer"
private const val PROP_YEAR = "year"

@Suppress("MemberVisibilityCanBePrivate")
class MediaMetadataProvider(private val connector: MediaSessionConnector) {
    companion object {
        private val METADATA_EMPTY = MediaMetadataCompat.Builder().build()
    }

    private var builder = MediaMetadataCompat.Builder()

    /**
     * Update metadata from [SourceDescription].
     */
    fun setMediaSessionMetadata(sourceDescription: SourceDescription?) {
        clearMediaSessionMetadataDescription()
        if (sourceDescription != null) {
            updateMetaDataDescription(sourceDescription)
            invalidateMediaSessionMetadata()
        }
    }

    /**
     * Get the current metadata object.
     */
    fun getMediaSessionMetadata(): MediaMetadataCompat {
        return builder.build()
    }

    /**
     * Update the mediaSession with the currently set metadata.
     */
    fun invalidateMediaSessionMetadata() {
        if (connector.debug) {
            Log.d(TAG, "MediaMetadataProvider::invalidateMediaSessionMetadata")
        }
        val player = connector.player
        if (player == null) {
            clearMediaSessionMetadataDescription()
            return
        }
        setAdvertisement(player.ads.isPlaying)
        if (!java.lang.Double.isNaN(player.duration)) {
            setDuration((1e03 * player.duration).toLong())
        }
        try {
            connector.mediaSession.setMetadata(getMediaSessionMetadata())
        } catch(e: IllegalStateException) {
            Log.e(TAG, "Failed to set metadata: ${e.message}")
        }
    }

    /**
     * Clear the current metadata set.
     */
    fun clearMediaSessionMetadataDescription() {
        if (connector.debug) {
            Log.d(TAG, "MediaMetadataProvider::clearMediaSessionMetadataDescription")
        }
        builder = MediaMetadataCompat.Builder()
        connector.mediaSession.setMetadata(METADATA_EMPTY)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT].
     */
    fun setAdvertisement(value: Boolean?) {
        builder.putLong(MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT, if (value == true) 1 else 0)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_ALBUM].
     */
    fun setAlbum(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_ALBUM_ART].
     */
    fun setAlbumArt(value: Bitmap?) {
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, makeSafeBitmapCopy(value))
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST].
     */
    fun setAlbumArtist(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI].
     */
    fun setAlbumArtUri(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_ART].
     */
    fun setArt(value: Bitmap?) {
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, makeSafeBitmapCopy(value))
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_ART_URI].
     */
    fun setArtUri(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_ARTIST].
     */
    fun setArtist(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_AUTHOR].
     */
    fun setAuthor(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_COMPILATION].
     */
    fun setCompilation(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_COMPILATION, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_COMPOSER].
     */
    fun setComposer(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_DATE].
     */
    fun setDate(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_DATE, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_DISC_NUMBER].
     */
    fun setDiscNumber(value: Long?) {
        value?.let {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, it)
        }
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION].
     */
    fun setDisplayDescription(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON].
     */
    fun setDisplayIcon(value: Bitmap?) {
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, makeSafeBitmapCopy(value))
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI].
     */
    fun setDisplayIconUri(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE].
     */
    fun setDisplaySubtitle(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE].
     */
    fun setDisplayTitle(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS].
     */
    fun setDownloadStatus(value: Long?) {
        value?.let {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS, it)
        }
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_DURATION].
     */
    fun setDuration(value: Long?) {
        value?.let {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it)
        }
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI].
     */
    fun setIconUri(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_GENRE].
     */
    fun setGenre(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_GENRE, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_MEDIA_ID].
     */
    fun setMediaId(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_MEDIA_URI].
     */
    fun setMediaUri(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_NUM_TRACKS].
     */
    fun setNumberOfTracks(value: Long?) {
        value?.let {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, it)
        }
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_TITLE].
     */
    fun setTitle(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER].
     */
    fun setTrackNumber(value: Long?) {
        value?.let {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, it)
        }
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_WRITER].
     */
    fun setWriter(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_WRITER, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_YEAR].
     */
    fun setYear(value: Long?) {
        value?.let {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_YEAR, it)
        }
    }

    private fun updateMetaDataDescription(sourceDescription: SourceDescription?) {
        if (sourceDescription == null) {
            connector.mediaSession.setMetadata(METADATA_EMPTY)
            return
        }
        sourceDescription.poster?.let {
            setIconUri(it)
        }
        sourceDescription.metadata?.let { metadata ->
            if (metadata.containsKey(PROP_ALBUM)) {
                setAlbum(metadata.get(PROP_ALBUM) as? String)
            }
            if (metadata.containsKey(PROP_ALBUM_ART)) {
                setAlbumArt(metadata.get(PROP_ALBUM_ART) as? Bitmap)
            }
            if (metadata.containsKey(PROP_ALBUM_ARTIST)) {
                setAlbumArtist(metadata.get(PROP_ALBUM_ARTIST) as? String)
            }
            if (metadata.containsKey(PROP_ALBUM_ART_URI)) {
                setAlbumArtUri(metadata.get(PROP_ALBUM_ART_URI) as? String)
            }
            if (metadata.containsKey(PROP_ART)) {
                setArt(metadata.get(PROP_ART) as? Bitmap)
            }
            if (metadata.containsKey(PROP_ART_URI)) {
                setArtUri(metadata.get(PROP_ART_URI) as? String)
            }
            if (metadata.containsKey(PROP_ARTIST)) {
                setArtist(metadata.get(PROP_ARTIST) as? String)
            }
            if (metadata.containsKey(PROP_AUTHOR)) {
                setAuthor(metadata.get(PROP_AUTHOR) as? String)
            }
            if (metadata.containsKey(PROP_COMPILATION)) {
                setCompilation(metadata.get(PROP_COMPILATION) as? String)
            }
            if (metadata.containsKey(PROP_COMPOSER)) {
                setComposer(metadata.get(PROP_COMPOSER) as? String)
            }
            if (metadata.containsKey(PROP_DATE)) {
                setDate(metadata.get(PROP_DATE) as? String)
            }
            if (metadata.containsKey(PROP_DISC_NUMBER)) {
                setDiscNumber(metadata.get(PROP_DISC_NUMBER) as? Long)
            }
            if (metadata.containsKey(PROP_DISPLAY_DESCRIPTION)) {
                setDisplayDescription(metadata.get(PROP_DISPLAY_DESCRIPTION) as? String)
            }
            if (metadata.containsKey(PROP_DISPLAY_ICON)) {
                setDisplayIcon(metadata.get(PROP_DISPLAY_ICON) as? Bitmap)
            }
            if (metadata.containsKey(PROP_DISPLAY_ICON_URI)) {
                setDisplayIconUri(metadata.get(PROP_DISPLAY_ICON_URI) as? String)
            }
            if (metadata.containsKey(PROP_DISPLAY_SUBTITLE)) {
                setDisplaySubtitle(metadata.get(PROP_DISPLAY_SUBTITLE) as? String)
            }
            if (metadata.containsKey(PROP_DISPLAY_TITLE)) {
                setDisplayTitle(metadata.get(PROP_DISPLAY_TITLE) as? String)
            }
            if (metadata.containsKey(PROP_DOWNLOAD_STATUS)) {
                setDownloadStatus(metadata.get(PROP_DOWNLOAD_STATUS) as? Long)
            }
            if (metadata.containsKey(PROP_GENRE)) {
                setGenre(metadata.get(PROP_GENRE) as? String)
            }
            if (metadata.containsKey(PROP_MEDIA_ID)) {
                setMediaId(metadata.get(PROP_MEDIA_ID) as? String)
            }
            if (metadata.containsKey(PROP_MEDIA_URI)) {
                setMediaUri(metadata.get(PROP_MEDIA_URI) as? String)
            }
            if (metadata.containsKey(PROP_NUM_TRACKS)) {
                setNumberOfTracks(metadata.get(PROP_NUM_TRACKS) as? Long)
            }
            if (metadata.containsKey(PROP_TITLE)) {
                setTitle(metadata.get(PROP_TITLE) as? String)
            }
            if (metadata.containsKey(PROP_TRACK_NUMBER)) {
                setTrackNumber(metadata.get(PROP_TRACK_NUMBER) as? Long)
            }
            if (metadata.containsKey(PROP_WRITER)) {
                setWriter(metadata.get(PROP_WRITER) as? String)
            }
            if (metadata.containsKey(PROP_YEAR)) {
                setYear(metadata.get(PROP_YEAR) as? Long)
            }
        }
    }
}

fun makeSafeBitmapCopy(bitmap: Bitmap?): Bitmap? {
    // Never pass a recycled bitmap, and make a copy so the original can be recycled.
    return bitmap?.takeIf { !it.isRecycled }?.copy(bitmap.config, false)
}