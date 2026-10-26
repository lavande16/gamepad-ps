// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.gamepad

import org.json.JSONObject

object DefaultPaths {
    fun resolve(values: JSONObject, dll: String): JSONObject = JSONObject(values.toString()).apply {
        // Android JSONObject escapes '/' when serializing. Replace parsed values, never JSON text.
        keys().forEach { key ->
            val value = opt(key)
            if (value is String) put(key, value.replace("\u0024{ROOT}/LSFG/Lossless.dll", dll))
        }
    }
}
