package com.armsx2.gamepad

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class PsGameFilesTest {
    @get:Rule val tmp=TemporaryFolder()
    @Test fun mixedLayoutsKeepIndependentSaveOwnership() {
        val root=tmp.newFolder("game")
        for(path in listOf("Flat.iso","Nested/game.chd","Legacy/ISO/game.bin","Nested/Sstates/not-a-rom.bin","Nested/Memcards/card.bin")) {
            File(root,path).apply{parentFile.mkdirs();writeText("fixture")}
        }
        val found=PsGameFiles.discover(root)
        assertEquals(3,found.size)
        assertEquals(setOf("Flat","Nested","Legacy"),found.map{it.directory.name}.toSet())
        assertTrue(File(root,"Flat.iso").isFile)
        assertFalse(File(root,"Flat").exists())
    }
    @Test fun externalSymlinkIsNeverScanned() {
        val root=tmp.newFolder("game");val outside=tmp.newFolder("outside")
        File(outside,"escape.iso").writeText("fixture")
        Files.createSymbolicLink(File(root,"link").toPath(),outside.toPath())
        assertTrue(PsGameFiles.discover(root).isEmpty())
    }
}
