package com.syauqialfanzari0008.studytask

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "is_done")
    val isDone: Boolean = false,
    @ColumnInfo(name = "priority")
    val priority: String = "Medium",
    @ColumnInfo(name = "due_date")
    val dueDate: String = "",
    @ColumnInfo(name = "category")
    val category: String = "Umum"
)