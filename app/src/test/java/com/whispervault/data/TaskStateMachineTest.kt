package com.whispervault.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskStateMachineTest {
    @Test
    fun waitingCanBecomeRunning() {
        assertTrue(TaskStateMachine.canTransition(TaskStatus.WAITING, TaskStatus.RUNNING))
    }

    @Test
    fun runningCanFinishOrFailOrReset() {
        assertTrue(TaskStateMachine.canTransition(TaskStatus.RUNNING, TaskStatus.DONE))
        assertTrue(TaskStateMachine.canTransition(TaskStatus.RUNNING, TaskStatus.FAILED))
        assertTrue(TaskStateMachine.canTransition(TaskStatus.RUNNING, TaskStatus.WAITING))
    }

    @Test
    fun terminalStatesAreFrozen() {
        assertFalse(TaskStateMachine.canTransition(TaskStatus.DONE, TaskStatus.WAITING))
        assertFalse(TaskStateMachine.canTransition(TaskStatus.FAILED, TaskStatus.RUNNING))
    }

    @Test
    fun crashResetsRunningToWaiting() {
        assertEquals(TaskStatus.WAITING, TaskStateMachine.nextAfterCrash(TaskStatus.RUNNING))
        assertEquals(TaskStatus.DONE, TaskStateMachine.nextAfterCrash(TaskStatus.DONE))
    }
}
