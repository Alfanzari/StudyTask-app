package com.syauqialfanzari0008.studytask

import java.util.UUID

data class Task(
    val title: String,
    val isDone: Boolean = false,
    val id: String = UUID.randomUUID().toString()
)