package com.tfg.nowcast_funcionalidad.autenticacion

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.tfg.nowcast_funcionalidad.HomeActivity
import com.tfg.nowcast_funcionalidad.ProviderType
import com.tfg.nowcast_funcionalidad.R
import kotlinx.android.synthetic.main.activity_auth.*

class AuthActivity : AppCompatActivity() {


    private val GOOGLE_SIGN_IN_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        setup()

        sesion()

        goToRegistActivity()
    }

    private fun sesion() {
        //Constante que guardará las preferencias pero esta vez sin el método "edit()", ya que no se van a editar datos, solo comprobar
        val preferencias = getSharedPreferences(getString(R.string.fichero_preferencias), Context.MODE_PRIVATE)
        //Constante que comprobará el email
        val email: String? = preferencias.getString("email", null)
        //Constante que comprobará el proveedor
        val provider: String? = preferencias.getString("provider", null)

        //Bucle que comprobará si existen los dos parámetros anteriores,
        // y si es así, nos enviará a la siguiente pantalla sin que tengamos que iniciar manualamente sesion
        if (email != null && provider != null) { //Mediante una operacion AND

            //Esta línea hará invisible la pantalla de autenticación si ya existe una sesion iniciada
            layout_auth.visibility = View.INVISIBLE

            //Nos enviará a la siguiente pantalla
            showHome(email, ProviderType.valueOf(provider))
        }

    }

    private fun setup() {

        registrar.setOnClickListener {

            if (email_regist.text.isNotEmpty() && pw_regist.text.isNotEmpty()) {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                        email_regist.text.toString(),
                        pw_regist.text.toString()
                    ).addOnCompleteListener {
                        if (it.isSuccessful) {
                            it.result?.user?.email?.let { it1 -> showHome(it1, ProviderType.BASIC) }
                        } else {
                            alerta()
                        }
                    }
                }
        }


        google_regist.setOnClickListener {

            val initGoogle = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("36828474316-2tcie8802hdja88fnq2v418roh4gku51.apps.googleusercontent.com")
                .requestEmail().build()

            val googleClient = GoogleSignIn.getClient(this, initGoogle)

            googleClient.signOut()

            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN_CODE)
        }

    }

    private fun goToRegistActivity(){

        login.setOnClickListener {
            goRegist()
        }
    }


    private fun alerta() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error autenticando al usuario")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun goRegist() {
        val registIntent : Intent = Intent(this, RegistActivity::class.java)

        startActivity(registIntent)
    }

    private fun showHome(email: String, provider: ProviderType) {
        val homeIntent: Intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider",provider)
        }
        startActivity(homeIntent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //Se comprueba que el codigo de esa actividad es igual a la constante global creada, si es igual,
        // significará que se esta intentando autenticar con google
        if (requestCode == GOOGLE_SIGN_IN_CODE) {

            //Ahora se configuran los servicios que proporciona firebase para iniciar sesión con google
            //Primero se crea una constante que será guardad más adelante por otra. Se encarga de guardar los datos de inicio de sesion
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            //A veces, "ActivityonReuslt" puede llegar a dar error,
            // ya que si no existe la cuenta, la siguiente lñínea de código que se encarga de recuperar la cuenta, no la estaría encontrando.
            try {
                //recuperar la cuenta autenticada
                val account = task.getResult(ApiException::class.java)

                //se comprueba que la cuenta existe
                if (account != null) {

                    //Autenticacion en google
                    val credenciales = GoogleAuthProvider.getCredential(account.idToken, null)

                    //Autenticacion en firebase. Le pasasmos la constante creada, para que el ususario salga en la consola de Firebase
                    FirebaseAuth.getInstance().signInWithCredential(credenciales)
                        .addOnCompleteListener {
                            //Se ejecuta la funcion "Addoncompletelistener()" para saber cuando ha finalizado la utenticacion en firebase
                            if (it.isSuccessful) {

                                //Si los datos son correctos se llama a la funcion "showhome" que contiene en intent
                                //Se le indica la cuenta de email, la que hemos recuperado mediante la constante "Account",
                                // y el tipo proveedor que se ha indicado en la otra actividad (GOOGLE)
                                showHome(account.email ?: "", ProviderType.GOOGLE)

                            } else {
                                //Si ocurre algún error se ejecuta la funcion creada "alerta()"
                                alerta()

                            }
                        }
                }
            } catch (e: ApiException) {
                //Si ocurre algún error se ejecuta la funcion creada "alerta()"
                alerta()
            }

        }
    }
}