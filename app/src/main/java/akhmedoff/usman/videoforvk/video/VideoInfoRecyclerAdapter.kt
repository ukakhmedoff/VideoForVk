package akhmedoff.usman.videoforvk.video

import akhmedoff.usman.data.model.Likes
import akhmedoff.usman.data.model.Owner
import akhmedoff.usman.data.model.Video
import akhmedoff.usman.videoforvk.R
import akhmedoff.usman.videoforvk.view.holders.VideoInfoViewHolder
import akhmedoff.usman.videoforvk.view.holders.VideoOwnerViewHolder
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.video_info_item.view.*

class VideoInfoRecyclerAdapter(private val clickListener: (Int) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var video: Video? = null
        set(value) {
            field = value
            notifyItemChanged(0)
        }
    var owner: Owner? = null
        set(value) {
            field = value
            notifyItemChanged(1)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.video_info_item -> VideoInfoViewHolder(
                clickListener,
                LayoutInflater.from(parent.context).inflate(
                    viewType,
                    parent,
                    false
                )
            )

            R.layout.video_owner_item -> VideoOwnerViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    viewType,
                    parent,
                    false
                )
            )

            else -> VideoInfoViewHolder(
                clickListener,
                LayoutInflater.from(parent.context).inflate(
                    viewType,
                    parent,
                    false
                )
            )
        }.apply { itemView.setOnClickListener { clickListener(it.id) } }

    override fun getItemViewType(position: Int) = when (position) {
        0 -> R.layout.video_info_item
        1 -> R.layout.video_owner_item
        else -> R.layout.video_info_item
    }

    override fun getItemCount(): Int =
        if (video != null && owner != null) 2
        else if (video != null) 1
        else 0

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (position) {
            0 -> (holder as VideoInfoViewHolder).bind(video!!)
            1 -> (holder as VideoOwnerViewHolder).bind(owner!!)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val item = payloads[0]

            if (position == 0
                && item is Likes
                && holder is VideoInfoViewHolder)
                if (item.userLikes) {
                    holder.setDrawable(
                        holder.itemView.like_button,
                        R.drawable.ic_favorite_fill_24dp
                    )
                } else {
                    holder.setDrawable(
                        holder.itemView.like_button,
                        R.drawable.ic_favorite_border
                    )
                }
        }

        super.onBindViewHolder(holder, position, payloads)
    }
}