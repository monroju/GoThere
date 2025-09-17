// app/src/main/java/com/example/gothere/model/DocumentItem.kt
package com.example.gothere.model

data class DocumentItem(
    val name: String = "",
    val path: String = "",   // e.g. "documents/{uid}/12345"
    val url: String = ""     // signed download URL (for preview/download)
)
