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
    val latitude: Double,
    val longitude: Double,
    val organizerId: String
)

@Entity(tableName = "activity_members", primaryKeys = ["activityId", "userId"])
data class ActivityMember(val activityId: String, val userId: String)
