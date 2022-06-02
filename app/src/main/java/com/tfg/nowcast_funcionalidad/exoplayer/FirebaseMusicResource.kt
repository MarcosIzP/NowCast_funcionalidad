package com.tfg.nowcast_funcionalidad.exoplayer

import android.content.Context
import android.media.MediaMetadata
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.tfg.nowcast_funcionalidad.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirebaseMusicResource @Inject constructor(
    private val musicDatabase: MusicDatabase
) {

    var songs = emptyList<MediaMetadataCompat>()


    suspend fun fetchMediaData() = withContext(Dispatchers.IO) {

        state = State.STATE_INITIALIZING

        val allSongs= musicDatabase.getAllSongs()

        songs = allSongs.map { song ->

            MediaMetadataCompat.Builder()

            .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artista)
            .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, song.media_id)
            .putString(MediaMetadata.METADATA_KEY_TITLE, song.nombre)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, song.nombre)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
            .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, song.imageURL)
            .putString(MediaMetadata.METADATA_KEY_GENRE, song.genero)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, song.imageURL)
            .putString(MediaMetadata.METADATA_KEY_MEDIA_URI, song.songURL)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, song.artista)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, song.artista)
                .build()
        }
        state = State.STATE_INITIALIZED

    }

    lateinit var context: Context

    var provideDataSourceFactory: DefaultDataSourceFactory = DefaultDataSourceFactory(
        context,Util.getUserAgent(context, "NowCast")
    )


    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory) : ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach { song ->
            val mediaSource = ProgressiveMediaSource.Factory(provideDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(song.getString(MediaMetadata.METADATA_KEY_MEDIA_URI).toUri()))
                concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    fun asMediaItems() = songs.map { song ->

        val desc = MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(MediaMetadata.METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }.toMutableList()

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    private var state : State = State.STATE_CREATED
        set(value) {

            if (value == State.STATE_INITIALIZED || value == State.STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value

                    onReadyListeners.forEach { listener ->
                        listener(state == State.STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    fun whenReady(action : (Boolean) -> Unit): Boolean {

        if (state == State.STATE_CREATED || state == State.STATE_INITIALIZING ) {
            onReadyListeners += action
            return false
        } else {
            action(state == State.STATE_INITIALIZED)
            return true
        }
    }

    enum class State{
        STATE_CREATED, //estado para cuando se empiecen a enviar peticiones a la base de datos
        STATE_INITIALIZING, // estado para cuando se empiecen a descargar las canciones
        STATE_INITIALIZED, // estado para cuando ya se hayan descargado
        STATE_ERROR, //estado por si ocurre un error
    }
}