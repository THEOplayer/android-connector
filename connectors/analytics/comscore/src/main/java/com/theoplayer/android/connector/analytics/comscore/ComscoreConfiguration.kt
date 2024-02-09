package com.theoplayer.android.connector.analytics.comscore

data class ComscoreConfiguration(
  val publisherId: String,
  val applicationName: String,
  val usagePropertiesAutoUpdateMode: Int,
  val userConsent: String,
  val secureTransmission: Boolean,
  val childDirected: Boolean,
  val debug: Boolean
)
