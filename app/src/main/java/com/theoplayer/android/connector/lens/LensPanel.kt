package com.theoplayer.android.connector.lens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The demo's Lens settings, kept across restarts. */
object LensDemoSettings {
    private const val PREFERENCES = "lens_demo"
    private const val SEND_TO_STAGING = "send_to_staging"

    fun sendToStaging(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(SEND_TO_STAGING, false)

    fun setSendToStaging(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putBoolean(SEND_TO_STAGING, enabled).commit()
    }
}

/** Keys every report carries, left out of the per-report line; the session ones are shown above the list. */
private val ENVELOPE = setOf("e", "sta", "sn", "ns", "ts", "v", "sid", "u", "os", "osv", "p", "dc", "cid", "h", "sf", "cen")

private val TIME = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT)

private fun JSONObject.event(): String =
    when (val e = optString("e")) {
        "ps" -> "ps:${optString("sta")}"
        "ce" -> "ce:${optString("cen")}"
        else -> e
    }

private fun value(value: Any?): String =
    when (value) {
        is JSONArray -> (0 until value.length()).joinToString(",") { value(value.get(it)) }
        is JSONObject -> if (value.has("value")) value(value.get("value")) else value.toString()
        else -> value.toString()
    }

private fun LensReport.line(): String {
    val keys = json.keys().asSequence().filter { it !in ENVELOPE }.joinToString(" ") { "$it=${value(json.get(it))}" }
    return "${TIME.format(Date(receivedAt))} ${json.optString("ns")} ${json.event()} #${json.optInt("sn")}  $keys"
}

/**
 * What the demo sends to Lens, received in the app, with the staging switch and the scenario runner.
 */
@Composable
fun LensPanel(
    reports: List<LensReport>,
    sendToStaging: Boolean,
    onSendToStagingChange: (Boolean) -> Unit,
    scenarioStatus: String?,
    scenarioRunning: Boolean,
    onRunScenario: () -> Unit,
    onStopScenario: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Lens", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text("Send to staging", style = MaterialTheme.typography.bodySmall)
            Switch(checked = sendToStaging, onCheckedChange = onSendToStagingChange)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (scenarioRunning) {
                TextButton(onClick = onStopScenario) { Text("Stop scenario") }
            } else {
                TextButton(onClick = onRunScenario) { Text("Run scenario") }
            }
            TextButton(onClick = onClear) { Text("Clear") }
            Spacer(Modifier.width(4.dp))
            Text(scenarioStatus.orEmpty(), style = MaterialTheme.typography.bodySmall)
        }
        val last = reports.firstOrNull()?.json
        Text(
            text = if (last == null) "No reports yet: Lens reports from the first play request on a source." else
                "sid ${last.optString("sid")}\ncid ${last.optString("cid")}  sf ${last.optString("sf")}  ${reports.size} reports",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        LazyColumn(Modifier.fillMaxWidth()) {
            items(reports) { report ->
                Text(report.line(), fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
    }
}
