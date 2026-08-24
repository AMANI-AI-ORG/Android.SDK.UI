package ai.amani.sample.domain.usecase

import ai.amani.sample.domain.model.ProfileUrlInfo
import ai.amani.sample.domain.repository.ProfileUrlRepository

/**
 * Resolves the scanned QR into a [ProfileUrlInfo] (token + server). Ports the verify app's
 * GetURLInfoUseCase.
 */
class GetProfileUrlUseCase(private val repository: ProfileUrlRepository) {
    suspend operator fun invoke(scannedUrl: String): ProfileUrlInfo? = repository.resolve(scannedUrl)
}
