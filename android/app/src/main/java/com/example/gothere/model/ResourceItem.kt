package com.example.gothere.model

data class ResourceItem(
    val name: String,
    val path: String,
    val sizeBytes: Long? = null,
    val downloadUrl: String? = null,
    val category: String = "" // <-- new
)
