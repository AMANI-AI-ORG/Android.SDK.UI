package ai.amani.sample.domain.model

/** Token + server resolved from a scanned Amani QR, used to start the UI SDK. */
data class ProfileUrlInfo(val token: String, val serverUrl: String)
