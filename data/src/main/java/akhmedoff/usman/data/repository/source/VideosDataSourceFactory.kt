package akhmedoff.usman.data.repository.source

import akhmedoff.usman.data.api.VkApi
import akhmedoff.usman.data.db.OwnerDao
import akhmedoff.usman.data.model.Video
import android.arch.paging.DataSource

class VideosDataSourceFactory(
    private val vkApi: VkApi,
    private val ownerId: String?,
    private val videoId: String?,
    private val albumId: String?,
    val ownerDao: OwnerDao
) : DataSource.Factory<Int, Video> {
    override fun create() =
        VideosDataSource(vkApi, ownerId, videoId, albumId, ownerDao)
}