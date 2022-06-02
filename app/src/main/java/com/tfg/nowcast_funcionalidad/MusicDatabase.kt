package com.tfg.nowcast_funcionalidad

import com.google.firebase.firestore.FirebaseFirestore
import com.tfg.nowcast_funcionalidad.Otros.Constantes.SONG_COLLECTION
import com.tfg.nowcast_funcionalidad.data.entities.Song
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class MusicDatabase {

    private val firestore = FirebaseFirestore.getInstance()

    private val songCollection = firestore.collection(SONG_COLLECTION)

    suspend fun getAllSongs(): List<Song> {

        return try {
            songCollection.get().await().toObjects(Song::class.java)
        } catch (e : Exception) {
            emptyList()
        }
    }

}