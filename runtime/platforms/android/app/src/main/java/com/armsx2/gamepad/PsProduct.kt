package com.armsx2.gamepad

/** One identity gate: legacy development package and the requested square-icon identity. */
object PsProduct {
    const val APPLICATION_ID = "com.gamepad.pspad"
    const val active = com.armsx2.BuildConfig.APPLICATION_ID == APPLICATION_ID || com.armsx2.BuildConfig.APPLICATION_ID == "com.gamepad.ps"
}
