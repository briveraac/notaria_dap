package com.duoc.plomoredsocial.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY rowid DESC")
    fun observeActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity_members")
    fun observeMembers(): Flow<List<ActivityMember>>

    @Insert
    suspend fun create(activity: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun join(member: ActivityMember)

    @Query("DELETE FROM activity_members WHERE activityId = :activityId AND userId = :userId")
    suspend fun leave(activityId: String, userId: String)
}
