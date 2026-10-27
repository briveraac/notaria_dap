package com.duoc.plomoredsocial.model

// Categorías reservadas para la futura selección de intereses.
// Las actividades actuales de Room todavía no guardan esta información.
enum class InterestCategory(val id: String) {
    OUTDOORS("outdoors"), SPORTS("sports"), CULTURE("culture"),
    GAMES("games"), TECHNOLOGY("technology"), COMMUNITY("community")
}
