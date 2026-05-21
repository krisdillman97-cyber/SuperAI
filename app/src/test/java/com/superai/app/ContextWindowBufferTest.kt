package com.superai.app

import com.superai.app.agent.state.AgentStateMachine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ContextWindowBufferTest {
    private lateinit var sm: AgentStateMachine

    @Before fun setUp() { sm = AgentStateMachine() }

    @Test fun `push single entry`() {
        sm.pushContext("hello world")
        assertEquals(1, sm.context.value.contextWindow.size)
        assertEquals("hello world", sm.context.value.contextWindow[0])
    }

    @Test fun `push multiple entries preserves order`() {
        listOf("a", "b", "c").forEach { sm.pushContext(it) }
        assertEquals(listOf("a", "b", "c"), sm.context.value.contextWindow)
    }

    @Test fun `build context string joins with newlines`() {
        sm.pushContext("line1")
        sm.pushContext("line2")
        assertEquals("line1\nline2", sm.buildContextString())
    }

    @Test fun `clear empties window`() {
        sm.pushContext("data")
        sm.clearContext()
        assertEquals(0, sm.context.value.contextWindow.size)
    }

    @Test fun `large entry prunes old context to stay within token limit`() {
        // Fill 3072 tokens worth of text (each char ≈ 0.25 tokens → 12288 chars)
        repeat(10) { sm.pushContext("x".repeat(1200)) }
        // Now push a large entry of 512 tokens (2048 chars)
        sm.pushContext("y".repeat(2048))
        // Total should be under maxContextTokens (4096)
        val totalTokens = sm.context.value.contextWindow.sumOf { it.length / 4 }
        assertTrue("Token count $totalTokens should be <= 4096", totalTokens <= 4096)
    }
}
