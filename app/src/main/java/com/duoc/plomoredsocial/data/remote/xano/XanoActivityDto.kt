package com.duoc.plomoredsocial.data.remote.xano

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Represents a remote activity record from Xano with full event data.
 */
data class XanoActivityDto(val raw: JsonObject)

val XanoActivityDto.id: String get() = raw.get("id")?.asString ?: raw.get("id")?.asLong?.toString() ?: ""
val XanoActivityDto.title: String get() = raw.get("titulo")?.asString?.takeIf { !it.isNullOrBlank() } ?: "Actividad Comunitaria"
val XanoActivityDto.description: String get() = raw.get("descripcion")?.asString ?: ""
val XanoActivityDto.commune: String get() = raw.get("comuna")?.asString?.takeIf { !it.isNullOrBlank() } ?: "Santiago"
val XanoActivityDto.meetingPoint: String get() = raw.get("ubicacion")?.asString?.takeIf { !it.isNullOrBlank() } ?: "Punto de encuentro"
val XanoActivityDto.organizer: String get() = raw.get("organizador")?.asString?.takeIf { !it.isNullOrBlank() } ?: "Organizador Mygos"
val XanoActivityDto.capacity: Int get() = raw.get("cupos")?.asInt?.coerceIn(1, 10) ?: 10
val XanoActivityDto.schedule: String get() = raw.get("fecha")?.asString?.takeIf { !it.isNullOrBlank() } ?: run {
    val created = raw.get("created_at")?.asLong
    if (created != null && created > 0) {
        val sdf = java.text.SimpleDateFormat("dd-MM-yyyy, HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(created))
    } else "Próximamente"
}
val XanoActivityDto.latitude: Double? get() = raw.get("latitude")?.takeIf { !it.isJsonNull }?.asDouble
val XanoActivityDto.longitude: Double? get() = raw.get("longitude")?.takeIf { !it.isJsonNull }?.asDouble

/** Hide common credential/contact fields if the public endpoint ever returns them. */
fun JsonElement.redactedForDisplay(): JsonElement {
    if (isJsonArray) {
        val copy = com.google.gson.JsonArray()
        asJsonArray.forEach { copy.add(it.redactedForDisplay()) }
        return copy
    }
    if (!isJsonObject) return deepCopy()

    val copy = JsonObject()
    asJsonObject.entrySet().forEach { (name, value) ->
        val key = name.lowercase()
        val isPrivate = listOf("password", "token", "secret", "email", "correo", "phone", "telefono")
            .any(key::contains)
        copy.add(name, if (isPrivate) com.google.gson.JsonPrimitive("[oculto]") else value.redactedForDisplay())
    }
    return copy
}
