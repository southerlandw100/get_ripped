package com.example.get_ripped.data.model

data class Exercise (
    val id: Long,
    val name: String,
    val lastDate: String,
    val note: String? = null,
    val sets: List<SetEntry> = emptyList()
)