package com.duoc.plomoredsocial.ui.navigation

import androidx.annotation.StringRes
import com.duoc.plomoredsocial.R

// Las rutas pertenecen a la interfaz; no agregan datos ni tablas a Room.
enum class MygosDestination(val route: String, @param:StringRes val title: Int) {
    HOME("inicio", R.string.nav_home),
    SEARCH("buscar", R.string.nav_search),
    ACTIVITIES("actividades", R.string.nav_activities),
    PROFILE("perfil", R.string.nav_profile),
    CONTACT("contacto", R.string.contact_title),
    LOGIN("login", R.string.nav_login),
    REGISTER("registro", R.string.nav_register);

    companion object {
        val main = listOf(HOME, SEARCH, PROFILE)
    }
}
