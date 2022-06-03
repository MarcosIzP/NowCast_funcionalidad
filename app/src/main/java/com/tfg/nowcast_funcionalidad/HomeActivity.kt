package com.tfg.nowcast_funcionalidad

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.facebook.login.LoginManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.tfg.nowcast_funcionalidad.data.entities.Song
import kotlinx.android.synthetic.main.activity_home.*


enum class ProviderType {
    //Tipo de autenticacion
//BASIC : autenticacion por email y contraseña
//GOOGLE : cuenta de google
    BASIC,
    GOOGLE,
    FACEBOOK
}

class HomeActivity : AppCompatActivity(), Player.Listener {

    lateinit var player: SimpleExoPlayer

    lateinit var playerView: PlayerControlView

    val database = FirebaseDatabase.getInstance()
    val myRef = database.getReference("songs")

    lateinit var songItems:ArrayList<MediaItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


        val bundle:Bundle? = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")

        setup(email ?: "", provider ?: "")


        val preferencias = getSharedPreferences(getString(R.string.fichero_preferencias), Context.MODE_PRIVATE).edit()

        preferencias.putString("email", email)
        preferencias.putString("proveedor", provider)
        preferencias.apply()



        //Inicializamos la variable mediante "Builder()", que nos permite iniciarlizala sin necesidad de crearla como nuevo objeto
        player = SimpleExoPlayer.Builder(this).build()

        playerView = findViewById(R.id.playerview)
        songItems = ArrayList()

        getSongs()
    }
    private fun setup(email: String, provider: String){

        //titulo que aparecerá arriba a la derecha
        title ="Inicio"
        emailview.text = email
        providerview.text = provider

        logOutButton.setOnClickListener {

            //Una vez se pulse el boton de cerrar sesion se eliminaran las preferencias guardadas

            val preferencias = getSharedPreferences(getString(R.string.fichero_preferencias), Context.MODE_PRIVATE).edit()
            preferencias.clear()
            preferencias.apply()

            if (provider == ProviderType.FACEBOOK.name) {
                LoginManager.getInstance().logOut()
            }

            //Llamada a los sevicios de firebase
            FirebaseAuth.getInstance().signOut()
            onBackPressed()
            //esta última línea sirve para una vez cerrada la sesión, volver a la pantalla de registro

            player.stop()
        }
    }

    fun getSongs() {
        myRef.get().addOnSuccessListener {


            for (data in it.children) {
                var songAux = data.getValue(Song::class.java)

                if (songAux!=null) {

                    var metaData = MediaMetadata.Builder()
                        .setTitle(songAux.nombre)
                        .setArtist(songAux.artista)
                        .setAlbumTitle(songAux.album)
                        .setGenre(songAux.genero)
                        .setArtworkUri(Uri.parse(songAux.imageURL))
                        .build()

                    var item:MediaItem = MediaItem.Builder()
                        .setUri(songAux.songURL)
                        .setMediaMetadata(metaData)
                        .build()

                    songItems.add(item)
                }

            }
            initPlayer()
        }
    }

    private fun initPlayer() {
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addMediaItems(songItems)

        player.addListener(this)

        player.prepare()

        playerView.player = player

    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)

        var nombre = findViewById<TextView>(R.id.song_name)
        var artista = findViewById<TextView>(R.id.artista)
        var album = findViewById<TextView>(R.id.album)
        var genero = findViewById<TextView>(R.id.genero)
        var imagen = findViewById<ImageView>(R.id.songimage_id)

        if (mediaItem!=null) {

            nombre.text = mediaItem.mediaMetadata.title
            artista.text = mediaItem.mediaMetadata.artist
            album.text = mediaItem.mediaMetadata.albumTitle
            genero.text = mediaItem.mediaMetadata.genre
            Picasso.get().load(mediaItem.mediaMetadata.artworkUri).into(imagen)
        }
    }
}