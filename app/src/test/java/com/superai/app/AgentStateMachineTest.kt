package com.superai.app

import com.superai.app.agent.state.AgentState
import com.superai.app.agent.state.AgentStateMachine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AgentStateMachineTest {
    private lateinit var sm: AgentStateMachine

    @Before fun setUp() { sm = AgentStateMachine() }

    @Test fun `initial state is IDLE`() {
        assertEquals(AgentState.IDLE, sm.context.value.state)
    }

    @Test fun `IDLE to LISTENING is valid`() {
        sm.transition(AgentState.LISTENING)
        assertEquals(AgentState.LISTENING, sm.context.value.state)
    }

    @Test fun `LISTENING to PROCESSING is valid`() {
        sm.transition(AgentState.LISTENING)
        sm.transition(AgentState.PROCESSING, "test directive")
        assertEquals(AgentState.PROCESSING, sm.context.value.state)
        assertEquals("test directive", sm.context.value.lastDirective)
    }

    @Test fun `PROCESSING to RESPONDING is valid`() {
        sm.transition(AgentState.LISTENING)
        sm.transition(AgentState.PROCESSING)
        sm.transition(AgentState.RESPONDING)
        assertEquals(AgentState.RESPONDING, sm.context.value.state)
    }

    @Test fun `invalid transition results in ERROR state`() {
        sm.transition(AgentState.RESPONDING)
        assertEquals(AgentState.ERROR, sm.context.value.state)
        assertNotNull(sm.context.value.errorMessage)
    }

    @Test fun `ERROR recovers to IDLE`() {
        sm.transition(AgentState.RESPONDING)
        assertEquals(AgentState.ERROR, sm.context.value.state)
        sm.transition(AgentState.IDLE)
        assertEquals(AgentState.IDLE, sm.context.value.state)
    }

    @Test fun `reset clears state to IDLE`() {
        sm.transition(AgentState.LISTENING)
        sm.pushContext("some context entry")
        sm.reset()
        assertEquals(AgentState.IDLE, sm.context.value.state)
        assertTrue(sm.context.value.contextWindow.isEmpty())
    }

    @Test fun `pushContext adds to window`() {
        sm.pushContext("entry1")
        sm.pushContext("entry2")
        assertEquals(2, sm.context.value.contextWindow.size)
    }

    @Test fun `clearContext empties window`() {
        sm.pushContext("entry1")
        sm.clearContext()
        assertTrue(sm.context.value.contextWindow.isEmpty())
    }

    @Test fun `setProfile stores profile id`() {
        sm.setProfile("profile-123")
        assertEquals("profile-123", sm.context.value.activeProfileId)
    }
}
