package akhmedoff.usman.thevt.ui.video

import akhmedoff.usman.data.Error
import akhmedoff.usman.data.model.Album
import akhmedoff.usman.data.model.ApiResponse
import akhmedoff.usman.data.model.Auth
import akhmedoff.usman.data.model.Likes
import akhmedoff.usman.data.model.Quality.EXTERNAL
import akhmedoff.usman.data.model.ResponseVideo
import akhmedoff.usman.data.model.User
import akhmedoff.usman.data.model.Video
import akhmedoff.usman.data.repository.AlbumRepository
import akhmedoff.usman.data.repository.UserRepository
import akhmedoff.usman.data.repository.VideoRepository
import akhmedoff.usman.data.utils.gson
import akhmedoff.usman.thevt.R
import android.util.Log
import androidx.lifecycle.Observer
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VideoPresenter(
        override var view: VideoContract.View?,
        private val videoRepository: VideoRepository,
        private val userRepository: UserRepository,
        private val albumRepository: AlbumRepository
) : VideoContract.Presenter {

    private lateinit var video: Video

    override fun onStart() {
        view?.let { view ->
            loadVideo("${view.getOwnerId()}_${view.getVideoId()}")
            getAlbumsByVideo(view.getVideoId(), view.getOwnerId())
        }
    }

    override fun onResume() {
        view?.setVideoPosition(view?.loadVideoPosition() ?: 0)
        view?.setVideoState(view?.getVideoState() ?: false)
    }

    override fun onPause() {
        view?.pauseVideo()
        view?.saveVideoPosition(view?.getVideoPosition() ?: 0)
    }

    override fun addToAlbums(albumsIds: MutableList<Album>) {
        val ids = mutableListOf<Int>()
        albumsIds.forEach {
            ids.add(it.id)
        }

        videoRepository
                .addToAlbum(
                        albumIds = ids,
                        ownerId = video.ownerId.toString(),
                        videoId = video.id.toString())
                .enqueue(object : Callback<ApiResponse<Int>> {
                    override fun onFailure(call: Call<ApiResponse<Int>>?,
                                           t: Throwable?) {
                    }

                    override fun onResponse(
                            call: Call<ApiResponse<Int>>?,
                            response: Response<ApiResponse<Int>>?
                    ) {
                        view?.hideAddDialog()
                    }
                })
    }

    private fun getAlbumsByVideo(videoId: String, ownerId: String) = albumRepository
            .getAlbumsByVideo(videoId = videoId, ownerId = ownerId)
            .enqueue(object : Callback<ApiResponse<List<Int>>> {
                override fun onResponse(
                        call: Call<ApiResponse<List<Int>>>?,
                        response: Response<ApiResponse<List<Int>>>?
                ) {

                    response?.body()?.response?.forEach {
                        if (it == -2) view?.setAdded()
                    }
                    response?.body()?.response?.let {
                        view?.showSelectedAlbums(it)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Int>>>?,
                                       t: Throwable?) {
                }

            })


    override fun onClick(itemView: Int) {
        when (itemView) {
            R.id.like_button ->
                if (video.likes?.userLikes == false)
                    likeCurrentVideo()
                else unlikeCurrentVideo()

            R.id.add_button -> shareCurrentVideo()

            R.id.add_to_videos -> addToMyVideos()

            R.id.add_to_album -> {
                view?.showAddDialog()
                loadAlbums()
            }
            R.id.delete_from_videos -> deleteVideo()
        }
    }

    private fun deleteVideo() = videoRepository
            .deleteVideo(ownerId = video.ownerId.toString(), videoId = video.id.toString())
            .enqueue(object : Callback<ApiResponse<Int>?> {
                override fun onFailure(call: Call<ApiResponse<Int>?>?,
                                       t: Throwable?) {

                }

                override fun onResponse(call: Call<ApiResponse<Int>?>?,
                                        response: Response<ApiResponse<Int>?>?) {
                    if (response?.body()?.response == 1) {
                        view?.setDeleted()
                    }
                }
            })

    private fun addToMyVideos() = videoRepository
            .addToUser(
                    ownerId = video.ownerId.toString(),
                    videoId = video.id.toString())
            .enqueue(object : Callback<ApiResponse<Int>> {
                override fun onFailure(call: Call<ApiResponse<Int>>?,
                                       t: Throwable?) {
                }

                override fun onResponse(
                        call: Call<ApiResponse<Int>>?,
                        response: Response<ApiResponse<Int>>?
                ) {
                    view?.hideAddDialog()
                    view?.setAdded()
                }
            })

    private fun loadAlbums() =
            view?.let { view ->
                albumRepository
                        .getAlbums()
                        .observe(view, Observer { pagedList ->
                            if (pagedList != null && pagedList.isNotEmpty()) {
                                view.showAlbums(pagedList)
                                view.showAlbumsLoading(false)
                                getAlbumsByVideo(video.id.toString(),
                                        video.ownerId.toString())
                            }
                        })
            }

    private fun shareCurrentVideo() =
            view?.let { view ->
                view.showShareDialog(
                        video.title,
                        view.getString(
                                R.string.shared_with_vt,
                                video.title,
                                "https://vk.com/video?z=video${video.ownerId}_${video.id}"
                        ))
            }

    private fun likeCurrentVideo() =
            likeVideo(video.ownerId.toString(),
                    video.id.toString(),
                    null,
                    null
            )

    private fun likeVideo(
            ownerId: String?,
            itemId: String,
            captchaSid: String?,
            captchaCode: String?
    ) = videoRepository
            .likeVideo(ownerId, itemId, captchaSid, captchaCode)
            .enqueue(object : Callback<ApiResponse<Likes>> {
                override fun onFailure(call: Call<ApiResponse<Likes>>?, t: Throwable?) {
                    video.likes?.let { view?.setLiked(it) }
                }

                override fun onResponse(
                        call: Call<ApiResponse<Likes>>?,
                        response: Response<ApiResponse<Likes>>?
                ) {
                    response?.body()?.let { _ ->
                        video.likes?.userLikes = true
                        video.likes?.let { view?.setLiked(it) }
                    }
                    response?.errorBody()?.let { body ->
                        errorConvert(body)
                        video.likes?.let { view?.setLiked(it) }
                    }

                }
            })

    private fun unlikeCurrentVideo() = unlikeVideo(video.ownerId.toString(),
            video.id.toString(), null, null)

    private fun unlikeVideo(
            ownerId: String?,
            itemId: String,
            captchaSid: String?,
            captchaCode: String?
    ) = videoRepository
            .unlikeVideo(ownerId, itemId, captchaSid, captchaCode)
            .enqueue(object : Callback<ApiResponse<Likes>> {
                override fun onFailure(call: Call<ApiResponse<Likes>>?, t: Throwable?) {
                    video.likes?.let { view?.setLiked(it) }
                }

                override fun onResponse(
                        call: Call<ApiResponse<Likes>>?,
                        response: Response<ApiResponse<Likes>>?
                ) {
                    response?.body()?.let { _ ->
                        video.likes?.userLikes = false
                        video.likes?.let { view?.setLiked(it) }
                    }
                    response?.errorBody()?.let { responseBody ->
                        errorConvert(responseBody)
                        video.likes?.let { view?.setLiked(it) }
                    }
                }
            })

    override fun changeQuality(position: Int) {
        val files = video.files.asReversed()
        view?.setQuality(files[position])
        view?.saveCurrentQuality(position)
        view?.setVideoPosition(view?.loadVideoPosition() ?: 0)
    }

    private fun errorConvert(response: ResponseBody) {
        val auth = gson.fromJson<Auth>(response.string(), Auth::class.java)

        when (auth.error) {
            Error.NEED_CAPTCHA -> {
                view?.saveCaptchaSid(auth.captchaSid!!)
                view?.showCaptcha(auth.captchaImg!!)
            }

            else -> {
            }
        }
    }

    override fun loadVideo(id: String) {
        view?.showProgress(true)
        view?.showUi(false)
        view?.showPlayer(false)
        videoRepository
                .getVideo(id)
                .enqueue(object : Callback<ResponseVideo> {
                    override fun onFailure(call: Call<ResponseVideo>?, t: Throwable?) {
                        view?.showProgress(false)
                        view?.showLoadError(true)
                    }

                    override fun onResponse(
                            call: Call<ResponseVideo>?,
                            response: Response<ResponseVideo>?
                    ) {
                        view?.showLoadError(false)
                        response?.body()?.let { responseVideo ->
                            when {
                                responseVideo.items.isNotEmpty() -> {
                                    video = responseVideo.items[0]
                                    showVideo(video)
                                }

                                else -> {
                                    view?.showProgress(false)
                                    view?.showUi(false)
                                    view?.showPlayer(false)
                                    view?.showLoadError(true)
                                }
                            }

                            responseVideo.groups?.forEach {
                                view?.showOwnerInfo(it)
                                view?.showProgress(false)

                                videoRepository.saveOwnerId(it.id)
                            }

                            responseVideo.profiles?.forEach {
                                loadUser(it)
                            }
                        }
                    }
                })
    }

    private fun showVideo(video: Video) {
        view?.showUi(true)
        view?.showPlayer(true)
        view?.showVideo(video)

        val qualityPosition = userRepository.getVideoQuality()

        video.files.forEach { videoUrl ->
            if (videoUrl.quality == EXTERNAL) {
                view?.setExternalUi(videoUrl)
            }
        }

        if (video.files.size > qualityPosition) {
            val quality = video.files[qualityPosition]
            view?.setQuality(quality)
            view?.saveCurrentQuality(qualityPosition)
            view?.setQualityPosition(qualityPosition)
        } else if (video.files.isNotEmpty()) {
            val quality = video.files[video.files.size - 1]
            view?.setQuality(quality)
            view?.saveCurrentQuality(video.files.size - 1)
            view?.setQualityPosition(video.files.size - 1)
        }
    }

    private fun loadUser(user: User) = userRepository
            .getUsers(user.id.toString())
            .enqueue(object : Callback<ApiResponse<List<User>>> {
                override fun onFailure(
                        call: Call<ApiResponse<List<User>>>?,
                        t: Throwable?
                ) {
                    Log.e("error", t?.message)

                    view?.showProgress(false)
                }

                override fun onResponse(
                        call: Call<ApiResponse<List<User>>>?,
                        response: Response<ApiResponse<List<User>>>?
                ) {
                    view?.showProgress(false)
                    response?.body()?.response?.get(0)?.let { user ->
                        view?.showOwnerInfo(user)
                        videoRepository.saveOwnerId(user.id)
                    }
                }
            })

    override fun clickFullscreen() {
        view?.saveIsFullscreen(
                when (view?.loadIsFullscreen() == true) {
                    true -> {
                        view?.setPlayerNormal()
                        view?.showSmallScreen()

                        false
                    }
                    false -> {
                        view?.showFullscreen()
                        view?.setPlayerFullscreen()

                        true
                    }
                }
        )
    }

    override fun changedPipMode(isOnPip: Boolean) {
        if (isOnPip) {
            view?.setPlayerFullscreen()
        } else {
            view?.setPlayerNormal()
        }
        view?.showUi(!isOnPip)
    }

    override fun onStop() {
        view?.pauseVideo()

        view?.getVideoState()?.let { isStartedVideo ->
            view?.saveVideoState(isStartedVideo)
        }
        view?.getVideoPosition()?.let { videoPosition ->
            view?.saveVideoPosition(videoPosition)
        }
    }

    override fun onDestroyView() {
        view?.stopVideo()
        view = null
    }

    override fun error(error: Error, message: String) {
    }

    override fun pipToggleButton() {
        view?.showUi(false)
        view?.enterPipMode(video)
    }

    override fun ownerClicked() {
        view?.let { view ->
            videoRepository.getOwner()
                    .observe(view, Observer { owner ->
                        owner?.let {
                            view.showOwnerGroup(it)
                        }
                    })
        }
    }

    override fun enterCaptcha(captchaCode: String) {
        view?.let {
            if (captchaCode.trim().isEmpty()) return

            videoRepository.likeVideo(
                    video.ownerId.toString(),
                    video.id.toString(),
                    it.loadCaptchaSid(),
                    captchaCode
            )
        }
    }

    override fun onBackListener() {
        if (view?.loadIsFullscreen() == true) {
            clickFullscreen()
        } else {
            view?.back()
        }
    }

    override fun openBrowser() {
        view?.let { view ->
            view.showVideoInBrowser(
                    "https://vk.com/video?z=video${view.getOwnerId()}_${view.getVideoId()}"
            )
        }
    }

    override fun getVideo(): Video = video

    override fun setVideo(video: Video) {
        this.video = video
        view?.showUi(true)
        showVideo(video)
        view?.showPlayer(true)
    }
}