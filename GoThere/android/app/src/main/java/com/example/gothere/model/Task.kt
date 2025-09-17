// app/src/main/java/com/example/gothere/model/Task.kt
package com.example.gothere.model

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val completed: Boolean = false,
    val category: String = "",
    val links: List<Link>? = null,    // uses Link from Link.kt (keep that file)
    val dueAt: Long? = null           // <-- optional due date used by Tasks UI/VM
)
