package com.whispervault.data

/**
 * Pure queue state transitions for unit testing (mirrors worker behavior).
 */
object TaskStateMachine {
    fun canTransition(from: TaskStatus, to: TaskStatus): Boolean = when (from) {
        TaskStatus.WAITING -> to == TaskStatus.RUNNING || to == TaskStatus.FAILED
        TaskStatus.RUNNING -> to == TaskStatus.DONE || to == TaskStatus.FAILED || to == TaskStatus.WAITING
        TaskStatus.DONE -> false
        TaskStatus.FAILED -> false
    }

    fun nextAfterCrash(current: TaskStatus): TaskStatus =
        if (current == TaskStatus.RUNNING) TaskStatus.WAITING else current
}
