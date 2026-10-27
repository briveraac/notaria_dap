// Entrada de Mygos: prepara MAIN-003 y abre la navegación de MAIN-006.
package com.duoc.plomoredsocial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.duoc.plomoredsocial.ui.theme.PlomoRedSocialTheme
import com.duoc.plomoredsocial.ui.navigation.MygosApp
import com.duoc.plomoredsocial.viewmodel.ContactViewModel
import com.duoc.plomoredsocial.repository.ContactRepository
import com.duoc.plomoredsocial.model.MygosDatabase
import com.duoc.plomoredsocial.repository.TemporaryAuthRepository
import com.duoc.plomoredsocial.repository.LocalSessionStore
import com.duoc.plomoredsocial.viewmodel.LoginViewModel
import com.duoc.plomoredsocial.viewmodel.HomeViewModel
import com.duoc.plomoredsocial.repository.RoomActivityRepository

// La actividad entrega las dependencias sin trasladar lógica de negocio a las pantallas.
class MainActivity : ComponentActivity() {
    // Obtiene el ViewModel de la actividad y prepara la pantalla.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = ContactRepository(MygosDatabase.getInstance(applicationContext).contactDao())
        val contactViewModel = ViewModelProvider(
            this, ContactViewModel.Factory(repository)
        )[ContactViewModel::class.java]
        val loginViewModel = ViewModelProvider(
            this, LoginViewModel.Factory(
                TemporaryAuthRepository(applicationContext.assets),
                LocalSessionStore(applicationContext.noBackupFilesDir)
            )
        )[LoginViewModel::class.java]
        val homeViewModel = ViewModelProvider(
            this, HomeViewModel.Factory(
                RoomActivityRepository(MygosDatabase.getInstance(applicationContext).activityDao())
            )
        )[HomeViewModel::class.java]
        setContent {
            PlomoRedSocialTheme {
                MygosApp(contactViewModel = contactViewModel, loginViewModel = loginViewModel,
                    homeViewModel = homeViewModel)
            }
        }
    }
}

// Saludo original, por si necesitamos volver a la pantalla anterior.
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

// Vista previa del saludo original.
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PlomoRedSocialTheme {
        Greeting("Android")
    }
}
