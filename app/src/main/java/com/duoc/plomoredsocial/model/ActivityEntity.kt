package com.duoc.plomoredsocial.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val title: String,
    val commune: String,
    val meetingPoint: String,
    val schedule: String,
    val latitude: Double?,
    val longitude: Double?,
    val organizerId: String,
    @androidx.room.ColumnInfo(defaultValue = "''") val description: String = "",
    @androidx.room.ColumnInfo(defaultValue = "10") val capacity: Int = 10
)

@Entity(tableName = "activity_members", primaryKeys = ["activityId", "userId"])
data class ActivityMember(val activityId: String, val userId: String)

// El organizador ocupa un cupo incluso si no tiene inscripción propia.
fun ActivityEntity.participantCount(members: List<ActivityMember>): Int =
    1 + members.filter { it.activityId == id && it.userId != organizerId }
        .map { it.userId }.distinct().size
