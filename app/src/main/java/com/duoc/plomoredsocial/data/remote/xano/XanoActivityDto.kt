package com.duoc.plomoredsocial.data.remote.xano

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * The live collection currently returns an empty array, so the Xano field schema
 * cannot be inferred. Preserve each JSON object without inventing property names.
 */
data class XanoActivityDto(val raw: JsonObject)

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
