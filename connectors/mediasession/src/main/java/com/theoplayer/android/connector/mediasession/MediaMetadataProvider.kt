package com.theoplayer.android.connector.mediasession

import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.metadata.MetadataDescription

val PROP_METADATA = hashMapOf(
    MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT to "advertisement",
    MediaMetadataCompat.METADATA_KEY_ALBUM to "album",
    MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST to "albumArtist",
    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI to "artUri",
    MediaMetadataCompat.METADATA_KEY_AUTHOR to "author",
    MediaMetadataCompat.METADATA_KEY_ARTIST to "artist",
    MediaMetadataCompat.METADATA_KEY_COMPILATION to "compilation",
    MediaMetadataCompat.METADATA_KEY_COMPOSER to "composer",
    MediaMetadataCompat.METADATA_KEY_DATE to "date",
    MediaMetadataCompat.METADATA_KEY_DISC_NUMBER to "discNumber",
    MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION to "displayDescription",
    MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI to "displayIconUri",
    MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE to "displaySubtitle",
    MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE to "displayTitle",
    MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS to "downloadStatus",
    MediaMetadataCompat.METADATA_KEY_DURATION to "duration",
    MediaMetadataCompat.METADATA_KEY_GENRE to "genre",
    MediaMetadataCompat.METADATA_KEY_MEDIA_ID to "mediaId",
    MediaMetadataCompat.METADATA_KEY_MEDIA_URI to "mediaUri",
    MediaMetadataCompat.METADATA_KEY_NUM_TRACKS to "numTracks",
    MediaMetadataCompat.METADATA_KEY_TITLE to "title",
    MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER to "trackNumber",
    MediaMetadataCompat.METADATA_KEY_WRITER to "writer",
    MediaMetadataCompat.METADATA_KEY_YEAR to "year",
)

class MediaMetadataProvider(private val connector: MediaSessionConnector) {
    companion object {
        private val METADATA_EMPTY = MediaMetadataCompat.Builder().build()
    }

    private var builder = MediaMetadataCompat.Builder()

    fun setMediaSessionMetadata(sourceDescription: SourceDescription?) {
        clearMediaSessionMetadataDescription()
        if (sourceDescription != null) {
            updateMetaDataDescription(sourceDescription)
            invalidateMediaSessionMetadata()
        }
    }

    fun getMediaSessionMetadata(): MediaMetadataCompat {
        return builder.build()
    }

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
            builder.putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                (1e03 * player.duration).toLong()
            )
        }
        connector.mediaSession.setMetadata(builder.build())
    }

    fun clearMediaSessionMetadataDescription() {
        if (connector.debug) {
            Log.d(TAG, "MediaMetadataProvider::clearMediaSessionMetadataDescription")
        }
        builder = MediaMetadataCompat.Builder()
        connector.mediaSession.setMetadata(METADATA_EMPTY)
    }

    private fun updateMetaDataDescription(sourceDescription: SourceDescription?) {
        if (sourceDescription == null) {
            connector.mediaSession.setMetadata(METADATA_EMPTY)
            return
        }
        val metadata = sourceDescription.metadata
        if (metadata != null) {
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_ALBUM)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_ARTIST)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_COMPOSER)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_DATE)
            buildLong(metadata, builder, MediaMetadataCompat.METADATA_KEY_DISC_NUMBER)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
            buildLong(metadata, builder, MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_GENRE)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_MEDIA_URI)
            buildLong(metadata, builder, MediaMetadataCompat.METADATA_KEY_NUM_TRACKS)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_TITLE)
            buildLong(metadata, builder, MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
            buildString(metadata, builder, MediaMetadataCompat.METADATA_KEY_WRITER)
            buildLong(metadata, builder, MediaMetadataCompat.METADATA_KEY_YEAR)
        } else {
            if (connector.debug) {
                Log.w(TAG, "No metadata passed with sourceDescription")
            }
        }
    }

    private fun buildString(
        metadataDescription: MetadataDescription,
        metadataBuilder: MediaMetadataCompat.Builder,
        metaProp: String
    ) {
        if (!metadataDescription.containsKey(PROP_METADATA[metaProp])) {
            return
        }
        if (connector.debug) {
            Log.d(
                TAG, "Found metadata field $metaProp: " + metadataDescription.get(
                    PROP_METADATA[metaProp]
                )
            )
        }
        metadataBuilder.putString(metaProp, metadataDescription.get(PROP_METADATA[metaProp]))
    }

    private fun buildLong(
        metadataDescription: MetadataDescription,
        metadataBuilder: MediaMetadataCompat.Builder,
        metaProp: String
    ) {
        if (!metadataDescription.containsKey(PROP_METADATA[metaProp])) {
            return
        }
        if (connector.debug) {
            Log.d(
                TAG, "Found metadata field $metaProp: " + metadataDescription.get(
                    PROP_METADATA[metaProp]
                )
            )
        }
        try {
            metadataBuilder.putLong(
                metaProp,
                metadataDescription.get<String>(PROP_METADATA[metaProp]).toLong()
            )
        } catch (ignore: NumberFormatException) {
            Log.e(TAG, "Invalid value for metadata ${PROP_METADATA[metaProp]}")
        }
    }
}
