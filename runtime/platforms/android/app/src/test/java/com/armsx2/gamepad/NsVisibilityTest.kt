package com.armsx2.gamepad
import org.junit.Assert.*
import org.junit.Test
import org.yuzu.yuzu_emu.views.*
class NsVisibilityTest {
    @Test fun hidesAfterTenSecondsButNeverWhileHolding() {
        val s=GamePadControllerVisibilityState();s.setEnabled(true,0)
        assertFalse(s.update(9999));assertTrue(s.update(10000));assertFalse(s.controlsVisible)
        assertEquals(GamePadWakeTouchDisposition.WAKE_ONLY,s.onTouchDown(11000,false))
        assertTrue(s.controlsVisible);assertFalse(s.update(50000));s.onTouchUp(50001)
        assertFalse(s.update(60000));assertTrue(s.update(60001))
    }
    @Test fun homeBypassesWakeOnlyAndDisablingRestoresVisibility() {
        val s=GamePadControllerVisibilityState();s.setEnabled(true,0);s.update(10000)
        assertEquals(GamePadWakeTouchDisposition.DISPATCH,s.onTouchDown(10001,true))
        s.onTouchCancel(10002);s.update(20002);s.setEnabled(false,20003)
        assertTrue(s.controlsVisible);assertFalse(s.update(90000))
    }
}
