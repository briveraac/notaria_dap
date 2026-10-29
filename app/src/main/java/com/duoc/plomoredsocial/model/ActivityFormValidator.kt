package com.duoc.plomoredsocial.model

import java.util.Calendar
import java.util.GregorianCalendar

// Calendario local estricto: no convierte automáticamente fechas imposibles en otro día.
object ActivityFormValidator {
    fun formatDate(value: String): String {
        val digits = value.filter { it in '0'..'9' }.take(8)
        return digits.take(2) + (if (digits.length > 2) "-" + digits.drop(2).take(2) else "") +
            (if (digits.length > 4) "-" + digits.drop(4) else "")
    }

    fun formatTime(value: String): String {
        val digits = value.filter { it in '0'..'9' }.take(4)
        return digits.take(2) + if (digits.length > 2) ":" + digits.drop(2) else ""
    }

    private fun dateCalendar(date: String): Calendar? {
        if (!Regex("[0-9]{2}-[0-9]{2}-[0-9]{4}").matches(date)) return null
        val year = date.substring(6).toInt()
        if (year !in 1..9999) return null
        return try {
            GregorianCalendar().apply {
                isLenient = false
                clear()
                // El mediodía evita rechazar un día válido si el horario de verano omite su medianoche.
                set(year, date.substring(3, 5).toInt() - 1, date.take(2).toInt(), 12, 0, 0)
                timeInMillis
            }
        } catch (_: IllegalArgumentException) { null }
    }

    fun dateError(date: String, now: Long = System.currentTimeMillis()): String? {
        val selected = dateCalendar(date) ?: return "Ingresa una fecha válida en formato DD-MM-AAAA."
        val today = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return if (selected.timeInMillis < today.timeInMillis) "La fecha no puede ser anterior a hoy." else null
    }

    fun timeError(time: String): String? =
        if (!Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(time))
            "Ingresa una hora válida de 00:00 a 23:59." else null

    fun scheduleError(date: String, time: String, now: Long = System.currentTimeMillis()): String? {
        dateError(date, now)?.let { return it }
        timeError(time)?.let { return it }
        val selected = requireNotNull(dateCalendar(date))
        return try {
            selected.set(Calendar.HOUR_OF_DAY, time.take(2).toInt())
            selected.set(Calendar.MINUTE, time.takeLast(2).toInt())
            if (selected.timeInMillis <= now) "Elige una fecha y hora posteriores al momento actual." else null
        } catch (_: IllegalArgumentException) { "La hora elegida no existe en la zona horaria local." }
    }

    fun descriptionError(description: String): String? =
        if (description.length > 2000) "La descripción admite hasta 2000 caracteres." else null

    // También valida en el guardado; no modifica actividades antiguas al leerlas.
    fun isValid(activity: ActivityEntity, now: Long = System.currentTimeMillis()): Boolean {
        val parts = activity.schedule.split(", ")
        val coordinatesValid = if (activity.latitude == null || activity.longitude == null)
            activity.latitude == null && activity.longitude == null
        else activity.latitude.isFinite() && activity.longitude.isFinite() &&
            activity.latitude in -90.0..90.0 && activity.longitude in -180.0..180.0
        return activity.title.isNotBlank() && activity.title.length <= 100 &&
            activity.commune.isNotBlank() && activity.commune.length <= 100 &&
            activity.meetingPoint.isNotBlank() && activity.meetingPoint.length <= 200 &&
            activity.capacity in 1..10 && descriptionError(activity.description) == null && coordinatesValid &&
            parts.size == 2 && scheduleError(parts[0], parts[1], now) == null
    }
}
