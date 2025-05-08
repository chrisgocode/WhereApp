package com.example.where.model

import com.google.firebase.Timestamp

data class Group(
    val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val polls: List<Poll> = emptyList()
)