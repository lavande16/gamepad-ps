package com.armsx2.gamepad

/** Readiness has no time limit: a menu pause or slow disc must not consume a boot restore. */
internal class PsBootRestoreGate {
    private var lastFrame = -1
    private var advancingPolls = 0
    fun ready(running: Boolean, identityReady: Boolean, frame: Int): Boolean {
        if (!running || !identityReady) {
            lastFrame = -1
            advancingPolls = 0
            return false
        }
        advancingPolls = if (lastFrame >= 0 && frame > lastFrame) advancingPolls + 1 else 0
        lastFrame = frame
        return advancingPolls >= 3
    }
}
