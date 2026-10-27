package com.duoc.plomoredsocial.model

// Categorías compartidas que también servirán para elegir y guardar intereses más adelante.
enum class InterestCategory(val id: String) {
    OUTDOORS("outdoors"), SPORTS("sports"), CULTURE("culture"),
    GAMES("games"), TECHNOLOGY("technology"), COMMUNITY("community")
}

// Estados visibles: Abierta, En progreso y Completa (sin cupos disponibles).
enum class ActivityStatus { OPEN, IN_PROGRESS, COMPLETE }

// Datos de cada actividad. Los participantes y la capacidad forman el contador, por ejemplo 1/5.
data class HomeActivity(
    val id: String,
    val title: String,
    val category: InterestCategory,
    val description: String,
    val status: ActivityStatus,
    val participants: Int,
    val capacity: Int
) {
    init {
        // Evita grupos de más de diez personas y contadores fuera de los cupos disponibles.
        require(capacity in 1..10)
        require(participants in 0..capacity)
        // Una actividad abierta debe tener cupos; una en progreso debe tener participantes.
        require(status != ActivityStatus.OPEN || participants < capacity)
        require(status != ActivityStatus.IN_PROGRESS || participants > 0)
        // En este catálogo "Completa" indica que todos los cupos están ocupados.
        require(status != ActivityStatus.COMPLETE || participants == capacity)
    }
}
