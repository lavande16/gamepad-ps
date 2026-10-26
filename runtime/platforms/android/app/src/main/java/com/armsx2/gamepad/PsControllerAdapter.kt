package com.armsx2.gamepad

import org.yuzu.yuzu_emu.views.GameFoldButton
import org.yuzu.yuzu_emu.views.GameFoldStick
import org.yuzu.yuzu_emu.views.GameFoldControllerListener
import kotlin.math.abs
import kotlin.math.roundToInt

/** Semantic DualShock 2 binding. No screen coordinates and no dependency on stock touch widgets. */
class PsControllerAdapter(
    private val send: (Int, Int, Boolean) -> Unit,
    private val home: () -> Unit,
    private val menu: () -> Unit = {},
) : GameFoldControllerListener {
    var enabled = false
        set(value) { if (!value) reset(); field = value }
    private val down = mutableSetOf<Int>()
    private val axes = mutableMapOf<Int, Int>()
    override fun onButtonChanged(button: GameFoldButton, pressed: Boolean) {
        if (button == GameFoldButton.HOME) { if (pressed) { reset(); home() }; return }
        val key = KEYS[button] ?: return
        if (!enabled && pressed) return
        if (pressed) down.add(key) else down.remove(key)
        if (pressed && key in setOf(106,107) && down.containsAll(setOf(106,107))) {
            reset(); menu(); return
        }
        send(key, 0, pressed)
    }
    override fun onStickChanged(stick: GameFoldStick, x: Float, y: Float) {
        val base = if (stick == GameFoldStick.LEFT) 110 else 120
        val dx = if (enabled) x.coerceIn(-1f, 1f) else 0f
        val dy = if (enabled) y.coerceIn(-1f, 1f) else 0f
        listOf(-dy, dx, dy, -dx).forEachIndexed { i, v ->
            val magnitude = (v.coerceAtLeast(0f) * 32767).roundToInt()
            if (axes[base+i] != magnitude) {
                axes[base+i] = magnitude
                send(base+i, magnitude, magnitude > 0)
            }
        }
    }
    fun reset() {
        // NativeApp.resetKeyStatus is a no-op in the pinned runtime. Explicit releases are required.
        KEYS.values.forEach { send(it, 0, false) }
        (110..113).plus(120..123).forEach { send(it, 0, false) }
        down.clear(); axes.clear()
    }
    companion object {
        val KEYS = mapOf(
            GameFoldButton.DPAD_UP to 19, GameFoldButton.DPAD_RIGHT to 22,
            GameFoldButton.DPAD_DOWN to 20, GameFoldButton.DPAD_LEFT to 21,
            GameFoldButton.X to 100, GameFoldButton.A to 97,
            GameFoldButton.B to 96, GameFoldButton.Y to 99,
            GameFoldButton.MINUS to 109, GameFoldButton.PLUS to 108,
            GameFoldButton.L to 102, GameFoldButton.ZL to 104,
            GameFoldButton.R to 103, GameFoldButton.ZR to 105,
            GameFoldButton.L3 to 106, GameFoldButton.R3 to 107,
        )
    }
}
