package com.duoc.plomoredsocial.repository

import com.duoc.plomoredsocial.model.ActivityStatus
import com.duoc.plomoredsocial.model.HomeActivity
import com.duoc.plomoredsocial.model.InterestCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Punto de entrega de las actividades a Inicio. Más adelante se podrá cambiar su origen
// y considerar los intereses guardados en Room sin volver a construir la pantalla.
interface HomeRepository {
    fun observeActivities(): Flow<List<HomeActivity>>
}

// Ocho actividades inventadas para la demostración, con estados y cupos de ejemplo.
// No consulta cuentas ni intereses: el listado es el mismo para todas las personas.
class DemoHomeRepository : HomeRepository {
    override fun observeActivities(): Flow<List<HomeActivity>> = flowOf(listOf(
        HomeActivity("park", "Caminata por el parque", InterestCategory.OUTDOORS,
            "Un paseo tranquilo para conversar y disfrutar del aire libre en grupo.", ActivityStatus.OPEN, 1, 5),
        HomeActivity("volley", "Vóleibol entre amigos", InterestCategory.SPORTS,
            "Una tarde de juego recreativo. Puedes participar aunque estés comenzando.", ActivityStatus.IN_PROGRESS, 6, 10),
        HomeActivity("books", "Café y lecturas", InterestCategory.CULTURE,
            "Comparte tu libro favorito y descubre nuevas historias en una conversación relajada.", ActivityStatus.OPEN, 3, 8),
        HomeActivity("games", "Tarde de juegos de mesa", InterestCategory.GAMES,
            "Conoce al grupo con juegos cooperativos y partidas para principiantes.", ActivityStatus.COMPLETE, 10, 10),
        HomeActivity("code", "Encuentro de tecnología", InterestCategory.TECHNOLOGY,
            "Trae una idea y comparte lo que estás aprendiendo sobre proyectos digitales.", ActivityStatus.OPEN, 4, 10),
        HomeActivity("garden", "Huerto en comunidad", InterestCategory.COMMUNITY,
            "Aprende sobre plantas y colabora en el cuidado de un huerto junto a otras personas.", ActivityStatus.IN_PROGRESS, 5, 8),
        HomeActivity("photo", "Paseo de fotografía", InterestCategory.CULTURE,
            "Explora detalles de la ciudad y practica fotografía con tu celular.", ActivityStatus.COMPLETE, 5, 5),
        HomeActivity("stretch", "Movimiento al aire libre", InterestCategory.SPORTS,
            "Una pausa activa con estiramientos suaves y ejercicios a tu propio ritmo.", ActivityStatus.OPEN, 2, 10)
    ))
}
