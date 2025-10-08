package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import kotlin.test.assertContains
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class UplynkSsaiDescriptionConverterTest {
    private lateinit var ssaiDescription: UplynkSsaiDescription

    private lateinit var converter: UplynkSsaiDescriptionConverter

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        ssaiDescription = UplynkSsaiDescription(
            prefix = "preplayprefix",
            assetIds = listOf("asset1", "asset2", "asset3"),
            preplayParameters = LinkedHashMap(mapOf("p1" to "v1", "p2" to "v2", "p3" to "v3"))
        )
        converter = UplynkSsaiDescriptionConverter()
    }

    @Test
    fun buildPreplayVodUrl_whenPrefixIsNotNull_startsUrlFromPrefix() {
        val result = converter.buildPreplayVodUrl(ssaiDescription)

        assertTrue(result.startsWith("preplayprefix"))
    }

    @Test
    fun buildPreplayVodUrl_whenPrefixIsNull_startsUrlFromPrefix() {
        ssaiDescription = ssaiDescription.copy(prefix = null)

        val result = converter.buildPreplayVodUrl(ssaiDescription)

        assertTrue(result.startsWith("https://content.uplynk.com"))
    }

    @Test
    fun buildPreplayVodUrl_whenAssetIdHasMultipleValues_addsThemAsCommaSeparatedList() {
        val result = converter.buildPreplayVodUrl(ssaiDescription)

        assertTrue(result.contains("/asset1,asset2,asset3/"))
    }

    @Test
    fun buildPreplayVodUrl_whenAssetIdHasSingleValue_usesItAsJsonFilename() {
        ssaiDescription = ssaiDescription.copy(assetIds = listOf("singleasset"))

        val result = converter.buildPreplayVodUrl(ssaiDescription)

        assertTrue(result.contains("/singleasset.json"))
    }

    @Test
    fun buildPreplayVodUrl_whenAssetIdsIsEmpty_addsUserIdAndExternalIds() {
        ssaiDescription = UplynkSsaiDescription(
            assetIds = listOf(), externalIds = listOf("extId1", "extId2"), userId = "userId"
        )

        val result = converter.buildPreplayVodUrl(ssaiDescription)

        assertTrue(result.contains("userId"))
        assertTrue(result.contains("extId1,extId2/multiple.json"))
    }

    @Test
    fun buildPreplayVodUrl_whenAssetIdsIsEmptyAndExternalIdIsSingle_addsUserIdAndExternalId() {
        ssaiDescription = UplynkSsaiDescription(
            assetIds = listOf(), externalIds = listOf("extId1"), userId = "userId"
        )

        val result = converter.buildPreplayVodUrl(ssaiDescription)

        assertTrue(result.contains("userId"))
        assertTrue(result.contains("extId1.json"))
    }

    @Test
    fun buildPreplayVodUrl_always_followsTheTemplate() {
        val result = converter.buildPreplayVodUrl(ssaiDescription)

        val items = result.split("/", "?")
        assertEquals("preplayprefix", items[0])
        assertEquals("preplay", items[1])
        assertEquals("asset1,asset2,asset3", items[2])
        assertEquals("multiple.json", items[3])
        assertEquals("v=2&ad.cping=0&p1=v1&p2=v2&p3=v3", items[4])
    }

    @Test
    fun buildAssetInfoUrls_withoutSid_doesNotContainPbsParameter() {
        val result = converter.buildAssetInfoUrls(ssaiDescription, "", "prefix")

        assertTrue(result.none { it.contains("pbs=") })
    }

    @Test
    fun buildAssetInfoUrls_withSid_hasPbsParameter() {
        val result = converter.buildAssetInfoUrls(ssaiDescription, "sessionId", "prefix")

        assertTrue(result.all { it.contains("pbs=sessionId") })
    }

    @Test
    fun buildAssetInfoUrls_whenAssetIdIsEmptyAndExternalIdIsEmpty_returnsEmptyUrl() {
        ssaiDescription = UplynkSsaiDescription(
            assetIds = listOf(), externalIds = listOf()
        )

        val result = converter.buildAssetInfoUrls(ssaiDescription, "", "prefix")

        assertEquals(0, result.size)
    }

    @Test
    fun buildAssetInfoUrls_whenAssetIdHasValues_returnsAssetInfoUrls() {
        val result = converter.buildAssetInfoUrls(ssaiDescription, "", "prefix")

        assertEquals(3, result.size)

        assertContains(result, "prefix/player/assetinfo/asset1.json")
        assertContains(result, "prefix/player/assetinfo/asset2.json")
        assertContains(result, "prefix/player/assetinfo/asset3.json")
    }

    @Test
    fun buildAssetInfoUrls_whenAssetIdIsEmpty_returnsAssetInfoUrlsUsingExternalId() {
        ssaiDescription = ssaiDescription.copy(
            assetIds = listOf(),
            externalIds = listOf("extId1", "extId2"),
            userId = "userId"
        )

        val result = converter.buildAssetInfoUrls(ssaiDescription, "", "prefix")

        assertEquals(2, result.size)

        assertContains(result, "prefix/player/assetinfo/ext/userId/extId1.json")
        assertContains(result, "prefix/player/assetinfo/ext/userId/extId2.json")
    }

    @Test
    fun buildStartPingUrl_always_hasStartParameter() {
        val result = converter.buildStartPingUrl("prefix", "sessionId", 200.toDuration(DurationUnit.SECONDS))

        assertContains(result, "ev=start")
    }

    @Test
    fun buildStartPingUrl_always_startsTheSameAsNormalPingRequest() {
        val result = converter.buildStartPingUrl("prefix", "sessionId", 200.toDuration(DurationUnit.SECONDS))
        val pingUrl = converter.buildPingUrl("prefix", "sessionId", 200.toDuration(DurationUnit.SECONDS))

        assertContains(result, pingUrl)
    }

    @Test
    fun buildSeekedPingUrl_always_hasSeekParameters() {
        val result = converter.buildSeekedPingUrl("prefix", "sessionId", 200.toDuration(DurationUnit.SECONDS), 180.toDuration(DurationUnit.SECONDS))

        assertContains(result, "ev=seek")
        assertContains(result, "ft=180")
    }

    @Test
    fun buildSeekedPingUrl_always_startsTheSameAsNormalPingRequest() {
        val result = converter.buildSeekedPingUrl("prefix", "sessionId", 200.toDuration(DurationUnit.SECONDS), 180.toDuration(DurationUnit.SECONDS))
        val pingUrl = converter.buildPingUrl("prefix", "sessionId", 200.toDuration(DurationUnit.SECONDS))

        assertContains(result, pingUrl)
    }

    @Test
    fun buildPingUrl_always_followsThePingTemplate() {
        val currentTime = 200
        val result = converter.buildPingUrl("prefix", "sessionId", currentTime.toDuration(DurationUnit.SECONDS))

        assertEquals(result, "prefix/session/ping/sessionId.json?v=3&pt=200")
    }
}