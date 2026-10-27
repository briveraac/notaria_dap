package com.duoc.plomoredsocial.repository

import com.duoc.plomoredsocial.model.ActivityDao
import com.duoc.plomoredsocial.model.ActivityEntity
import com.duoc.plomoredsocial.model.ActivityMember
import kotlinx.coroutines.flow.Flow

// Inicio y Actividades leen los mismos datos. Este contrato no crea copias ni inscripciones.
interface ActivityRepository {
    fun observeActivities(): Flow<List<ActivityEntity>>
    fun observeMembers(): Flow<List<ActivityMember>>
}

// Usa las tablas que ya existen en Room, sin agregar datos de demostración.
class RoomActivityRepository(private val dao: ActivityDao) : ActivityRepository {
    override fun observeActivities() = dao.observeActivities()
    override fun observeMembers() = dao.observeMembers()
}
