// SPDX-FileCopyrightText: Copyright 2026 GameFold Project
// SPDX-License-Identifier: GPL-3.0-or-later

package org.yuzu.yuzu_emu.utils

/** Product boundary for the independent GamePad NS APK. */
object GamePadNsProductContract {
    const val APPLICATION_ID = "com.gamepad.pspad"
    const val VERSION_NAME = "0.4.0"
    const val VERSION_CODE = 7
    const val EXTERNAL_ROOT_NAME = "GamePadPS"
    const val DEFAULTS_MIGRATION_VERSION = 2

    /** GamePad NS is a landscape-only full-screen controller shell. */
    const val STRICT_GAMEPAD_SHELL = true
}
