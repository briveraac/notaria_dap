// Entrada de la app. Por ahora abre Contáctanos para poder probarlo.
package com.duoc.plomoredsocial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.duoc.plomoredsocial.ui.theme.PlomoRedSocialTheme
import com.duoc.plomoredsocial.ui.contact.ContactScreen
import com.duoc.plomoredsocial.ui.contact.ContactViewModel
import com.duoc.plomoredsocial.data.contact.ContactRepository
import com.duoc.plomoredsocial.data.local.MygosDatabase

// Mantiene la pantalla original mientras probamos Contáctanos.
class MainActivity : ComponentActivity() {
    // Obtiene el ViewModel de la actividad y prepara la pantalla.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = ContactRepository(MygosDatabase.getInstance(applicationContext).contactDao())
        val contactViewModel = ViewModelProvider(
            this, ContactViewModel.Factory(repository)
        )[ContactViewModel::class.java]
        // Cambiar a false para volver al saludo original cuando terminemos las pruebas.
        val previewContact = true
        setContent {
            PlomoRedSocialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (previewContact) {
                        ContactScreen(
                            viewModel = contactViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
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