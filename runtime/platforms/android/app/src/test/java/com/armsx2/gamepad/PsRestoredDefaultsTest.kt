package com.armsx2.gamepad

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PsRestoredDefaultsTest {
    @Test fun forcesGlobalAndEveryExistingGameWithoutResettingOtherChoices() {
        val d=JSONObject("""{"global":{"textureUpscale":0,"fpsLimit":0,"audioVolume":42},"games":{"A":{"gsBackThreadMode":0,"upscaleFloat":3},"B":{"precacheTextureReplacements":false}}}""")
        PsRestoredDefaults.forceDocument(d)
        for(values in listOf(d.getJSONObject("global"),d.getJSONObject("games").getJSONObject("A"),d.getJSONObject("games").getJSONObject("B"))) {
            assertEquals(2,values.getInt("textureUpscale"));assertEquals(1,values.getInt("gsBackThreadMode"))
            assertEquals(60,values.getInt("fpsLimit"));assertTrue(values.getBoolean("precacheTextureReplacements"))
        }
        assertEquals(42,d.getJSONObject("global").getInt("audioVolume"))
        assertEquals(3,d.getJSONObject("games").getJSONObject("A").getInt("upscaleFloat"))
    }
    @Test fun updatesNativeSectionRemovesDuplicateOldValuesAndPreservesOthers() {
        val before="[Other]\nTextureUpscale = 7\n[EmuCore/GS]\nTextureUpscale = 0\nTextureUpscale = 1\nAspectRatio = Stretch\n[Pad]\nEnabled = true\n"
        val after=PsRestoredDefaults.forceGameIni(before)
        assertTrue(after.contains("[Other]\nTextureUpscale = 7"));assertTrue(after.contains("TextureUpscale = 2"))
        assertFalse(after.contains("TextureUpscale = 0"));assertFalse(after.contains("TextureUpscale = 1"))
        assertTrue(after.contains("PrecacheTextureReplacements = true"));assertTrue(after.contains("GSBackThreadMode = 1"))
        assertTrue(after.contains("AspectRatio = Stretch"));assertTrue(after.contains("[Pad]\nEnabled = true"))
        assertEquals(after,PsRestoredDefaults.forceGameIni(after))
    }
    @Test fun appendsMissingNativeSection() {
        val result=PsRestoredDefaults.forceGameIni("[Pad]\nEnabled = true\n")
        assertTrue(result.contains("[EmuCore/GS]\n"));assertTrue(result.contains("TextureUpscale = 2"))
    }
}
