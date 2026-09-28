package com.bitbytestudio.physicstodo.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


// ============================================================
// MODEL
// ============================================================

@Stable
data class Todo(
    val id: Long,
    val text: String,
    val completed: Boolean
)

@Stable
class TodoViewModel : ViewModel() {

    private val _todos = MutableStateFlow(
        listOf(
            Todo(
                id = 1L,
                text = "Buy groceries",
                completed = false
            ),
            Todo(
                id = 2L,
                text = "Complete Android project",
                completed = false
            ),
            Todo(
                id = 3L,
                text = "Read a book",
                completed = false
            ),
            Todo(
                id = 4L,
                text = "Go for a walk",
                completed = false
            ),
            Todo(
                id = 5L,
                text = "Learn Jetpack Compose",
                completed = false
            )
        )
    )

    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()

    fun toggleTodo(id: Long) {

        _todos.value = _todos.value.map { todo ->

            if (todo.id == id) {
                todo.copy(
                    completed = !todo.completed
                )
            } else {
                todo
            }
        }
    }
}