package com.buildai.ultra.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "build_history")
data class BuildHistoryEntity(
    @PrimaryKey val id: String,
    val idea: String,
    val status: String,
    val downloadUrl: String?,
    val apkSize: Long,
    val errorMessage: String?,
    val createdAt: Long
)
