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
        builder.putLong(
            MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT,
            if (player.ads.isPlaying) 1 else 0
        )
        if (!java.lang.Double.isNaN(player.duration)) {
            setDuration((1e03 * player.duration).toLong())
        }
        connector.mediaSession.setMetadata(builder.build())
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
     * See [MediaMetadataCompat.METADATA_KEY_ALBUM].
     */
    fun setAlbum(value: String?) {
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_ALBUM_ART].
     */
    fun setAlbumArt(value: Bitmap?) {
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, value)
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
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, value)
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
    fun setDiscNumber(value: Long) {
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, value)
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
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, value)
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
    fun setDownloadStatus(value: Long) {
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS, value)
    }

    /**
     * See [MediaMetadataCompat.METADATA_KEY_DURATION].
     */
    fun setDuration(value: Long) {
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, value)
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
    fun setNumberOfTracks(value: Long) {
        builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, value)
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
    fun setTrackNumber(value: Long) {
        builder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, value)
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
    fun setYear(value: Long) {
        builder.putLong(MediaMetadataCompat.METADATA_KEY_YEAR, value)
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
                setAlbum(metadata.get(PROP_ALBUM))
            }
            if (metadata.containsKey(PROP_ALBUM_ART)) {
                try {
                    setAlbumArt(metadata.get(PROP_ALBUM_ART))
                } catch (ignore: ClassCastException) {
                    Log.e(TAG, "Invalid value for metadata $PROP_ALBUM_ART")
                }
            }
            if (metadata.containsKey(PROP_ALBUM_ARTIST)) {
                setAlbumArtist(metadata.get(PROP_ALBUM_ARTIST))
            }
            if (metadata.containsKey(PROP_ALBUM_ART_URI)) {
                setAlbumArtUri(metadata.get(PROP_ALBUM_ART_URI))
            }
            if (metadata.containsKey(PROP_ART)) {
                try {
                    setArt(metadata.get(PROP_ART))
                } catch (ignore: ClassCastException) {
                    Log.e(TAG, "Invalid value for metadata $PROP_ART")
                }
            }
            if (metadata.containsKey(PROP_ARTIST)) {
                setArtist(metadata.get(PROP_ARTIST))
            }
            if (metadata.containsKey(PROP_AUTHOR)) {
                setAuthor(metadata.get(PROP_AUTHOR))
            }
            if (metadata.containsKey(PROP_COMPILATION)) {
                setCompilation(metadata.get(PROP_COMPILATION))
            }
            if (metadata.containsKey(PROP_COMPOSER)) {
                setComposer(metadata.get(PROP_COMPOSER))
            }
            if (metadata.containsKey(PROP_DATE)) {
                setDate(metadata.get(PROP_DATE))
            }
            if (metadata.containsKey(PROP_DISC_NUMBER)) {
                try {
                    setDiscNumber(metadata.get(PROP_DISC_NUMBER))
                } catch (ignore: ClassCastException) {
                    Log.e(TAG, "Invalid value for metadata $PROP_DISC_NUMBER")
                }
            }
            if (metadata.containsKey(PROP_DISPLAY_DESCRIPTION)) {
                setDisplayDescription(metadata.get(PROP_DISPLAY_DESCRIPTION))
            }
            if (metadata.containsKey(PROP_DISPLAY_ICON)) {
                try {
                    setDisplayIcon(metadata.get(PROP_DISPLAY_ICON))
                } catch (ignore: ClassCastException) {
                    Log.e(TAG, "Invalid value for metadata $PROP_DISPLAY_ICON")
                }
            }
            if (metadata.containsKey(PROP_DISPLAY_ICON_URI)) {
                setDisplayIconUri(metadata.get(PROP_DISPLAY_ICON_URI))
            }
            if (metadata.containsKey(PROP_DISPLAY_SUBTITLE)) {
                setDisplaySubtitle(metadata.get(PROP_DISPLAY_SUBTITLE))
            }
            if (metadata.containsKey(PROP_DISPLAY_TITLE)) {
                setDisplayTitle(metadata.get(PROP_DISPLAY_TITLE))
            }
            if (metadata.containsKey(PROP_DOWNLOAD_STATUS)) {
                try {
                    setDownloadStatus(metadata.get(PROP_DOWNLOAD_STATUS))
                } catch (ignore: ClassCastException) {
                    Log.e(TAG, "Invalid value for metadata $PROP_DOWNLOAD_STATUS")
                }
            }
            if (metadata.containsKey(PROP_GENRE)) {
                setGenre(metadata.get(PROP_GENRE))
            }
            if (metadata.containsKey(PROP_MEDIA_ID)) {
                setMediaId(metadata.get(PROP_MEDIA_ID))
            }
            if (metadata.containsKey(PROP_MEDIA_URI)) {
                setMediaUri(metadata.get(PROP_MEDIA_URI))
            }
            if (metadata.containsKey(PROP_NUM_TRACKS)) {
                try {
                    setNumberOfTracks(metadata.get(PROP_NUM_TRACKS))
                } catch (ignore: ClassCastException) {
                    Log.e(TAG, "Invalid value for metadata $PROP_NUM_TRACKS")
                }
            }
            if (metadata.containsKey(PROP_TITLE)) {
                setTitle(metadata.get(PROP_TITLE))
            }
            if (metadata.containsKey(PROP_TRACK_NUMBER)) {
                try {
                    setTrackNumber(metadata.get(PROP_TRACK_NUMBER))
                } catch (ignore: ClassCastException) {
                    Log.e(TAG, "Invalid value for metadata $PROP_TRACK_NUMBER")
                }
            }
            if (metadata.containsKey(PROP_WRITER)) {
                setWriter(metadata.get(PROP_WRITER))
            }
            if (metadata.containsKey(PROP_YEAR)) {
                try {
                    setYear(metadata.get(PROP_YEAR))
                } catch (ignore: ClassCastException) {
                    Log.e(TAG, "Invalid value for metadata $PROP_YEAR")
                }
            }
        }
    }
}
