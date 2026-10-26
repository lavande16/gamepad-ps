package com.armsx2.gamepad

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DefaultPathsTest {
    @Test fun escapedJsonPathsResolveOnFirstInitializationAndUserPathsRemain() {
        val json = JSONObject("""{"lsfgDllPath":"${'$'}{ROOT}\/LSFG\/Lossless.dll","lsfgEnabled":true,"upscaleFloat":4}""")
        val path = "/data/user/0/com.gamepad.ps/files/lsfg/Lossless.dll"
        // Reproduce Android's slash-escaping even when these tests use desktop org.json.
        val androidJson = object : JSONObject(json.toString()) {
            override fun toString(): String = super.toString().replace("/", "\\/")
        }
        val seeded = DefaultPaths.resolve(androidJson, path)
        assertEquals(path, seeded.getString("lsfgDllPath"))
        assertTrue(seeded.getBoolean("lsfgEnabled"))
        assertEquals(4, seeded.getInt("upscaleFloat"))
        assertEquals("${'$'}{ROOT}/LSFG/Lossless.dll", json.getString("lsfgDllPath"))
        val custom = JSONObject().put("lsfgDllPath", "/user/chosen.dll")
        assertEquals("/user/chosen.dll", DefaultPaths.resolve(custom, path).getString("lsfgDllPath"))
    }
}
