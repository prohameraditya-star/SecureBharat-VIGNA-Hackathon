package com.example.paisacheck360

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class ScamVideoAdapter(
    private val context: Context,
    private val videoList: List<ScamVideo>,
    private val lifecycle: Lifecycle
) : RecyclerView.Adapter<ScamVideoAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.video_item, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.videoTitle.text = video.title

        lifecycle.addObserver(holder.youtubePlayerView)

        holder.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer) {
                youTubePlayer.cueVideo(video.videoId, 0f)
            }
        })
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val youtubePlayerView: YouTubePlayerView = itemView.findViewById(R.id.youtubePlayerView)
        val videoTitle: TextView = itemView.findViewById(R.id.videoTitle)
    }
}
