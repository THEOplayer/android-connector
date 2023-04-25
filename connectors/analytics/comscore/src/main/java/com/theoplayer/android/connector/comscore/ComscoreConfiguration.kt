package com.theoplayer.android.connector.comscore

data class ComscoreConfiguration(
  val publisherId: String,
  val applicationName: String,
  val userConsent: String,
  val secureTransmission: Boolean,
  val childDirected: Boolean,
  val debug: Boolean
)
