package ai.amani.sample.data.repository

import ai.amani.sample.data.remote.ProfileUrlRemoteDataSource
import ai.amani.sample.domain.model.ProfileUrlInfo
import ai.amani.sample.domain.repository.ProfileUrlRepository

class ProfileUrlRepositoryImpl(
    private val remote: ProfileUrlRemoteDataSource
) : ProfileUrlRepository {
    override suspend fun resolve(scannedUrl: String): ProfileUrlInfo? = remote.getProfileUrl(scannedUrl)
}
