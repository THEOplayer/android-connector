package com.theoplayer.android.connector.lens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dolby.optiview.lens.LensDestination
import com.dolby.optiview.lens.LensFormat
import org.json.JSONArray
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The demo's Lens settings. Lens takes them when the connector joins, so they apply on the next start. */
data class LensDemoSettings(
    val sendToStaging: Boolean = false,
    /** An additional destination for the reports, such as a test CMCD endpoint. */
    val destinationUrl: String? = null,
    val destinationFormat: LensFormat = LensFormat.CMCD,
) {
    val destination: LensDestination?
        get() = destinationUrl?.let { LensDestination(it, format = destinationFormat) }

    fun save(context: Context) {
        preferences(context).edit()
            .putBoolean(SEND_TO_STAGING, sendToStaging)
            .putString(DESTINATION_URL, destinationUrl)
            .putString(DESTINATION_FORMAT, destinationFormat.name)
            .commit()
    }

    companion object {
        private const val SEND_TO_STAGING = "send_to_staging"
        private const val DESTINATION_URL = "destination_url"
        private const val DESTINATION_FORMAT = "destination_format"

        private fun preferences(context: Context) = context.getSharedPreferences("lens_demo", Context.MODE_PRIVATE)

        fun load(context: Context): LensDemoSettings {
            val preferences = preferences(context)
            return LensDemoSettings(
                sendToStaging = preferences.getBoolean(SEND_TO_STAGING, false),
                destinationUrl = preferences.getString(DESTINATION_URL, null),
                destinationFormat = LensFormat.valueOf(preferences.getString(DESTINATION_FORMAT, null) ?: LensFormat.CMCD.name),
            )
        }
    }
}

private fun isHttpUrl(url: String): Boolean =
    try {
        URL(url).protocol in setOf("http", "https") && URL(url).host.isNotEmpty()
    } catch (_: MalformedURLException) {
        false
    }

@Composable
private fun DestinationDialog(
    settings: LensDemoSettings,
    onSave: (LensDemoSettings) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var url by remember { mutableStateOf(settings.destinationUrl.orEmpty()) }
    var format by remember { mutableStateOf(settings.destinationFormat) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Additional destination") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.trim() },
                    label = { Text("URL") },
                    placeholder = { Text("https://collector.example.com/cmcd") },
                    singleLine = true,
                    isError = url.isNotEmpty() && !isHttpUrl(url),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (option in LensFormat.entries) {
                        RadioButton(selected = format == option, onClick = { format = option })
                        Text(option.name, Modifier.padding(end = 16.dp))
                    }
                }
                Text("Saving restarts the app.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = isHttpUrl(url),
                onClick = { onSave(settings.copy(destinationUrl = url, destinationFormat = format)) },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (settings.destinationUrl != null) {
                    TextButton(onClick = { onSave(settings.copy(destinationUrl = null)) }) { Text("Remove") }
                }
                TextButton(onClick = onDismissRequest) { Text("Cancel") }
            }
        },
    )
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
    settings: LensDemoSettings,
    onSettingsChange: (LensDemoSettings) -> Unit,
    scenarioStatus: String?,
    scenarioRunning: Boolean,
    onRunScenario: () -> Unit,
    onStopScenario: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var destinationDialogOpen by remember { mutableStateOf(false) }
    if (destinationDialogOpen) {
        DestinationDialog(settings, onSave = onSettingsChange, onDismissRequest = { destinationDialogOpen = false })
    }
    Column(modifier.padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Lens", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text("Send to staging", style = MaterialTheme.typography.bodySmall)
            Switch(checked = settings.sendToStaging, onCheckedChange = { onSettingsChange(settings.copy(sendToStaging = it)) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = settings.destination?.let { "Also to ${it.url} (${it.format})" } ?: "No additional destination",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
            TextButton(onClick = { destinationDialogOpen = true }) { Text("Destination") }
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
