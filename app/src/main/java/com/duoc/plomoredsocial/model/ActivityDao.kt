package com.duoc.plomoredsocial.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ActivityDao {
    @Query("SELECT * FROM activities ORDER BY rowid DESC")
    abstract fun observeActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity_members")
    abstract fun observeMembers(): Flow<List<ActivityMember>>

    @Query("SELECT * FROM activities WHERE organizerId = :userId ORDER BY rowid DESC")
    abstract fun observeCreated(userId: String): Flow<List<ActivityEntity>>

    @Query("SELECT a.* FROM activities a INNER JOIN activity_members m ON a.id = m.activityId WHERE m.userId = :userId AND a.organizerId != :userId ORDER BY a.rowid DESC")
    abstract fun observeParticipating(userId: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    abstract suspend fun find(id: String): ActivityEntity?

    @Insert
    protected abstract suspend fun insertActivity(activity: ActivityEntity)

    // También protege a quienes guardan directamente desde la capa de datos.
    suspend fun create(activity: ActivityEntity) {
        require(activity.capacity in 1..10) { "La capacidad debe estar entre 1 y 10." }
        insertActivity(activity)
    }

    @Query("SELECT COUNT(*) FROM activity_members WHERE activityId = :activityId AND userId != :organizerId")
    protected abstract suspend fun countGuests(activityId: String, organizerId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM activity_members WHERE activityId = :activityId AND userId = :userId)")
    protected abstract suspend fun isMember(activityId: String, userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertMember(member: ActivityMember)

    // La lectura del cupo y la escritura comparten transacción, incluso entre llamadas concurrentes.
    @Transaction
    open suspend fun join(member: ActivityMember): Boolean {
        require(member.userId.isNotBlank())
        val activity = find(member.activityId) ?: return false
        if (member.userId == activity.organizerId || isMember(member.activityId, member.userId)) return true
        if (1 + countGuests(activity.id, activity.organizerId) >= activity.capacity) return false
        insertMember(member)
        return true
    }

    @Query("DELETE FROM activity_members WHERE activityId = :activityId AND userId = :userId")
    abstract suspend fun leave(activityId: String, userId: String)

    @Query("DELETE FROM activity_members WHERE activityId = :activityId")
    protected abstract suspend fun deleteMembers(activityId: String)

    @Query("DELETE FROM activities WHERE id = :activityId AND organizerId = :userId")
    protected abstract suspend fun deleteOwnedActivity(activityId: String, userId: String): Int

    // No hay claves foráneas en el esquema histórico: limpiamos ambas tablas de forma atómica.
    @Transaction
    open suspend fun deleteAsOrganizer(activityId: String, userId: String): Boolean {
        if (userId.isBlank() || find(activityId)?.organizerId != userId) return false
        deleteMembers(activityId)
        return deleteOwnedActivity(activityId, userId) == 1
    }
}
