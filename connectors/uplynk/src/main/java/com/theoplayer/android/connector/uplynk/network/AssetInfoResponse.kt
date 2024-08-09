package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the details of an asset retrieved from the AssetInfo request.
 *
 * This data class encapsulates various properties and metadata about an asset,
 * such as its ratings, duration, thumbnail information, and more.
 *
 * For further details, please refer to the Uplynk Documentation:
 * [AssetInfo Documentation](https://docs.edgecast.com/video/#Develop/AssetInfo.htm%3FTocPath%3DDevelop%7CClient%2520(Media%2520Player)%7C_____1)
 */
@Serializable
data class AssetInfoResponse(
    /**
     * Returns whether the asset is audio only.
     *
     * <ul>
     *     <li>1 if it is audio.
     * </ul>
     */
    @SerialName("audio_only")
    val audioOnly: Int,
    /**
     * List of objects which contain information for the boundaries for the asset.
     *
     * <ul>
     *     <li>Will contain instances of the subclasses of {@link BoundaryDetail}.
     * </ul>
     *
     * @return List of objects which contain information for the {@link BoundaryDetail}s for the asset. (<b>Nullable</b>)
     */
    @SerialName("boundary_details")
    val boundaryDetails: List<BoundaryDetail>? = null,
    /**
     * Returns whether an error occurred.
     *
     * <ul>
     *     <li>Zero if error
     *     <li>One otherwise.
     * </ul>
     */
    @SerialName("error")
    val error: Int,
    /**
     * The tv-rating of the asset.
     *
     * <ul>
     *      <li>-1: Not Available
     *      <li>0: Not Rated
     *      <li>1: TV-Y
     *      <li>2: TV-Y7
     *      <li>3: TV-G
     *      <li>4: TV-PG
     *      <li>5: TV-14
     *      <li>6: TV-MA
     *      <li>7: Not Rated
     * </ul>
     *
     * @return The {@link VerizonMediaAssetTvRating}. (<b>NonNull</b>)
     */
    @SerialName("tv_rating")
    val tvRating: Int,
    /**
     * The number of slices available for the asset.
     */
    @SerialName("max_slice")
    val maxSlice: Int,
    /**
     * The prefix URL to the thumbnails.
     *
     */
    @SerialName("thumb_prefix")
    val thumbPrefix: String,

    /**
     * The average slice duration.
     */
    @SerialName("slice_dur")
    val sliceDur: Double,
    /**
     * The movie rating of the asset.
     *
     * <ul>
     *     <li>-1: Not Available
     *     <li>0: Not Applicable
     *     <li>1: G
     *     <li>2: PG
     *     <li>3: PG-13
     *     <li>4: R
     *     <li>5: NC-17
     *     <li>6: X
     *     <li>7: Not Rated
     * </ul>
     *
     */
    @SerialName("movie_rating")
    val movieRating: Int,
    /**
     * The identifier of the owner.
     *
     * <p>Example:</p>
     * <ul>
     *     <li>"fb3a4cb996THEOOWNERa101477ffad8fb"
     * </ul>
     */
    @SerialName("owner")
    val owner: String,
    /**
     * The metadata attached to the asset.
     *
     * <ul>
     *     <li>Metadata may be added via the CMS.
     * </ul>
     *
     * <b>NonNull</b>
     */
    @SerialName("meta")
    val meta: Map<String, String>,
    /**
     * The available bitrates of the asset.
     *
     * <b>NonNull</b>
     */
    @SerialName("rates")
    val rates: List<Int>,
    /**
     * List of thumbnail resolutions of the asset.
     *
     * <b>NonNull</b>
     */
    @SerialName("thumbs")
    val thumbs: List<Thumbnail>,
    /**
     * The poster URL of the asset.
     *
     * <b>NonNull</b>
     */
    @SerialName("poster_url")
    val posterUrl: String,
    /**
     * The duration of the asset.
     *
     * <b>NonNull</b>
     */
    @SerialName("duration")
    val duration: Double,
    /**
     * The default poster URL created for the asset.
     *
     * <b>NonNull</b>
     */
    @SerialName("default_poster_url")
    val defaultPosterUrl: String,
    /**
     * The description of the asset.
     *
     * <b>NonNull</b>
     */
    @SerialName("desc")
    val desc: String,
    /**
     * The ratings for the asset, as bitwise flags.
     *
     * These available flags are the following:
     * <ul>
     *     <li>D: Drug-related themes are present
     *     <li>V: Violence is present
     *     <li>S: Sexual situations are present
     *     <li>L: Adult Language is present
     * </ul>
     *
     * This number is a bitwise number to indicate if one or more of these values are present.
     * <ul>
     *     <li>[D][V][S][L] - 0: No rating flag.
     *     <li>[D][V][S][L] - 1: Language flag.
     *     <li>[D][V][S][L] - 2: Sex flag.
     *     <li>[D][V][S][L] - 4: Violence flag.
     *     <li>[D][V][S][L] - 8: Drugs flag.
     *     <li>[D][V][S][L] - 15: All flags are on.
     * </ul>
     *
     */
    @SerialName("rating_flags")
    val ratingFlags: Int,
    /**
     * The identifier of the external source.
     *
     * <b>NonNull</b>
     */
    @SerialName("external_id")
    val externalId: String,
    /**
     * Returns whether the asset is an ad.
     *
     * <ul>
     *     <li>Zero if it is not an ad
     *     <li>One otherwise
     * </ul>
     */
    @SerialName("is_ad")
    val isAd: Int,
    /**
     * The identifier of the asset.
     *
     * <p>Example:</p>
     * <ul>
     *     <li>"5a3THEO272d44THEOc810d5849aeTHEO"
     * </ul>
     * <b>NonNull</b>
     */
    @SerialName("asset")
    val asset: String
)

/**
 * Data class representing the details of a boundary.
 *
 * @property offset Indicates the offset, in seconds, of the boundary.
 * @property duration Indicates the duration, in seconds, of the boundary.
 */
@Serializable
data class BoundaryDetail(
    val offset: Int,
    val duration: Int
)

/**
 * Data class representing a thumbnail resolution.
 *
 */
@Serializable
data class Thumbnail(
    /**
     * The height of the thumbnail, in pixels. (<b>Nullable</b>)
     */
    val height: Int?,
    /**
     * The width of the thumbnail, in pixels. (<b>Nullable</b>)
     */
    val width: Int?,
    /**
     * The prefix of the thumbnail. (<b>NonNull</b>)
     */
    val prefix: String,
    /**
     * The requested width, in pixels.
     *
     * <ul>
     *     <li>This can differ from the actual width because images are not stretched.
     * </ul>
     */
    val bw: Int,
    /**
     * The requested height, in pixels.
     *
     * <ul>
     *     <li>This can differ from the actual width because images are not stretched.
     * </ul>
     */
    val bh: Int,
)
