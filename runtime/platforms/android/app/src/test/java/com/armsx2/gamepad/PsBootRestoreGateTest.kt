package com.armsx2.gamepad

import org.junit.Assert.*
import org.junit.Test

class PsBootRestoreGateTest {
    @Test fun slowBootHasNoFifteenSecondExpiry() {
        val gate=PsBootRestoreGate()
        repeat(400) {assertFalse(gate.ready(true,false,0))}
        assertFalse(gate.ready(true,true,1))
        assertFalse(gate.ready(true,true,2))
        assertFalse(gate.ready(true,true,3))
        assertTrue(gate.ready(true,true,4))
    }
    @Test fun pauseCannotConsumeTheRestoreRequest() {
        val gate=PsBootRestoreGate()
        repeat(400) {assertFalse(gate.ready(false,true,200))}
        assertFalse(gate.ready(true,true,201))
        assertFalse(gate.ready(true,true,202))
        assertFalse(gate.ready(true,true,203))
        assertTrue(gate.ready(true,true,204))
    }
    @Test fun staleCounterAndStalledFramesAreNotReadiness() {
        val gate=PsBootRestoreGate()
        repeat(100) {assertFalse(gate.ready(true,true,5000))}
        assertFalse(gate.ready(true,true,1))
        assertFalse(gate.ready(true,true,2))
        assertFalse(gate.ready(true,true,3))
        assertTrue(gate.ready(true,true,4))
    }
    @Test fun PauseBetweenFramesRequiresNewSustainedAdvancement() {
        val gate=PsBootRestoreGate()
        gate.ready(true,true,1);gate.ready(true,true,2);gate.ready(true,true,3)
        assertFalse(gate.ready(false,true,3))
        assertFalse(gate.ready(true,true,4))
        assertFalse(gate.ready(true,true,5))
        assertFalse(gate.ready(true,true,6))
        assertTrue(gate.ready(true,true,7))
    }
}
