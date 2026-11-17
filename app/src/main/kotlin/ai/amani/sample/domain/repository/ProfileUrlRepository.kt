package ai.amani.sample.domain.repository

import ai.amani.sample.domain.model.ProfileUrlInfo

/** Resolves a scanned Amani QR into the token + server needed to start the UI SDK. */
interface ProfileUrlRepository {
    /** Returns null when the QR is malformed or the profile could not be resolved. */
    suspend fun resolve(scannedUrl: String): ProfileUrlInfo?
}
