package com.example.nasonly.model

import java.net.InetAddress

data class NasDevice(
    val ip: InetAddress,
    val name: String?,
    val reachable: Boolean,
)
