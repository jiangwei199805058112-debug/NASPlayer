package com.example.nasonly.model

data class NasHost(
    val host: String,
    val share: String,
    val username: String,
    val password: String,
    val domain: String = "",
)