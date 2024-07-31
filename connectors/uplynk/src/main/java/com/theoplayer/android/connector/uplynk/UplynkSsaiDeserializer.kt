package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.api.source.ssai.CustomSsaiDescription
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UplynkSsaiDeserializer : CustomSsaiDescriptionSerializer {
    override fun fromJson(json: String): UplynkSsaiDescription {
        return Json.decodeFromString(json)
    }

    override fun toJson(value: CustomSsaiDescription): String {
        return Json.encodeToString(value as UplynkSsaiDescription)
    }
}
