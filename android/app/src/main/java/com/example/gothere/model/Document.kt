package com.example.gothere.model

data class Document(
    val id: String = "",
    val name: String = "",
    val downloadUrl: String = "",
    val path: String = ""   // Firebase Storage path used for deletes
)
